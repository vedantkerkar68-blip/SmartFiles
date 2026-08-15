package com.smartfiles.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.smartfiles.core.database.entity.ProcessingQueueEntity
import com.smartfiles.core.model.QueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Insert
    suspend fun insert(item: ProcessingQueueEntity): Long

    @Insert
    suspend fun insertAll(items: List<ProcessingQueueEntity>)

    @Query("UPDATE processing_queue SET status = 'PENDING', lastAttemptAt = NULL, nextEligibleAt = 0 WHERE queueId = :queueId")
    suspend fun reset(queueId: Long)

    @Query("UPDATE processing_queue SET status = 'DONE' WHERE queueId = :queueId")
    suspend fun markDone(queueId: Long)

    @Query("UPDATE processing_queue SET status = 'FAILED', retryCount = retryCount + 1, lastAttemptAt = :now, nextEligibleAt = :nextEligible WHERE queueId = :queueId")
    suspend fun markFailed(queueId: Long, now: Long, nextEligible: Long)

    @Query("UPDATE processing_queue SET status = 'IN_PROGRESS', lastAttemptAt = :now WHERE queueId = :queueId")
    suspend fun markInProgress(queueId: Long, now: Long)

    @Query("SELECT * FROM processing_queue WHERE status = 'PENDING' AND nextEligibleAt <= :now ORDER BY priority DESC, queueId ASC LIMIT :limit")
    suspend fun dequeueBatch(now: Long, limit: Int): List<ProcessingQueueEntity>

    @Query("SELECT COUNT(*) FROM processing_queue WHERE status IN ('PENDING','IN_PROGRESS')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM processing_queue WHERE status = 'PENDING' AND nextEligibleAt <= :now")
    suspend fun pendingCount(now: Long): Int

    @Query("SELECT f.fileId FROM files f WHERE f.processingStatus != 'INDEXED' AND f.isDeletedFromSource = 0 AND NOT EXISTS (SELECT 1 FROM processing_queue q WHERE q.fileId = f.fileId AND q.status IN ('PENDING','IN_PROGRESS')) LIMIT :limit")
    suspend fun getChangedFilesNotQueued(limit: Int): List<Long>

    @Query("SELECT queueId FROM processing_queue WHERE fileId = :fileId AND status IN ('PENDING','IN_PROGRESS')")
    suspend fun existingPendingForFile(fileId: Long): List<Long>
}
