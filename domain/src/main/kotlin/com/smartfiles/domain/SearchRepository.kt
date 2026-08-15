package com.smartfiles.domain

import com.smartfiles.core.model.FileItem

/** A parsed natural-language query (LLD §4.6). */
data class ParsedQuery(
    val raw: String,
    val residualText: String,
    val albumName: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val docType: com.smartfiles.core.model.DocType? = null,
)

/** Component scores feeding a fused, explainable rank (LLD §4.6). */
data class RankedSearchResult(
    val file: FileItem,
    val finalScore: Float,
    val keywordScore: Float,
    val semanticScore: Float,
    val filenameScore: Float,
    val metadataScore: Float,
    val explanation: List<String>,
)

interface SearchRepository {
    suspend fun search(query: ParsedQuery): List<RankedSearchResult>
    suspend fun relatedFiles(fileId: Long): List<RankedSearchResult>
}
