package com.smartfiles.domain

import com.smartfiles.core.model.AlbumSuggestion
import com.smartfiles.core.model.AssignmentSource
import javax.inject.Inject

/** What a classification pass did with a file (LLD §4.3a decision gates). */
sealed interface ClassificationOutcome {
    data object None : ClassificationOutcome
    data class Assigned(val albumId: Long, val confidence: Float, val tags: List<String>) : ClassificationOutcome
    data class Suggested(val suggestion: AlbumSuggestion) : ClassificationOutcome
}

/**
 * Orchestrates Level-3 classification for one file (HLD UC3): builds the
 * representative text from the indexed content, classifies it, persists tags,
 * then applies the confidence gates — auto-assign above [AppSettings.autoClassifyThreshold],
 * suggest in the 60–85% band, hold otherwise.
 */
class ClassifyFileUseCase @Inject constructor(
    private val fileRepository: FileRepository,
    private val classificationEngine: ClassificationEngine,
    private val albumRepository: AlbumRepository,
    private val tagRepository: TagRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(fileId: Long): ClassificationOutcome {
        val source = fileRepository.classificationSource(fileId) ?: return ClassificationOutcome.None
        val representativeText = buildString {
            append(source.displayName)
            source.extractedText?.let {
                append('\n')
                append(it.take(MAX_REPRESENTATIVE_CHARS))
            }
        }

        val result = classificationEngine.classify(representativeText, fileId)

        if (result.suggestedTags.isNotEmpty()) {
            tagRepository.replaceTagsForFile(fileId, result.suggestedTags.map { TagCandidate(it, result.confidence) })
        }

        val albumId = result.albumId ?: return ClassificationOutcome.None
        val settings = settingsRepository.get()
        return when {
            result.confidence >= settings.autoClassifyThreshold -> {
                albumRepository.assign(fileId, albumId, result.confidence, AssignmentSource.AUTO)
                ClassificationOutcome.Assigned(albumId, result.confidence, result.suggestedTags)
            }
            result.confidence >= settings.suggestThreshold -> {
                val suggestion = AlbumSuggestion(
                    fileId = fileId,
                    sourceAlbumId = null,
                    suggestedAlbumId = albumId,
                    confidence = result.confidence,
                    reasons = result.reasoning,
                )
                albumRepository.offerSuggestion(suggestion)
                ClassificationOutcome.Suggested(suggestion)
            }
            else -> ClassificationOutcome.None
        }
    }

    companion object {
        private const val MAX_REPRESENTATIVE_CHARS = 6_000
    }
}