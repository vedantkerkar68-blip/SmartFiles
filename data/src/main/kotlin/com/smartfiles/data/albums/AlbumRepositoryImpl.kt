package com.smartfiles.data.albums

import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.database.dao.AlbumDao
import com.smartfiles.core.database.dao.AlbumWithCount
import com.smartfiles.core.database.dao.FileDao
import com.smartfiles.core.database.entity.AlbumEntity
import com.smartfiles.core.model.AlbumItem
import com.smartfiles.core.model.AlbumNode
import com.smartfiles.core.model.AlbumSuggestion
import com.smartfiles.core.model.AlbumType
import com.smartfiles.core.model.AssignmentSource
import com.smartfiles.domain.AlbumRepository
import com.smartfiles.domain.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [AlbumRepository] (LLD §4.5, §2.3). Seeds the predefined top-level
 * taxonomy, maintains the self-referential album tree, applies file-level
 * assignments (writing both the join row and the denormalized
 * `files.primaryAlbumId`), persists per-file suggestions, and runs the
 * DynamicAlbumCreator cluster sweep. Files are never touched — this is a virtual
 * index only.
 */
@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao,
    private val fileDao: FileDao,
    private val settingsRepository: SettingsRepository,
    private val localStrategy: LocalClassificationStrategy,
    private val logger: AppLogger,
) : AlbumRepository {

    override fun observeAlbumTree(): Flow<List<AlbumNode>> =
        albumDao.observeAlbumsWithCounts().map { rows -> buildTree(rows.map { it.toItem() }) }

    override fun observeSuggestions(): Flow<List<AlbumSuggestion>> =
        albumDao.observePendingSuggestions().map { rows ->
            rows.map { row ->
                AlbumSuggestion(
                    fileId = row.fileId,
                    sourceAlbumId = row.sourceAlbumId,
                    suggestedAlbumId = row.suggestedAlbumId,
                    confidence = row.confidence,
                    reasons = row.reasons.split(REASONS_SEPARATOR).filter { it.isNotBlank() },
                    fileName = row.displayName,
                )
            }
        }

    override suspend fun ensureSeedAlbums() {
        for (category in CategoryLexicon.TOP_LEVEL) {
            if (albumDao.getByName(category.name) == null) {
                albumDao.upsert(
                    AlbumEntity(
                        name = category.name,
                        parentAlbumId = null,
                        type = AlbumType.PREDEFINED,
                        iconOrEmoji = category.emoji,
                    ),
                )
            }
        }
    }

    override suspend fun assign(fileId: Long, albumId: Long, confidence: Float, assignedBy: AssignmentSource) {
        val now = System.currentTimeMillis()
        albumDao.assign(fileId, albumId, confidence, assignedBy.name, now)
        albumDao.setPrimaryAlbum(fileId, albumId)
        albumDao.setClassificationConfidence(fileId, confidence)
    }

    override suspend fun createAlbum(name: String, parentAlbumId: Long?): AlbumItem {
        val id = albumDao.upsert(
            AlbumEntity(
                name = name,
                parentAlbumId = parentAlbumId,
                type = AlbumType.USER_CREATED,
            ),
        )
        return AlbumItem(albumId = id, name = name, parentAlbumId = parentAlbumId, type = AlbumType.USER_CREATED)
    }

    override suspend fun offerSuggestion(suggestion: AlbumSuggestion) {
        if (albumDao.pendingSuggestionCountForFile(suggestion.fileId) > 0) return
        if (albumDao.rejectedSuggestionCount(suggestion.fileId, suggestion.suggestedAlbumId) > 0) return
        albumDao.insertSuggestion(
            fileId = suggestion.fileId,
            sourceAlbumId = suggestion.sourceAlbumId,
            suggestedAlbumId = suggestion.suggestedAlbumId,
            confidence = suggestion.confidence,
            reasons = suggestion.reasons.joinToString(REASONS_SEPARATOR),
            createdAt = System.currentTimeMillis(),
        )
    }

    override suspend fun acceptSuggestion(suggestion: AlbumSuggestion) {
        assign(suggestion.fileId, suggestion.suggestedAlbumId, suggestion.confidence, AssignmentSource.SUGGESTED_ACCEPTED)
        albumDao.markSuggestionAccepted(suggestion.fileId, suggestion.suggestedAlbumId)
    }

    override suspend fun rejectSuggestion(fileId: Long, suggestedAlbumId: Long) {
        albumDao.markSuggestionRejected(fileId, suggestedAlbumId)
    }

    override suspend fun reconcileDynamicAlbums() {
        val now = System.currentTimeMillis()
        if (now - lastReconcileMs < RECONCILE_INTERVAL_MS) return
        lastReconcileMs = now

        val settings = settingsRepository.get()
        if (fileDao.uncategorizedFileCount() < settings.newAlbumMinClusterSize) return

        val candidates = fileDao.classificationCandidates(CANDIDATE_LIMIT)
        val members = candidates.mapNotNull { c ->
            c.extractedText?.takeIf { it.isNotBlank() }
                ?.let { ClusterMember(c.fileId, c.displayName, TermProfiles.of(it)) }
        }
        if (members.size < MIN_SUGGEST_CLUSTER) return

        val creator = DynamicAlbumCreator()
        for (cluster in Clusterer.greedy(members, GREEDY_MIN_SIMILARITY)) {
            if (cluster.size < MIN_SUGGEST_CLUSTER) continue
            when (creator.evaluate(cluster, settings, maxSimilarityToExisting = null)) {
                // maxSimilarityToExisting stays null until Phase 4 provides album
                // centroids, so the distinctness gate holds AUTO_CREATE back —
                // the suggest path still runs and surfaces the cluster to the user.
                NewAlbumDecision.AUTO_CREATE -> createSubAlbum(cluster, creator)
                NewAlbumDecision.SUGGEST_TO_USER -> suggestSubAlbum(cluster, creator)
                NewAlbumDecision.KEEP_UNCATEGORIZED -> Unit
            }
        }
    }

    private suspend fun createSubAlbum(cluster: List<ClusterMember>, creator: DynamicAlbumCreator) {
        val name = creator.nameFor(cluster) ?: return
        if (albumDao.getByName(name) != null) return
        val parentId = parentAlbumIdFor(cluster)
        val id = albumDao.upsert(
            AlbumEntity(
                name = name,
                parentAlbumId = parentId,
                type = AlbumType.AUTO_CREATED,
                createdAutomatically = true,
            ),
        )
        val confidence = creator.cohesionOf(cluster)
        for (member in cluster) {
            assign(member.fileId, id, confidence, AssignmentSource.AUTO)
        }
        logger.i(TAG, "auto-created album '$name' with ${cluster.size} files")
    }

    private suspend fun suggestSubAlbum(cluster: List<ClusterMember>, creator: DynamicAlbumCreator) {
        val name = creator.nameFor(cluster) ?: return
        var album = albumDao.getByName(name)
        if (album == null) {
            val parentId = parentAlbumIdFor(cluster)
            val id = albumDao.upsert(
                AlbumEntity(
                    name = name,
                    parentAlbumId = parentId,
                    type = AlbumType.AUTO_CREATED,
                    createdAutomatically = true,
                ),
            )
            album = albumDao.getById(id)
        }
        val albumId = album?.albumId ?: return
        val confidence = creator.cohesionOf(cluster).coerceIn(SUGGEST_MIN, SUGGEST_MAX)
        for (member in cluster) {
            offerSuggestion(
                AlbumSuggestion(
                    fileId = member.fileId,
                    sourceAlbumId = null,
                    suggestedAlbumId = albumId,
                    confidence = confidence,
                    reasons = listOf(
                        "Similar to ${cluster.size} file(s) in this album",
                        "Cluster cohesion ${(confidence * 100).roundToInt()}%",
                    ),
                ),
            )
        }
        logger.i(TAG, "suggested new album '$name' for ${cluster.size} files")
    }

    private suspend fun parentAlbumIdFor(cluster: List<ClusterMember>): Long? {
        val joined = cluster.joinToString("\n") { it.displayName }
        val prediction = localStrategy.classifyCategory(joined)
        val category = prediction.category ?: return null
        return albumDao.getByName(category.name)?.albumId
    }

    private fun buildTree(items: List<AlbumItem>): List<AlbumNode> {
        val byParent = items.groupBy { it.parentAlbumId }
        fun childrenOf(parentId: Long?): List<AlbumNode> =
            (byParent[parentId] ?: emptyList())
                .sortedBy { it.name }
                .map { AlbumNode(it, childrenOf(it.albumId)) }
        return childrenOf(null)
    }

    private fun AlbumWithCount.toItem() = AlbumItem(
        albumId = albumId,
        name = name,
        parentAlbumId = parentAlbumId,
        type = AlbumType.valueOf(type),
        confidence = confidence,
        createdAutomatically = createdAutomatically,
        fileCount = fileCount,
        iconOrEmoji = iconOrEmoji,
    )

    companion object {
        private const val TAG = "AlbumRepository"
        private const val REASONS_SEPARATOR = "\n"
        private const val CANDIDATE_LIMIT = 400
        private const val MIN_SUGGEST_CLUSTER = 3
        private const val GREEDY_MIN_SIMILARITY = 0.5f
        private const val SUGGEST_MIN = 0.60f
        private const val SUGGEST_MAX = 0.84f
        private const val RECONCILE_INTERVAL_MS = 10 * 60_000L
        @Volatile private var lastReconcileMs = 0L
    }
}