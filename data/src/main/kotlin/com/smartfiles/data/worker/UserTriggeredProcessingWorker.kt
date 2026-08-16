package com.smartfiles.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.database.dao.QueueDao
import com.smartfiles.core.database.entity.ProcessingQueueEntity
import com.smartfiles.core.model.FileItem
import com.smartfiles.domain.ContentExtractor
import com.smartfiles.domain.FileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * "Process now" escape hatch: drains the full queue promptly with no battery
 * constraints (LLD §4.10). Scheduled as normal unique work so it cannot crash
 * on API-34 foreground-service-type rules; a foreground notification upgrade is
 * a later enhancement (see DECISIONS.md).
 */
@HiltWorker
class UserTriggeredProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val queueDao: QueueDao,
    private val fileRepository: FileRepository,
    private val contentExtractor: ContentExtractor,
    private val logger: AppLogger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            while (true) {
                val batch = queueDao.dequeueBatch(System.currentTimeMillis(), USER_BATCH_SIZE)
                if (batch.isEmpty()) break
                for (item in batch) {
                    processItem(item)
                }
            }
            Result.success()
        } catch (e: Exception) {
            logger.e(TAG, "user-triggered processing failed", e)
            Result.retry()
        }
    }

    private suspend fun processItem(item: ProcessingQueueEntity) {
        val now = System.currentTimeMillis()
        queueDao.markInProgress(item.queueId, now)
        try {
            val file: FileItem? = fileRepository.observeFile(item.fileId).first()
            if (file == null) {
                queueDao.markDone(item.queueId)
                return
            }
            val extraction = contentExtractor.extract(file)
            fileRepository.updateProcessingResult(item.fileId, extraction)
            queueDao.markDone(item.queueId)
        } catch (e: Exception) {
            logger.w(TAG, "processing failed for queue item ${item.queueId}", e)
            val backoff = RETRY_INITIAL_MS shl item.retryCount.coerceAtMost(MAX_EXPONENT)
            queueDao.markFailed(item.queueId, now, now + backoff)
        }
    }

    companion object {
        private const val TAG = "UserTriggeredProcessingWorker"
        private const val USER_BATCH_SIZE = 50
        private const val RETRY_INITIAL_MS = 15_000L
        private const val MAX_EXPONENT = 4
    }
}
