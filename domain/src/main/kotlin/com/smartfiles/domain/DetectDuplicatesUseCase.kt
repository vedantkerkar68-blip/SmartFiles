package com.smartfiles.domain

/** Runs a duplicate scan across all file categories (LLD §4.7). */
class DetectDuplicatesUseCase(
    private val duplicateDetectionRepository: DuplicateDetectionRepository,
) {
    suspend fun scanAll() {
        duplicateDetectionRepository.scanForExactDuplicates()
        duplicateDetectionRepository.scanForPerceptualDuplicates()
    }
}
