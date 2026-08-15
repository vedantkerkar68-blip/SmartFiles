package com.smartfiles.core.common

/**
 * Compact float16 packing/unpacking for embedding vectors (LLD §2.4, §4.4).
 * Vectors are pre-normalized (unit length) at generation time so cosine
 * similarity reduces to a plain dot product, requiring no per-query
 * normalization pass. Storage uses the IEEE 754 half-precision format via
 * bit-manipulation so no external dependency is needed.
 */
object EmbeddingCodec {

    private const val FLOAT32_EXPONENT_BIAS = 127
    private const val FLOAT16_EXPONENT_BIAS = 15

    fun encode(vector: FloatArray): ByteArray {
        val bytes = ByteArray(vector.size * 2)
        var i = 0
        for (v in vector) {
            val bits = java.lang.Float.floatToRawIntBits(v)
            val half = floatToHalfBits(bits)
            bytes[i++] = (half and 0xFF).toByte()
            bytes[i++] = ((half ushr 8) and 0xFF).toByte()
        }
        return bytes
    }

    fun decode(bytes: ByteArray, expectedSize: Int): FloatArray {
        val out = FloatArray(expectedSize)
        var i = 0
        var b = 0
        while (b + 1 < bytes.size && i < expectedSize) {
            val half = (bytes[b].toInt() and 0xFF) or ((bytes[b + 1].toInt() and 0xFF) shl 8)
            out[i++] = halfBitsToFloat(half)
            b += 2
        }
        return out
    }

    /** Dot product. Assumes both vectors have already been normalized. */
    fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        var i = 0
        val n = minOf(a.size, b.size)
        while (i < n) {
            sum += a[i] * b[i]
            i++
        }
        return sum
    }

    /** Normalizes in place and returns the same array. */
    fun normalizeInPlace(v: FloatArray): FloatArray {
        var norm = 0f
        for (x in v) norm += x * x
        norm = kotlin.math.sqrt(norm)
        if (norm > 0f) {
            for (i in v.indices) v[i] = v[i] / norm
        }
        return v
    }

    private fun floatToHalfBits(value: Int): Int {
        val sign = (value ushr 16) and 0x8000
        var exponent = (value ushr 23) and 0xFF
        var mantissa = value and 0x7FFFFF

        if (exponent == 0xFF) {
            // NaN or infinity
            return sign or 0x7C00 or (if (mantissa == 0) 0 else 0x200)
        }
        val unbiased = exponent - FLOAT32_EXPONENT_BIAS
        if (unbiased > 15) {
            // Overflow -> infinity
            return sign or 0x7C00
        }
        return when {
            unbiased < -14 -> {
                // Subnormal half
                var shifted = mantissa or 0x800000
                val shift = (-unbiased - 1).coerceAtMost(24)
                val halfMantissa = shifted ushr (shift + 13)
                sign or halfMantissa
            }
            else -> {
                val halfExponent = (unbiased + FLOAT16_EXPONENT_BIAS) shl 10
                val halfMantissa = mantissa shr 13
                sign or halfExponent or halfMantissa
            }
        }
    }

    private fun halfBitsToFloat(half: Int): Float {
        val sign = (half ushr 15) and 0x1
        val exponent = (half ushr 10) and 0x1F
        val mantissa = half and 0x3FF
        val bits = when (exponent) {
            0 -> {
                if (mantissa == 0) 0 else {
                    // Subnormal -> normalize
                    var e = -1
                    var m = mantissa
                    do {
                        e++
                        m = m shl 1
                    } while ((m and 0x400) == 0)
                    (sign shl 31) or ((FLOAT32_EXPONENT_BIAS - FLOAT16_EXPONENT_BIAS - e) shl 23) or (m and 0x3FF shl 13)
                }
            }
            0x1F -> {
                (sign shl 31) or 0x7F800000 or ((mantissa shl 13) and 0x7FFFFF)
            }
            else -> {
                (sign shl 31) or ((exponent - FLOAT16_EXPONENT_BIAS + FLOAT32_EXPONENT_BIAS) shl 23) or (mantissa shl 13)
            }
        }
        return java.lang.Float.intBitsToFloat(bits)
    }
}
