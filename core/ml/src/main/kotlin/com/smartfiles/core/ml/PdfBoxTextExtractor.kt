package com.smartfiles.core.ml

import android.content.Context
import com.smartfiles.core.common.AppLogger
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** PDF text-layer extraction using PdfBox-Android (LLD §4.2). */
class PdfBoxTextExtractor(
    private val context: Context,
    private val logger: AppLogger,
) {
    suspend fun open(input: java.io.InputStream, onOpen: (PDDocument, Int) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                PDFBoxResourceLoader.init(context)
                PDDocument.load(input).use { doc ->
                    if (doc.isEncrypted) {
                        onOpen(doc, doc.numberOfPages)
                    } else {
                        onOpen(doc, doc.numberOfPages)
                    }
                }
                true
            } catch (e: Exception) {
                logger.w(TAG, "Failed to open/parse PDF", e)
                false
            }
        }

    suspend fun extractText(input: java.io.InputStream): String? = withContext(Dispatchers.IO) {
        try {
            PDFBoxResourceLoader.init(context)
            PDDocument.load(input).use { doc ->
                if (doc.isEncrypted) null
                else PDFTextStripper().getText(doc)
            }
        } catch (e: Exception) {
            logger.w(TAG, "PDF text extraction failed", e)
            null
        }
    }

    companion object { private const val TAG = "PdfBoxTextExtractor" }
}
