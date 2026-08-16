package com.smartfiles.data.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.database.dao.QueueDao
import com.smartfiles.core.database.entity.ProcessingQueueEntity
import com.smartfiles.core.model.FileItem
import com.smartfiles.domain.ContentExtractor
import com.smartfiles.domain.FileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Drains the persistent processing queue, advancing each file through Level-2
 * content extraction (LLD §4.2, §4.10). Batch size scales with charging+idle
 * state. A killed/rescheduled worker resumes safely because each file's result
 * is committed to Room only after it completes. Anything enqueued while the
 * loop runs is picked up on the next iteration; stragglers/backoff are
 * re-triggered by ProcessingQueueRepositoryImpl.enqueue and periodic scans.
 */
@HiltWorker
class DeepProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val queueDao: QueueDao,
    private val fileRepository: FileRepository,
    private val contentExtractor: ContentExtractor,
    private val logger: AppLogger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            while (true) {
                val batchSize = batchSize()
                val batch = queueDao.dequeueBatch(System.currentTimeMillis(), batchSize)
                if (batch.isEmpty()) break
                for (item in batch) {
                    processItem(item)
                }
                // A short batch means the queue is nearly drained; stop here.
                if (batch.size < batchSize) break
            }
            Result.success()
        } catch (e: Exception) {
            logger.e(TAG, "deep processing failed", e)
            Result.retry()
        }
    }

    private suspend fun processItem(item: ProcessingQueueEntity) {
        val now = System.currentTimeMillis()
        queueDao.markInProgress(item.queueId, now)
        try {
            val file: FileItem? = fileRepository.observeFile(item.fileId).first()
            if (file == null) {
                // File vanished from the index; nothing left to do.
                queueDao.markDone(item.queueId)
                return
            }
            val extraction = contentExtractor.extract(file)
            fileRepository.updateProcessingResult(item.fileId, extraction)
            queueDao.markDone(item.queueId)
        } catch (e: Exception) {
            logger.w(TAG, "processing failed for queue item ${item.queueId}", e)
            val backoff = RETRY_INITIAL_MS shl item.retryCount.coerceAtMost(MAX_EXPONENT)
            queueDao.markFailed(item.queueId, now, now + backoff)
        }
    }

    private fun batchSize(): Int =
        if (isCharging() && isIdle()) DEEP_BATCH_CHARGING else DEEP_BATCH_BATTERY

    private fun isCharging(): Boolean {
        val intent = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isIdle(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isDeviceIdleMode
    }

    companion object {
        private const val TAG = "DeepProcessingWorker"
        private const val DEEP_BATCH_BATTERY = 5
        private const val DEEP_BATCH_CHARGING = 20
        private const val RETRY_INITIAL_MS = 15_000L
        private const val MAX_EXPONENT = 4
    }
}