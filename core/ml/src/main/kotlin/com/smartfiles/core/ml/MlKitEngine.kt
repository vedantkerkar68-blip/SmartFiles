package com.smartfiles.core.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** OCR + image labeling via on-device ML Kit (LLD §4.2). */
object MlKitEngine {

    private val textRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val labeler by lazy { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }

    data class Recognition(val text: String, val avgConfidence: Float?)

    suspend fun recognise(bitmap: Bitmap): Recognition {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = textRecognizer.process(image).await()
        val blocks = result.textBlocks
        val text = result.text
        var sum = 0f
        var count = 0
        for (block in blocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val conf = element.confidence
                    if (conf != null) {
                        sum += conf
                        count++
                    }
                }
            }
        }
        val avg = if (count > 0) sum / count else null
        return Recognition(text, avg)
    }

    suspend fun label(bitmap: Bitmap, topK: Int = 5): List<Pair<String, Float>> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val results = labeler.process(image).await()
        return results
            .sortedByDescending { it.confidence }
            .take(topK)
            .map { it.text to it.confidence }
    }
}
