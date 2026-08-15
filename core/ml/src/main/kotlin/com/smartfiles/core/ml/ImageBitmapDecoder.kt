package com.smartfiles.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes images from a content URI into a bounded-size bitmap (downsampled),
 * so full-resolution copies are never retained (HLD §8). */
object ImageBitmapDecoder {

    const val MAX_DIMENSION = 1024

    suspend fun decode(context: Context, uri: Uri, maxDimension: Int = MAX_DIMENSION): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                // First, measure intrinsic bounds without decoding pixels.
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                val w = options.outWidth
                val h = options.outHeight
                if (w <= 0 || h <= 0) return@withContext null

                var sample = 1
                while (maxOf(w, h) / sample > maxDimension) sample *= 2

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                }
            } catch (e: Exception) {
                null
            }
        }
}
