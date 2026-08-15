package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartfiles.core.model.AlbumType

/** LLD §2.2 — AlbumEntity. */
@Entity(
    tableName = "albums",
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["albumId"],
            childColumns = ["parentAlbumId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("parentAlbumId"), Index(value = ["name"], unique = true)],
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val albumId: Long = 0,
    val name: String,
    val parentAlbumId: Long? = null,
    val type: AlbumType,
    val confidence: Float? = null,
    val createdAutomatically: Boolean = false,
    val centroidEmbedding: ByteArray? = null,
    val iconOrEmoji: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
