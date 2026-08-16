package com.smartfiles.core.ml

import android.content.Context
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.common.CoroutineDispatchers
import com.smartfiles.core.common.EmbeddingCodec
import com.smartfiles.domain.EmbeddingCapabilities
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

/**
 * LiteRT lifecycle for the bundled MiniLM-L6-int8 text-embedding model
 * (LLD §4.4 + §8 threading). The interpreter is loaded lazily on first use and
 * unloaded after [MODEL_IDLE_UNLOAD_MS] of inactivity (coroutine watchdog), and
 * every inference is serialized onto a single-thread dispatcher — LiteRT/ML Kit
 * interpreters are not safe for concurrent use, and serializing caps peak
 * memory to one in-flight model.
 *
 * [available] is false whenever the model cannot be loaded (missing/corrupt
 * asset, unsupported runtime), so callers degrade to keyword-only behavior
 * instead of crashing or emitting fabricated vectors.
 */
class EmbeddingModelManager(
    private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val logger: AppLogger,
) : EmbeddingCapabilities {

    private val watchdogScope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val loadMutex = Mutex()

    private var mappedBuffer: MappedByteBuffer? = null
    private var interpreter: Interpreter? = null
    private var tokenizer: WordPieceTokenizer? = null
    private var loadFailure: Boolean = false
    @Volatile private var lastUsedAtMs: Long = 0
    private var idleWatchdog: Job? = null
    private var residentDim: Int = 0

    override val available: Boolean
        get() = interpreter != null || (!loadFailure && modelAssetExists())

    override val modelVersion: String = MODEL_VERSION

    override val embeddingDim: Int
        get() = residentDim.takeIf { it > 0 } ?: DEFAULT_DIM

    init {
        try {
            tokenizer = WordPieceTokenizer.load(context.assets.open(VOCAB_ASSET))
            logger.i(TAG, "embedding vocab loaded (${tokenizer?.let { it.tokenIds("x").size }} dims = $embeddingDim)")
        } catch (e: Exception) {
            loadFailure = true
            logger.w(TAG, "embedding vocab unavailable -> model disabled", e)
        }
    }

    /** Embeds [text] into a unit-norm vector, or null when the model is off. */
    suspend fun embed(text: String): FloatArray? {
        if (!available) return null
        return withContext(dispatchers.mlInference) {
            loadMutex.withLock {
                // Double-check under lock: a concurrent loader may have finished.
                if (!available) return@withLock null
                val interp = ensureLoaded() ?: return@withLock null
                lastUsedAtMs = System.currentTimeMillis()
                try {
                    val tokens = tokenizer ?: return@withLock null
                    val ids = tokens.tokenIds(text)
                    val mask = tokens.attentionMask(text)
                    // Each input tensor is shaped [1, seq] (int32) after allocateTensors();
                    // the model takes input_ids + attention_mask as two separate tensors.
                    val inputs: Array<Any> = arrayOf(arrayOf(ids), arrayOf(mask))
                    val outputs = HashMap<Int, Any>()
                    val outRows = arrayOf(FloatArray(embeddingDim))
                    outputs[0] = outRows
                    interp.runForMultipleInputsOutputs(inputs, outputs)
                    EmbeddingCodec.normalizeInPlace(outRows[0])
                } catch (e: Exception) {
                    logger.w(TAG, "embedding inference failed", e)
                    null
                }
            }
        }
    }

    /** Loads the interpreter on demand; re-schedules the idle-unload watchdog. */
    private fun ensureLoaded(): Interpreter? {
        interpreter?.let { scheduleIdleUnload(); return it }
        return try {
            val buffer = context.assets.openFd(MODEL_ASSET).use { afd ->
                FileInputStream(afd.fileDescriptor).channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength,
                )
            }
            mappedBuffer = buffer
            val interp = Interpreter(buffer)
            // The exported model has flexible [1, seq] int32 inputs; fix them to the
            // tokenizer's max capacity so shapes are static across runs.
            interp.resizeInput(0, intArrayOf(1, WordPieceTokenizer.MAX_SEQ))
            interp.resizeInput(1, intArrayOf(1, WordPieceTokenizer.MAX_SEQ))
            interp.allocateTensors()
            findDim(interp)
            interpreter = interp
            loadFailure = false
            logger.i(TAG, "embedding model loaded (dim=$embeddingDim)")
            scheduleIdleUnload()
            interp
        } catch (e: Exception) {
            loadFailure = true
            logger.w(TAG, "embedding model could not be loaded -> disabled", e)
            null
        }
    }

    /** Reads the model's output dim from its output tensor metadata if available. */
    private fun findDim(interp: Interpreter) {
        try {
            val out = interp.getOutputTensor(0)
            val shape = out.shape()
            residentDim = shape.takeIf { it.isNotEmpty() }?.last() ?: DEFAULT_DIM
        } catch (_: Throwable) {
            residentDim = DEFAULT_DIM
        }
    }

    private fun scheduleIdleUnload() {
        idleWatchdog?.cancel()
        idleWatchdog = watchdogScope.launch {
            delay(MODEL_IDLE_UNLOAD_MS)
            val idleMs = System.currentTimeMillis() - lastUsedAtMs
            if (idleMs >= MODEL_IDLE_UNLOAD_MS) unload()
        }
    }

    private fun unload() {
        try {
            interpreter?.close()
        } finally {
            interpreter = null
            lastUsedAtMs = 0
            logger.i(TAG, "embedding model unloaded (idle)")
        }
    }

    private fun modelAssetExists(): Boolean = try {
        context.assets.openFd(MODEL_ASSET).use {} ; true
    } catch (_: Exception) {
        false
    }

    private fun shutdown() {
        watchdogScope.cancel()
        unload()
        mappedBuffer = null
        tokenizer = null
    }

    companion object {
        private const val TAG = "EmbeddingModel"
        const val MODEL_VERSION = "minilm-int8-v1"
        const val MODEL_ASSET = "embeddings/all-MiniLM-L6-v2-int8.tflite"
        const val VOCAB_ASSET = "embeddings/vocab.txt"
        const val MODEL_IDLE_UNLOAD_MS = 60_000L
        const val DEFAULT_DIM = 384
    }
}