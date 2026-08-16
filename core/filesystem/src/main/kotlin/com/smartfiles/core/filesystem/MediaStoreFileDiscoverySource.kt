package com.smartfiles.core.filesystem

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.model.DiscoveredFile
import com.smartfiles.domain.MediaFileDiscoverySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaStore-backed discovery for broadly indexed media (camera roll, downloads
 * on API 29+). Requires no tree grant. Results are de-duplicated by URI because
 * the Downloads collection overlaps the image collection.
 */
class MediaStoreFileDiscoverySource(
    private val context: Context,
    private val logger: AppLogger,
) : MediaFileDiscoverySource {

    override suspend fun listIndexedMedia(): List<DiscoveredFile> = withContext(Dispatchers.IO) {
        val seen = HashSet<String>()
        val result = mutableListOf<DiscoveredFile>()
        result += query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ID, DISPLAY_NAME, MIME_TYPE, SIZE, DATE_MODIFIED,
            seen,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            result += query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ID, DISPLAY_NAME, MIME_TYPE, SIZE, DATE_MODIFIED,
                seen,
            )
        }
        result
    }

    private fun query(
        collection: android.net.Uri,
        idCol: String,
        nameCol: String,
        mimeCol: String,
        sizeCol: String,
        dateCol: String,
        seen: MutableSet<String>,
    ): List<DiscoveredFile> {
        val out = mutableListOf<DiscoveredFile>()
        try {
            context.contentResolver.query(
                collection,
                arrayOf(idCol, nameCol, mimeCol, sizeCol, dateCol),
                null,
                null,
                "$idCol ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(idCol))
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    if (!seen.add(uri)) continue
                    val mime = cursor.getString(cursor.getColumnIndexOrThrow(mimeCol))
                        ?: "application/octet-stream"
                    out += DiscoveredFile(
                        uri = uri,
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow(nameCol))
                            ?: "unnamed",
                        mimeType = mime,
                        sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(sizeCol)),
                        // MediaStore DATE_MODIFIED is in seconds; the rest of the
                        // app uses epoch millis.
                        dateModifiedSource = cursor.getLong(cursor.getColumnIndexOrThrow(dateCol)) * 1000L,
                        docType = MimeTypeMapper.docTypeFor(mime),
                    )
                }
            }
        } catch (e: Exception) {
            logger.w(TAG, "MediaStore query failed for $collection", e)
        }
        return out
    }

    companion object {
        private const val TAG = "MediaStoreFileDiscoverySource"
        private const val ID = MediaStore.Images.Media._ID
        private const val DISPLAY_NAME = MediaStore.Images.Media.DISPLAY_NAME
        private const val MIME_TYPE = MediaStore.Images.Media.MIME_TYPE
        private const val SIZE = MediaStore.Images.Media.SIZE
        private const val DATE_MODIFIED = MediaStore.Images.Media.DATE_MODIFIED
    }
}
