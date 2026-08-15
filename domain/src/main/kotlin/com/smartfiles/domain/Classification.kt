package com.smartfiles.domain

import com.smartfiles.core.model.AlbumType

/** Classification decision plus the human-readable reasoning behind it. */
data class ClassificationResult(
    val albumId: Long? = null,
    val confidence: Float = 0f,
    val reasoning: List<String> = emptyList(),
    val suggestedTags: List<String> = emptyList(),
)

interface ClassificationEngine {
    suspend fun classify(representativeText: String, fileId: Long): ClassificationResult
}

enum class AlbumAssignmentDecision { AUTO_ASSIGN, SUGGEST, LEAVE_UNCATEGORIZED }

class AlbumDecision(
    val decision: AlbumAssignmentDecision,
    val albumId: Long? = null,
    val confidence: Float = 0f,
)

/** Strategy seam for optional cloud classification (LLD §4.12). */
interface ClassificationStrategy {
    suspend fun classify(text: String): ClassificationResult
}
