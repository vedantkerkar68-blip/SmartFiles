package com.smartfiles.core.ml

import android.content.Context
import android.graphics.Bitmap
import com.smartfiles.core.common.AppLogger
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * OCR fallback for scanned/textless PDFs (LLD §4.2, Phase 2). Renders each page
 * with PdfBox's rasterizer (works on every API level the app supports, unlike
 * the platform PdfRenderer which moved packages in API 35+) and runs ML Kit
 * text recognition. Page count and render dimensions are capped so
 * multi-hundred-page documents cannot exhaust memory; bitmaps are recycled.
 */
class ScannedPdfOcrExtractor(
    private val context: Context,
    private val logger: AppLogger,
) {

    data class OcrOutput(
        val text: String,
        val avgConfidence: Float?,
        val recognizedPageCount: Int,
    )

    suspend fun ocr(pdfFile: File, maxPages: Int = MAX_PAGES): OcrOutput? =
        withContext(Dispatchers.IO) {
            var document: PDDocument? = null
            try {
                PDFBoxResourceLoader.init(context)
                document = PDDocument.load(pdfFile)
                if (document.isEncrypted) return@withContext null

                val renderer = PDFRenderer(document)
                val pages = document.numberOfPages.coerceAtMost(maxPages)
                val sb = StringBuilder()
                var confSum = 0f
                var confCount = 0
                var recognized = 0

                for (i in 0 until pages) {
                    val page = document.getPage(i)
                    val wPt = page.cropBox.width
                    val hPt = page.cropBox.height
                    if (wPt <= 0f || hPt <= 0f) continue
                    val scale = minOf(
                        OCR_TARGET_DPI / 72f,
                        MAX_RENDER_DIM / maxOf(wPt, hPt),
                    )
                    val bitmap = renderPage(renderer, i, 72f * scale) ?: continue
                    try {
                        val result = MlKitEngine.recognise(bitmap)
                        if (result.text.isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append('\n')
                            sb.append(result.text)
                            recognized++
                        }
                        val conf = result.avgConfidence
                        if (conf != null) {
                            confSum += conf
                            confCount++
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }

                val avg = if (confCount > 0) confSum / confCount else null
                OcrOutput(sb.toString(), avg, recognized)
            } catch (e: Exception) {
                logger.w(TAG, "scanned PDF OCR failed", e)
                null
            } finally {
                document?.close()
            }
        }

    private fun renderPage(renderer: PDFRenderer, pageIndex: Int, dpi: Float): Bitmap? =
        try {
            // pdfbox-android's PDFRenderer renders directly to an Android Bitmap.
            renderer.renderImageWithDPI(pageIndex, dpi)
        } catch (e: Exception) {
            logger.w(TAG, "page render failed (index $pageIndex)", e)
            null
        }

    companion object {
        private const val TAG = "ScannedPdfOcrExtractor"
        private const val OCR_TARGET_DPI = 160
        private const val MAX_RENDER_DIM = 2500
        private const val MAX_PAGES = 30
    }
}