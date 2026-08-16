package com.smartfiles.data.albums

import com.smartfiles.domain.ClassificationResult
import javax.inject.Inject

/** Text-level evidence produced by the local classification strategy. */
data class CategoryPrediction(
    val category: CategoryLexicon.Category?,
    val keywordScore: Float,
    val matchedTerms: List<String>,
)

/**
 * Default, always-available classification strategy (LLD §4.12). Pure lexicon
 * rule-matching over the curated seed taxonomy — no network, no learned model.
 * The domain [com.smartfiles.domain.ClassificationStrategy] seam means a future
 * opt-in cloud strategy can replace this implementation without touching the
 * rest of the pipeline.
 */
class LocalClassificationStrategy @Inject constructor() {

    /** Classifies text against the lexicon and returns the best top-level fit. */
    fun classifyCategory(text: String): CategoryPrediction {
        val toks = CategoryLexicon.tokens(text)
        val grams = toks + toks.zipWithNext { a, b -> "$a $b" }
        if (grams.isEmpty()) return CategoryPrediction(null, 0f, emptyList())

        var best: CategoryLexicon.Category? = null
        var bestScore = 0f
        var bestTerms = emptyList<String>()
        for (category in CategoryLexicon.TOP_LEVEL) {
            if (category.keywords.isEmpty()) continue // Photos/Uncategorized are media/default-driven
            var sum = 0f
            var matched = emptyList<String>()
            for (kw in category.keywords) {
                if (grams.any { CategoryLexicon.termMatches(kw.term, it) }) {
                    sum += kw.weight
                    matched += kw.term
                }
            }
            val score = (sum / MATCH_NORMALIZER).coerceIn(0f, 1f)
            if (score > bestScore) {
                bestScore = score
                best = category
                bestTerms = matched
            }
        }
        return if (best != null && bestScore >= MIN_CATEGORY_SCORE) {
            CategoryPrediction(best, bestScore, bestTerms)
        } else {
            CategoryPrediction(null, 0f, emptyList())
        }
    }

    /** Thin domain-interface adapter: text -> best-fit category signal. */
    suspend fun classify(text: String): ClassificationResult {
        val prediction = classifyCategory(text)
        return ClassificationResult(
            albumId = null,
            confidence = prediction.keywordScore,
            reasoning = prediction.category?.let { listOf("Matched category ${it.displayName}") } ?: emptyList(),
            suggestedTags = prediction.matchedTerms.take(5),
        )
    }

    companion object {
        /** Total keyword weight that saturates a category's keyword score. */
        private const val MATCH_NORMALIZER = 2.5f

        /** Minimum rule-match strength before a category is proposed at all. */
        private const val MIN_CATEGORY_SCORE = 0.35f
    }
}