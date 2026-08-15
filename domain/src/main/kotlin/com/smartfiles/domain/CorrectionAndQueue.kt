package com.smartfiles.domain

import com.smartfiles.core.model.CorrectionType

data class UserCorrection(
    val fileId: Long,
    val previousAlbumId: Long? = null,
    val correctedAlbumId: Long? = null,
    val correctionType: CorrectionType,
)

interface UserCorrectionRepository {
    suspend fun recordCorrection(correction: UserCorrection)
}

/** Tracks processed/queued state and exposes progress without leaking DB details. */
data class ProcessingStatusSnapshot(
    val pendingCount: Int,
    val inProgressCount: Int,
    val indexedCount: Long,
)

interface ProcessingQueueRepository {
    suspend fun enqueue(fileIds: List<Long>, targetLevel: Int, priority: Int = 0)
    suspend fun statusSnapshot(): ProcessingStatusSnapshot
    fun observeStatus(): kotlinx.coroutines.flow.Flow<ProcessingStatusSnapshot>
}
