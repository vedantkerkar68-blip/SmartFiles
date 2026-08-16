package com.smartfiles.data.albums

import com.smartfiles.domain.AppSettings
import kotlin.math.sqrt

/**
 * Term-profile models and clustering helpers for the DynamicAlbumCreator sweep
 * (LLD §4.3b). A profile is a term-frequency bag over the file's extracted text.
 * Cosine similarity over these profiles plays the role that embedding cosine
 * plays once Phase 4 provides vectors; the two are kept behind the same
 * [DynamicAlbumCreator.evaluate] seam so the algorithm is identical either way.
 */
object TermProfiles {

    /** Builds a term-frequency profile, bounded to [maxChars] of input. */
    fun of(text: String, maxChars: Int = MAX_PROFILE_CHARS): Map<String, Float> {
        val counts = HashMap<String, Float>()
        for (token in CategoryLexicon.tokens(text.take(maxChars))) {
            counts.merge(token, 1f, Float::plus)
        }
        return counts
    }

    fun cosine(a: Map<String, Float>, b: Map<String, Float>): Float {
        var dot = 0.0
        for ((k, v) in a) {
            val other = b[k] ?: continue
            dot += v * other
        }
        if (dot <= 0.0) return 0f
        val na = a.values.sumOf { (it * it).toDouble() }
        val nb = b.values.sumOf { (it * it).toDouble() }
        val denom = sqrt(na) * sqrt(nb)
        return if (denom > 0.0) (dot / denom).toFloat() else 0f
    }

    /** Mean of profiles; null when [members] is empty. */
    fun centroid(members: List<ClusterMember>): Map<String, Float>? {
        if (members.isEmpty()) return null
        val sums = HashMap<String, Float>()
        for (m in members) for ((k, v) in m.profile) sums.merge(k, v, Float::plus)
        val n = members.size
        return sums.mapValues { it.value / n }
    }

    private const val MAX_PROFILE_CHARS = 4_000
}

data class ClusterMember(
    val fileId: Long,
    val displayName: String,
    val profile: Map<String, Float>,
)

enum class NewAlbumDecision { AUTO_CREATE, SUGGEST_TO_USER, KEEP_UNCATEGORIZED }

/** Greedy single-pass clustering: each member joins the first existing cluster
 * whose centroid it resembles; otherwise it seeds a new cluster. */
object Clusterer {
    fun greedy(members: List<ClusterMember>, minSimilarity: Float): List<List<ClusterMember>> {
        val clusters = mutableListOf<MutableList<ClusterMember>>()
        val centroids = mutableListOf<Map<String, Float>>()
        for (member in members) {
            var target: Int = -1
            var best = minSimilarity
            for ((i, c) in centroids.withIndex()) {
                val sim = TermProfiles.cosine(member.profile, c)
                if (sim > best) {
                    best = sim
                    target = i
                }
            }
            if (target >= 0) {
                clusters[target].add(member)
                centroids[target] = TermProfiles.centroid(clusters[target]) ?: continue
            } else {
                clusters.add(mutableListOf(member))
                centroids.add(member.profile)
            }
        }
        return clusters
    }
}

/**
 * Cluster-level evidence checks for creating *new* sub-albums (LLD §4.3b).
 *
 * The auto-create branch requires distinctness from existing albums
 * ([maxSimilarityToExisting]). When that value is unknown (no album centroids
 * yet, as in Phase 3), distinctness evidence is treated as absent so
 * AUTO_CREATE is gated off; the suggest branch (size + cohesion only) still
 * runs, which is how dynamic sub-albums surface to the user today.
 */
class DynamicAlbumCreator {

    fun evaluate(
        cluster: List<ClusterMember>,
        settings: AppSettings,
        maxSimilarityToExisting: Float? = null,
    ): NewAlbumDecision {
        val size = cluster.size
        if (size < MIN_SUGGEST_CLUSTER_SIZE) return NewAlbumDecision.KEEP_UNCATEGORIZED

        val cohesion = cohesionOf(cluster)
        val distinctiveness = maxSimilarityToExisting?.let { 1f - it } ?: 0f

        return when {
            size >= settings.newAlbumMinClusterSize &&
                cohesion >= settings.newAlbumCohesionThreshold &&
                distinctiveness >= settings.newAlbumDistinctivenessThreshold ->
                NewAlbumDecision.AUTO_CREATE

            size >= MIN_SUGGEST_CLUSTER_SIZE && cohesion >= SUGGEST_COHESION_THRESHOLD ->
                NewAlbumDecision.SUGGEST_TO_USER

            else -> NewAlbumDecision.KEEP_UNCATEGORIZED
        }
    }

    /** Best-effort display name from the most frequent non-stopword term. */
    fun nameFor(cluster: List<ClusterMember>): String? {
        val counts = HashMap<String, Int>()
        for (m in cluster) for (term in m.profile.keys) {
            if (term.length >= 4 && CategoryLexicon.byName(term) == null) counts.merge(term, 1, Int::plus)
        }
        val best = counts.maxByOrNull { it.value } ?: return null
        return best.key.replaceFirstChar { it.uppercase() }
    }

    /** Average pairwise cosine similarity within a cluster. */
    fun cohesionOf(cluster: List<ClusterMember>): Float {
        if (cluster.size < 2) return 0f
        var sum = 0f
        var pairs = 0
        for (i in cluster.indices) {
            for (j in i + 1 until cluster.size) {
                sum += TermProfiles.cosine(cluster[i].profile, cluster[j].profile)
                pairs++
            }
        }
        return if (pairs == 0) 0f else sum / pairs
    }

    companion object {
        /** LLD §4.3b: 3 files with cohesion >= 0.65 -> suggest to the user. */
        private const val MIN_SUGGEST_CLUSTER_SIZE = 3
        private const val SUGGEST_COHESION_THRESHOLD = 0.65f
    }
}