package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartfiles.core.model.SuggestionStatus

/**
 * Persisted album suggestion offered to the user for files whose classification
 * landed in the 60–85% band (LLD §4.3a, §7 "Album lifecycle"). Survives process
 * death so the user can act on it later from the Albums screen.
 */
@Entity(
    tableName = "album_suggestions",
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
            childColumns = ["suggestedAlbumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fileId"), Index("suggestedAlbumId"), Index("status")],
)
data class AlbumSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val suggestionId: Long = 0,
    val fileId: Long,
    val sourceAlbumId: Long? = null,
    val suggestedAlbumId: Long,
    val confidence: Float,
    /** Human-readable reasons, newline-joined. */
    val reasons: String = "",
    val status: SuggestionStatus = SuggestionStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
)