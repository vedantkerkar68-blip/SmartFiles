package com.smartfiles.core.filesystem

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.model.DiscoveredFile
import com.smartfiles.domain.FileSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Storage Access Framework implementation of [FileSource]. Walks granted
 * document trees (ACTION_OPEN_DOCUMENT_TREE + persistable perms) and maps
 * documents to [DiscoveredFile] metadata. Never modifies originals.
 */
class SafFileSource(
    private val context: Context,
    private val logger: AppLogger,
) : FileSource {

    /** cache of tree root -> set of directory ids to skip re-descending. */
    private val visitedTrees = ConcurrentHashMap<String, Boolean>()

    override suspend fun listFilesInTree(rootUri: String, maxDepth: Int): List<DiscoveredFile> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<DiscoveredFile>()
            if (maxDepth < 0 || maxDepth > MAX_DEPTH_GUARD) {
                logger.w(TAG, "maxDepth $maxDepth exceeds guard; clamping")
            }
            walk(DocumentFile.fromTreeUri(context, Uri.parse(rootUri)), 0, maxDepth, result)
            result
        }

    override fun isTreeGranted(uri: String): Boolean = try {
        val flags = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(uri), DocumentsContract.getTreeDocumentId(Uri.parse(uri)))
        context.contentResolver.getType(flags) != null
    } catch (e: Exception) {
        false
    }

    override fun canAccess(uri: String): Boolean = try {
        context.contentResolver.getType(Uri.parse(uri)) != null
    } catch (e: Exception) {
        false
    }

    override suspend fun getDocumentFileUri(mimeType: String, fileName: String): String? = null

    override suspend fun resolveDisplayName(uri: String): String? = withContext(Dispatchers.IO) {
        try {
            DocumentFile.fromSingleUri(context, Uri.parse(uri))?.name
        } catch (e: Exception) {
            logger.w(TAG, "resolveDisplayName failed", e)
            null
        }
    }

    private fun walk(dir: DocumentFile?, depth: Int, maxDepth: Int, out: MutableList<DiscoveredFile>) {
        if (dir == null) return
        if (!dir.isDirectory) return
        if (dir.name?.startsWith(".") == true) return
        if (depth > maxDepth) return

        val children = dir.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                val name = child.name
                if (name == null || name.startsWith(".") || name == EXCLUDE_DIR) continue
                walk(child, depth + 1, maxDepth, out)
            } else if (child.isFile) {
                val mime = child.type ?: "application/octet-stream"
                out += DiscoveredFile(
                    uri = child.uri.toString(),
                    displayName = child.name ?: "unnamed",
                    mimeType = mime,
                    sizeBytes = child.length() ?: 0L,
                    dateModifiedSource = child.lastModified() ?: 0L,
                    parentDocumentId = DocumentsContract.getDocumentId(child.uri),
                    docType = MimeTypeMapper.docTypeFor(mime),
                )
            }
        }
    }

    companion object {
        private const val TAG = "SafFileSource"
        private const val EXCLUDE_DIR = ".thumbnails"
        private const val MAX_DEPTH_GUARD = 64
    }
}
