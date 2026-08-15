package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** LLD §2.2 — DuplicateGroupMemberEntity. */
@Entity(
    tableName = "duplicate_group_members",
    primaryKeys = ["groupId", "fileId"],
    foreignKeys = [
        ForeignKey(
            entity = DuplicateGroupEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fileId")],
)
data class DuplicateGroupMemberEntity(
    val groupId: Long,
    val fileId: Long,
    val similarityScore: Float,
)
