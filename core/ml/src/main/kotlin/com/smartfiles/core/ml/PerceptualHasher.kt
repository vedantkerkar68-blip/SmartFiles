package com.smartfiles.core.ml

import android.graphics.Bitmap

/**
 * 64-bit perceptual hash (average-hash of an 8x8 grayscale downsample). Used for
 * near-duplicate image detection (LLD §4.7). Purely CPU, no dependency. The
 * returned value's 64 bits are compared with [hamming].
 */
object PerceptualHasher {
    private const val SIZE = 8

    fun hash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, false)
        val pixels = IntArray(SIZE * SIZE)
        scaled.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        if (scaled !== bitmap) scaled.recycle()

        val grayscale = FloatArray(SIZE * SIZE)
        var sum = 0f
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val gray = 0.299f * r + 0.587f * g + 0.114f * b
            grayscale[i] = gray
            sum += gray
        }
        val mean = sum / grayscale.size

        var hash = 0L
        for (i in grayscale.indices) {
            if (grayscale[i] >= mean) hash = hash or (1L shl i)
        }
        return hash
    }

    fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
