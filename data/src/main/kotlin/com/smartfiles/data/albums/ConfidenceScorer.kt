package com.smartfiles.data.albums

import kotlin.math.roundToInt

/**
 * Composite file-level classification confidence (LLD §4.3a).
 *
 *     confidence = 0.45·keyword + 0.35·centroid + 0.15·clusterAgreement + 0.05·historyPrior
 *
 * Evidence that is *not available yet* contributes its weight to neither the
 * numerator nor the denominator, so the returned score stays on the 0..1 scale
 * (no artificial inflation from missing signals). [Verdict.coverage] reports the
 * share of evidence actually present, so callers can see whether a high score is
 * built on real signal or on thin evidence.
 */
object ConfidenceScorer {

    const val W_KEYWORD = 0.45f
    const val W_CENTROID = 0.35f
    const val W_CLUSTER = 0.15f
    const val W_HISTORY = 0.05f

    /** Neutral prior used when the user has no correction history for a category. */
    const val NEUTRAL_HISTORY_PRIOR = 0.5f

    data class Evidence(
        /** Lexicon/rule match strength for the predicted category, 0..1. */
        val categoryKeywordScore: Float? = null,
        /** Cosine similarity to the nearest album centroid; null before Phase 4. */
        val embeddingToCentroidScore: Float? = null,
        /** Fraction of similar already-classified files that agree; null when none. */
        val existingClusterAgreement: Float? = null,
        /** Past-correction signal; null uses the neutral prior. */
        val userHistoryPrior: Float? = null,
    )

    data class Verdict(
        val confidence: Float,
        /** Sum of weights of evidence that was actually provided (0..1). */
        val coverage: Float,
        val reasoning: List<String>,
    )

    fun score(e: Evidence): Verdict {
        val prior = e.userHistoryPrior ?: NEUTRAL_HISTORY_PRIOR
        val evidence = listOf(
            Scored(W_KEYWORD, "keyword", e.categoryKeywordScore),
            Scored(W_CENTROID, "centroid", e.embeddingToCentroidScore),
            Scored(W_CLUSTER, "cluster agreement", e.existingClusterAgreement),
            Scored(W_HISTORY, "history prior", prior),
        )
        val available = evidence.filter { it.score != null }
        val availableWeight = available.fold(0f) { acc, s -> acc + s.weight }
        val weighted = available.fold(0f) { acc, s -> acc + s.weight * (s.score ?: 0f) }
        val confidence = if (availableWeight > 0f) (weighted / availableWeight).coerceIn(0f, 1f) else 0f
        val reasoning = available.map { sc ->
            "${sc.label}: ${(sc.score!! * 100).roundToInt()}%"
        }
        return Verdict(
            confidence = confidence,
            coverage = availableWeight.coerceIn(0f, 1f),
            reasoning = reasoning,
        )
    }

    private data class Scored(val weight: Float, val label: String, val score: Float?)
}