package com.smartfiles.domain

import com.smartfiles.core.model.AssignmentSource

/** Parses a natural-language query into an intent (LLD §4.6, §12). */
class SearchFilesUseCase(
    private val searchRepository: SearchRepository,
    private val queryIntentParser: QueryIntentParser,
) {
    suspend operator fun invoke(rawQuery: String): List<RankedSearchResult> =
        searchRepository.search(queryIntentParser.parse(rawQuery))
}

class GetRelatedFilesUseCase(
    private val searchRepository: SearchRepository,
) {
    suspend operator fun invoke(fileId: Long): List<RankedSearchResult> =
        searchRepository.relatedFiles(fileId)
}

class ApplyUserCorrectionUseCase(
    private val correctionRepository: UserCorrectionRepository,
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(correction: UserCorrection) {
        correctionRepository.recordCorrection(correction)
        correction.correctedAlbumId?.let { target ->
            if (correction.fileId > 0) {
                albumRepository.assign(correction.fileId, target, confidence = 1f, assignedBy = AssignmentSource.USER)
            }
        }
    }
}

/** Query intent parser contract implemented in the data layer (LLD §4.6). */
interface QueryIntentParser {
    fun parse(rawQuery: String): ParsedQuery
}
