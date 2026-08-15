package com.smartfiles.domain

import com.smartfiles.core.model.FileItem
import kotlinx.coroutines.flow.Flow

/** Result of a content-extraction pass (LLD §4.2). */
data class ExtractionResult(
    val text: String? = null,
    val source: ExtractionSource = ExtractionSource.NONE,
    val ocrConfidenceAvg: Float? = null,
    val labels: List<String> = emptyList(),
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val perceptualHash: Long? = null,
    val thumbnailBytes: ByteArray? = null,
)

enum class ExtractionSource { NONE, TEXT_LAYER, OCR }

interface ContentExtractor {
    suspend fun extract(file: FileItem): ExtractionResult
}
