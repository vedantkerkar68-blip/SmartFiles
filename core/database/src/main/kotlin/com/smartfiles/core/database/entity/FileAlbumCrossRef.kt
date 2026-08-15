package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.smartfiles.core.model.AssignmentSource

/** LLD §2.2 — FileAlbumCrossRef (many-to-many, a file may be in >1 album). */
@Entity(
    tableName = "file_album_cross_ref",
    primaryKeys = ["fileId", "albumId"],
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("albumId")],
)
data class FileAlbumCrossRef(
    val fileId: Long,
    val albumId: Long,
    val confidence: Float,
    val assignedBy: AssignmentSource,
    val assignedAt: Long = System.currentTimeMillis(),
)
