package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smartfiles.core.model.QueueStatus

/** LLD §2.2 — ProcessingQueueEntity (persistent, resumable queue). */
@Entity(
    tableName = "processing_queue",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["fileId"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("status"), Index("fileId")],
)
data class ProcessingQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
    val fileId: Long,
    val targetLevel: Int,
    val priority: Int = 0,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val nextEligibleAt: Long = 0,
    val status: QueueStatus = QueueStatus.PENDING,
)
