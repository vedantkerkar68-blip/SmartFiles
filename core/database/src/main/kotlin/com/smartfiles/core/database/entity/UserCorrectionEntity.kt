package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.smartfiles.core.model.CorrectionType

/** LLD §2.2 — UserCorrectionEntity. */
@Entity(
    tableName = "user_corrections",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class UserCorrectionEntity(
    @PrimaryKey(autoGenerate = true) val correctionId: Long = 0,
    val fileId: Long,
    val previousAlbumId: Long?,
    val correctedAlbumId: Long?,
    val correctionType: CorrectionType,
    val timestamp: Long = System.currentTimeMillis(),
    /** Prevents repeated re-flagging of a "not a duplicate" pair. */
    val notDuplicatePair: String? = null,
)
