package com.smartfiles.domain

import javax.inject.Inject

/**
 * Orchestrates a metadata scan: enumerates files from granted sources, syncs
 * Level-1 metadata with the repository (which applies change detection), and
 * enqueues deep processing for new/changed files (LLD §4.1, §6.1).
 */
class ScanFilesUseCase @Inject constructor(
    private val fileSource: FileSource,
    private val fileRepository: FileRepository,
    private val processingQueueRepository: ProcessingQueueRepository,
) {
    /** Returns the number of files enqueued for deep processing. */
    suspend operator fun invoke(rootUris: List<String>, maxDepth: Int = DEFAULT_MAX_DEPTH): Int {
        val discovered = rootUris.flatMap { fileSource.listFilesInTree(it, maxDepth) }
            .filter { it.sizeBytes <= MAX_FILE_SIZE }

        val changedIds = fileRepository.syncScan(discovered)

        if (changedIds.isNotEmpty()) {
            processingQueueRepository.enqueue(changedIds, targetLevel = TARGET_LEVEL_ALL)
        }
        return changedIds.size
    }

    companion object {
        const val DEFAULT_MAX_DEPTH = 8
        const val MAX_FILE_SIZE = 512L * 1024 * 1024
        const val TARGET_LEVEL_ALL = 3
    }
}
