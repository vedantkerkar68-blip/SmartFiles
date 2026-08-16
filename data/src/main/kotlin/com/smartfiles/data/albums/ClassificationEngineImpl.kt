package com.smartfiles.data.albums

import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.database.dao.AlbumDao
import com.smartfiles.core.database.dao.FileDao
import com.smartfiles.core.model.DocType
import com.smartfiles.domain.ClassificationEngine
import com.smartfiles.domain.ClassificationResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Level-3 classification engine (LLD §4.3). Resolves the lexicon prediction to
 * a concrete seed album, assembles file-level evidence (keyword match, centroid
 * similarity when available, agreement among already-classified similar files),
 * scores it with [ConfidenceScorer], and returns the classification with its
 * human-readable reasoning.
 *
 * The centroid component is wired but inert until Phase 4 produces embeddings —
 * [ConfidenceScorer] renormalizes over whatever evidence is actually present, so
 * this never fabricates confidence from missing signals.
 */
@Singleton
class ClassificationEngineImpl @Inject constructor(
    private val fileDao: FileDao,
    private val albumDao: AlbumDao,
    private val localStrategy: LocalClassificationStrategy,
    private val tagExtractor: TagExtractor,
    private val logger: AppLogger,
) : ClassificationEngine {

    override suspend fun classify(representativeText: String, fileId: Long): ClassificationResult {
        val docType = fileDao.docTypeOf(fileId)
        val prediction = localStrategy.classifyCategory(representativeText)

        // Images with no textual evidence are assigned to Photos on media-type
        // evidence alone (a scanned ID still wins on content if it has any).
        if (prediction.category == null && docType == DocType.IMAGE) {
            val photos = albumDao.getByName(PHOTOS_ALBUM)
            if (photos != null) {
                return ClassificationResult(
                    albumId = photos.albumId,
                    confidence = PHOTOS_CONFIDENCE,
                    reasoning = listOf("Media type: image with no extractable text"),
                )
            }
        }
        if (prediction.category == null) return ClassificationResult()

        val categoryAlbum = albumDao.getByName(prediction.category.name)
            ?: run {
                logger.w(TAG, "seed album '${prediction.category.name}' missing; classification skipped")
                return ClassificationResult()
            }

        val evidence = ConfidenceScorer.Evidence(
            categoryKeywordScore = prediction.keywordScore,
            embeddingToCentroidScore = null, // Phase 4 (embeddings + album centroids)
            existingClusterAgreement = clusterAgreement(representativeText, categoryAlbum.albumId),
            userHistoryPrior = null, // Phase 6 (correction history)
        )
        val verdict = ConfidenceScorer.score(evidence)
        val tags = tagExtractor.extract(representativeText).map { it.first }

        return ClassificationResult(
            albumId = categoryAlbum.albumId,
            confidence = verdict.confidence,
            reasoning = reasoningFor(prediction, verdict),
            suggestedTags = tags,
        )
    }

    private suspend fun clusterAgreement(text: String, targetAlbumId: Long): Float? {
        val target = TermProfiles.of(text)
        val sample = fileDao.recentlyClassifiedProfiles(AGREEMENT_SAMPLE)
        if (sample.isEmpty()) return null
        var similar = 0
        var agree = 0
        for (row in sample) {
            val profile = TermProfiles.of(row.extractedText)
            if (TermProfiles.cosine(target, profile) >= AGREEMENT_MIN_SIMILARITY) {
                similar++
                if (row.primaryAlbumId == targetAlbumId) agree++
            }
        }
        return if (similar == 0) null else agree / similar.toFloat()
    }

    private fun reasoningFor(prediction: CategoryPrediction, verdict: ConfidenceScorer.Verdict): List<String> =
        buildList {
            prediction.category?.let { add("Category: ${it.displayName}") }
            prediction.matchedTerms.take(5).forEach { add("Matched term: $it") }
            addAll(verdict.reasoning)
            if (verdict.coverage < 1f) {
                add("Evidence coverage: ${(verdict.coverage * 100).roundToInt()}%")
            }
        }

    companion object {
        private const val TAG = "ClassificationEngine"
        private const val PHOTOS_ALBUM = "Photos"
        private const val PHOTOS_CONFIDENCE = 0.9f
        private const val AGREEMENT_SAMPLE = 40
        private const val AGREEMENT_MIN_SIMILARITY = 0.4f
    }
}