package com.smartfiles.core.model

/**
 * A discovered file, as seen by the discovery layer. Deliberately Android-free
 * (no [android.net.Uri]) so the domain stays JVM-testable; the URI is carried as
 * an opaque String.
 */
data class DiscoveredFile(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModifiedSource: Long,
    val parentDocumentId: String? = null,
    val docType: DocType,
)

/** Change-detection signature cached per file (LLD §4.1). */
data class ChangeSignature(
    val sizeBytes: Long,
    val dateModifiedSource: Long,
    val sha256Hash: String?,
)

data class FileItem(
    val fileId: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModifiedSource: Long,
    val docType: DocType,
    val processingStatus: ProcessingStatus,
    val processingLevel: Int,
    val primaryAlbumId: Long? = null,
    val classificationConfidence: Float? = null,
    val extractedTextPreview: String? = null,
)

data class AlbumItem(
    val albumId: Long,
    val name: String,
    val parentAlbumId: Long? = null,
    val type: AlbumType,
    val confidence: Float? = null,
    val createdAutomatically: Boolean = false,
    val fileCount: Int = 0,
)

/** A file assignable to one or more albums, used for the self-referential tree. */
data class AlbumNode(
    val album: AlbumItem,
    val children: List<AlbumNode> = emptyList(),
)

data class TagItem(
    val tagId: Long,
    val name: String,
    val category: String? = null,
)

/** A single album suggestion offered to the user (60-85% confidence band). */
data class AlbumSuggestion(
    val fileId: Long,
    val sourceAlbumId: Long?,
    val suggestedAlbumId: Long,
    val confidence: Float,
    val reasons: List<String>,
)
