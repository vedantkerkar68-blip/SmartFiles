package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** LLD §2.2 — EmbeddingEntity, keyed by (fileId, modelVersion) for safe model upgrades. */
@Entity(
    tableName = "embeddings",
    primaryKeys = ["fileId", "modelVersion"],
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fileId")],
)
data class EmbeddingEntity(
    val fileId: Long,
    val modelVersion: String,
    val vector: ByteArray,
    val dim: Int,
    val generatedAt: Long = System.currentTimeMillis(),
)
