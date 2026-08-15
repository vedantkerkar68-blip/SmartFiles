package com.smartfiles.domain

interface EmbeddingRepository {
    suspend fun generateAndStore(fileId: Long, representativeText: String)
    suspend fun vectorFor(fileId: Long): FloatArray?
    suspend fun topKSimilar(queryVector: FloatArray, k: Int, excludeFileId: Long? = null): List<ScoredFileId>
    suspend fun countEmbeddings(): Long
}

data class ScoredFileId(val fileId: Long, val score: Float)

/** Whether the embedding model is available on this device (feature-detect). */
interface EmbeddingCapabilities {
    val available: Boolean
    val modelVersion: String
    val embeddingDim: Int
}
