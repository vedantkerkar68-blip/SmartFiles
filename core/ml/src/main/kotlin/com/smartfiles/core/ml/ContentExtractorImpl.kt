package com.smartfiles.core.ml

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.model.DocType
import com.smartfiles.core.model.FileItem
import com.smartfiles.domain.ContentExtractor
import com.smartfiles.domain.ExtractionResult
import com.smartfiles.domain.ExtractionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Content extraction entry point (LLD §4.2). Dispatches to PdfBox for
 * text-layer PDFs and ML Kit for images (OCR + labels). PdfBox page-rendering
 * OCR fallback for scanned PDFs is added in Phase 2 (see DECISIONS.md).
 */
class ContentExtractorImpl(
    private val context: Context,
    private val pdfExtractor: PdfBoxTextExtractor,
    private val logger: AppLogger,
) : ContentExtractor {

    private val minPdfChars: Int = 100

    override suspend fun extract(file: FileItem): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            when (file.docType) {
                DocType.PDF -> extractPdf(file)
                DocType.IMAGE -> extractImage(file)
                else -> ExtractionResult(source = ExtractionSource.NONE)
            }
        } catch (e: Exception) {
            logger.w(TAG, "extract failed for ${file.displayName}", e)
            ExtractionResult(source = ExtractionSource.NONE)
        }
    }

    private suspend fun extractPdf(file: FileItem): ExtractionResult {
        val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(Uri.parse(file.uri))?.use { input ->
                pdfExtractor.extractText(input)
            }
        } ?: return ExtractionResult(source = ExtractionSource.NONE)

        if (text.isBlank() || text.length < minPdfChars) {
            // Scanned / textless PDF: page-rendered OCR lands in Phase 2 with PDFRenderer.
            return ExtractionResult(source = ExtractionSource.NONE)
        }
        return ExtractionResult(
            text = text,
            source = ExtractionSource.TEXT_LAYER,
        )
    }

    private suspend fun extractImage(file: FileItem): ExtractionResult {
        val uri = Uri.parse(file.uri)
        val bitmap = ImageBitmapDecoder.decode(context, uri) ?: return ExtractionResult(source = ExtractionSource.NONE)

        val text = MlKitEngine.recognise(bitmap)
        val labels = MlKitEngine.label(bitmap).map { it.first }
        val hash = PerceptualHasher.hash(bitmap)
        val thumbnail = makeThumbnail(bitmap)

        return ExtractionResult(
            text = text.text.ifBlank { null },
            source = if (text.text.isNotBlank()) ExtractionSource.OCR else ExtractionSource.NONE,
            ocrConfidenceAvg = if (text.text.isNotBlank()) text.avgConfidence else null,
            labels = labels,
            widthPx = bitmap.width,
            heightPx = bitmap.height,
            perceptualHash = hash,
            thumbnailBytes = thumbnail,
        )
    }

    private fun makeThumbnail(bitmap: Bitmap): ByteArray? = try {
        val scale = minOf(1f, THUMB_MAX.toFloat() / maxOf(bitmap.width, bitmap.height))
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val thumb = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val out = ByteArrayOutputStream()
        thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)
        if (thumb !== bitmap) thumb.recycle()
        out.toByteArray()
    } catch (e: Exception) {
        logger.w(TAG, "thumbnail failed", e)
        null
    }

    companion object {
        private const val TAG = "ContentExtractorImpl"
        private const val THUMB_MAX = 256
    }
}
