package com.smartfiles.domain

/** A tag candidate produced by the tag extractor (LLD §4.3). */
data class TagCandidate(
    val name: String,
    val confidence: Float,
)

interface TagRepository {
    /** Replaces the persisted tag set for a file and refreshes its FTS tag string. */
    suspend fun replaceTagsForFile(fileId: Long, tags: List<TagCandidate>)
}