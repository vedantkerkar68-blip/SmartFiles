package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartfiles.core.model.DocType
import com.smartfiles.core.model.ProcessingStatus

/** LLD §2.2 — FileEntity. */
@Entity(
    tableName = "files",
    indices = [Index("uri"), Index("sha256Hash"), Index("processingStatus"), Index("primaryAlbumId")],
)
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val fileId: Long = 0,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModifiedSource: Long,
    val dateFirstIndexed: Long = System.currentTimeMillis(),
    val sha256Hash: String? = null,
    val perceptualHash: Long? = null,
    val docType: DocType,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val extractedText: String? = null,
    val ocrApplied: Boolean = false,
    val ocrConfidenceAvg: Float? = null,
    val processingStatus: ProcessingStatus = ProcessingStatus.DISCOVERED,
    val processingLevel: Int = 0,
    val primaryAlbumId: Long? = null,
    val classificationConfidence: Float? = null,
    val isDeletedFromSource: Boolean = false,
    val lastError: String? = null,
    /** Denormalized space-joined tags used by FTS; refreshed on tag changes. */
    val tagsConcat: String? = null,
)
