package com.smartfiles.domain

interface EmbeddingRepository {
    suspend fun generateAndStore(fileId: Long, representativeText: String)
    suspend fun vectorFor(fileId: Long): FloatArray?
    suspend fun topKSimilar(queryVector: FloatArray, k: Int, excludeFileId: Long? = null): List<ScoredFileId>
    suspend fun countEmbeddings(): Long

    /** Recomputes an album's centroid embedding from its members' stored vectors. */
    suspend fun recomputeAlbumCentroid(albumId: Long)

    /** Recomputes centroids for every album that has stored embeddings. */
    suspend fun recomputeAllAlbumCentroids()

    /**
     * Max cosine similarity of [queryVector] to any album centroid that exists,
     * or null when no centroids have been computed yet (evidence absent).
     */
    suspend fun closestCentroidSimilarity(queryVector: FloatArray): Float?
}

data class ScoredFileId(val fileId: Long, val score: Float)

/** Whether the embedding model is available on this device (feature-detect). */
interface EmbeddingCapabilities {
    val available: Boolean
    val modelVersion: String
    val embeddingDim: Int
}
