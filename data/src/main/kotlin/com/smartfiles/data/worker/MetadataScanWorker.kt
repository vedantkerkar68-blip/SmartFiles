package com.smartfiles.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.common.CoroutineDispatchers
import com.smartfiles.core.model.DiscoveredFile
import com.smartfiles.domain.FileRepository
import com.smartfiles.domain.FileSource
import com.smartfiles.domain.FolderRepository
import com.smartfiles.domain.MediaFileDiscoverySource
import com.smartfiles.domain.ProcessingQueueRepository
import com.smartfiles.domain.ScanFilesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext

/**
 * Level-1 metadata scan (LLD §4.1, §4.10). Walks every granted SAF tree plus the
 * MediaStore collections, syncs change detection, and enqueues deep processing
 * for new/changed files. The app stays usable while this runs.
 */
@HiltWorker
class MetadataScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val folderRepository: FolderRepository,
    private val safFileSource: FileSource,
    private val mediaSource: MediaFileDiscoverySource,
    private val fileRepository: FileRepository,
    private val processingQueueRepository: ProcessingQueueRepository,
    private val dispatchers: CoroutineDispatchers,
    private val logger: AppLogger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(dispatchers.io) {
        try {
            val roots = folderRepository.activeUris()
            val discovered = mutableListOf<DiscoveredFile>()
            for (root in roots) {
                discovered += safFileSource.listFilesInTree(root, ScanFilesUseCase.DEFAULT_MAX_DEPTH)
            }
            // MediaStore media needs no tree grant — include it automatically.
            discovered += mediaSource.listIndexedMedia()

            val changed = fileRepository.syncScan(discovered)
            if (changed.isNotEmpty()) {
                processingQueueRepository.enqueue(changed, targetLevel = ScanFilesUseCase.TARGET_LEVEL_ALL)
            }
            Result.success()
        } catch (e: Exception) {
            logger.e(TAG, "metadata scan failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "MetadataScanWorker"
    }
}
