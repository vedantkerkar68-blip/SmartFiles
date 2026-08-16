package com.smartfiles.domain

import com.smartfiles.core.model.DiscoveredFile

/**
 * Abstraction over the filesystem access layer (SAF / MediaStore). Implemented
 * in core:filesystem and injected into the data layer. Keeps DOMAIN unaware of
 * Android storage mechanics.
 */
interface FileSource {
    /** Enumerate documents inside a granted tree, with a bounded depth. */
    suspend fun listFilesInTree(rootUri: String, maxDepth: Int): List<DiscoveredFile>
    /** Resolve the document-tree tree/document id from a persisted uri. */
    fun isTreeGranted(uri: String): Boolean
    suspend fun getDocumentFileUri(mimeType: String, fileName: String): String?
    suspend fun resolveDisplayName(uri: String): String?
    /** Returns true if a previously granted uri is still accessible. */
    fun canAccess(uri: String): Boolean
}

/**
 * Broad, grant-free discovery of media the OS already indexes (MediaStore),
 * so the user is not forced to grant a whole tree just to include media.
 */
interface MediaFileDiscoverySource {
    suspend fun listIndexedMedia(): List<DiscoveredFile>
}
