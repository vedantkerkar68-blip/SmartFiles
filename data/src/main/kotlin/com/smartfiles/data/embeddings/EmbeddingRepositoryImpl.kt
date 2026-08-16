package com.smartfiles.data.embeddings

import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.common.CoroutineDispatchers
import com.smartfiles.core.common.EmbeddingCodec
import com.smartfiles.core.database.dao.AlbumDao
import com.smartfiles.core.database.dao.EmbeddingDao
import com.smartfiles.core.database.entity.EmbeddingEntity
import com.smartfiles.core.ml.EmbeddingModelManager
import com.smartfiles.core.ml.RepresentativeTextBuilder
import com.smartfiles.domain.EmbeddingRepository
import com.smartfiles.domain.EmbeddingCapabilities
import com.smartfiles.domain.ScoredFileId
import com.smartfiles.domain.SettingsRepository
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * Room-backed [EmbeddingRepository] (LLD §4.4). Vectors are generated through the
 * LiteRT [EmbeddingModelManager], packed as float16 via [EmbeddingCodec], keyed by
 * (fileId, modelVersion) so an embedding-model upgrade can re-embed the corpus
 * incrementally ([EmbeddingEntity]). If the model is unavailable the repository
 * simply stores nothing — callers degrade to keyword-only behavior.
 *
 * `topKSimilar` streams the corpus in bounded pages (LLD `VECTOR_SEARCH_CHUNK_SIZE`)
 * through a capacity-k min-heap, so peak memory is flat regardless of corpus size;
 * since vectors are pre-normalized, cosine similarity reduces to a dot product.
 */
@Singleton
class EmbeddingRepositoryImpl @Inject constructor(
    private val embeddingDao: EmbeddingDao,
    private val albumDao: AlbumDao,
    private val modelManager: EmbeddingModelManager,
    private val representativeTextBuilder: RepresentativeTextBuilder,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: CoroutineDispatchers,
    private val logger: AppLogger,
) : EmbeddingRepository, EmbeddingCapabilities by modelManager {

    override suspend fun generateAndStore(fileId: Long, representativeText: String) {
        if (!modelManager.available) return
        val vector = modelManager.embed(representativeText) ?: return
        embeddingDao.upsert(
            EmbeddingEntity(
                fileId = fileId,
                modelVersion = modelManager.modelVersion,
                vector = EmbeddingCodec.encode(vector),
                dim = vector.size,
                generatedAt = System.currentTimeMillis(),
            ),
        )
        logger.i(TAG, "stored embedding for file $fileId (dim=${vector.size})")
    }

    override suspend fun vectorFor(fileId: Long): FloatArray? {
        val row = embeddingDao.getForFile(fileId, modelManager.modelVersion) ?: return null
        return EmbeddingCodec.decode(row.vector, row.dim)
    }

    override suspend fun topKSimilar(
        queryVector: FloatArray,
        k: Int,
        excludeFileId: Long?,
    ): List<ScoredFileId> = withContext(dispatchers.io) {
        val chunkSize = settingsRepository.get().vectorSearchChunkSize
        val model = modelManager.modelVersion
        val heap = PriorityQueue<ScoredFileId>(k) { a, b -> a.score.compareTo(b.score) }
        var offset = 0
        while (true) {
            val page = embeddingDao.getVectorPage(model, chunkSize, offset)
            if (page.isEmpty()) break
            for (row in page) {
                if (row.fileId == excludeFileId) continue
                val vec = EmbeddingCodec.decode(row.vector, row.dim)
                val score = EmbeddingCodec.dot(queryVector, vec)
                if (heap.size < k) {
                    heap.offer(ScoredFileId(row.fileId, score))
                } else {
                    val weakest = heap.peek()
                    if (weakest != null && score > weakest.score) {
                        heap.poll()
                        heap.offer(ScoredFileId(row.fileId, score))
                    }
                }
            }
            offset += chunkSize
        }
        heap.toList().sortedByDescending { it.score }
    }

    override suspend fun countEmbeddings(): Long =
        embeddingDao.count(modelManager.modelVersion)

    override suspend fun recomputeAlbumCentroid(albumId: Long) {
        val rows = embeddingDao.embeddingsForAlbum(albumId, modelManager.modelVersion)
        if (rows.isEmpty()) return
        val dim = rows.first().dim
        val centroid = FloatArray(dim)
        for (row in rows) {
            val vec = EmbeddingCodec.decode(row.vector, row.dim)
            for (i in 0 until dim) centroid[i] += vec[i]
        }
        EmbeddingCodec.normalizeInPlace(centroid)
        albumDao.updateCentroid(albumId, EmbeddingCodec.encode(centroid))
        logger.i(TAG, "recomputed centroid for album $albumId from ${rows.size} vectors")
    }

    override suspend fun recomputeAllAlbumCentroids() {
        for (albumId in albumDao.allAlbumIds()) {
            recomputeAlbumCentroid(albumId)
        }
    }

    override suspend fun closestCentroidSimilarity(queryVector: FloatArray): Float? {
        val centroids = albumDao.albumsWithCentroids()
        if (centroids.isEmpty()) return null
        var best = -1f
        for (row in centroids) {
            val vec = EmbeddingCodec.decode(row.centroidEmbedding, queryVector.size)
            val sim = EmbeddingCodec.dot(queryVector, vec)
            if (sim > best) best = sim
        }
        return best.coerceIn(0f, 1f)
    }

    /** Builds representative text (filename + excerpt + tags) and stores an embedding. */
    suspend fun generateAndStoreFromSource(
        fileId: Long,
        displayName: String,
        extractedText: String?,
        tags: List<String>,
    ) {
        generateAndStore(fileId, representativeTextBuilder.build(displayName, extractedText, tags))
    }

    companion object {
        private const val TAG = "EmbeddingRepository"
    }
}