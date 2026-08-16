package com.smartfiles.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.smartfiles.core.common.AppLogger
import com.smartfiles.domain.FolderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily re-validation of SAF grants (LLD §4.10, §4.11). Lapsed permissions are
 * flagged NEEDS_REGRANT; the existing index is never deleted.
 */
@HiltWorker
class PermissionValidationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val folderRepository: FolderRepository,
    private val logger: AppLogger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        folderRepository.refreshPermissionStates()
        Result.success()
    } catch (e: Exception) {
        logger.e(TAG, "permission validation failed", e)
        Result.retry()
    }

    companion object {
        private const val TAG = "PermissionValidationWorker"
    }
}
