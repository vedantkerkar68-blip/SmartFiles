package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** LLD §2.2 — FileTagCrossRef. */
@Entity(
    tableName = "file_tag_cross_ref",
    primaryKeys = ["fileId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["tagId"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class FileTagCrossRef(
    val fileId: Long,
    val tagId: Long,
    val confidence: Float,
)
