package com.smartfiles.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartfiles.core.database.entity.AlbumEntity
import com.smartfiles.core.database.entity.FileAlbumCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY name")
    fun observeAll(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE parentAlbumId IS :parentId ORDER BY name")
    fun observeChildren(parentId: Long?): Flow<List<AlbumEntity>>

    @Query("SELECT a.*, (SELECT COUNT(*) FROM file_album_cross_ref f WHERE f.albumId = a.albumId) AS fileCount FROM albums a ORDER BY a.name")
    fun observeAlbumsWithCounts(): Flow<List<AlbumWithCount>>

    @Query("SELECT * FROM albums WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE albumId = :id")
    suspend fun getById(id: Long): AlbumEntity?

    @Upsert
    suspend fun upsert(album: AlbumEntity): Long

    @Query("UPDATE albums SET centroidEmbedding = :centroid WHERE albumId = :albumId")
    suspend fun updateCentroid(albumId: Long, centroid: ByteArray)

    @Query("UPDATE albums SET confidence = :confidence WHERE albumId = :albumId")
    suspend fun updateConfidence(albumId: Long, confidence: Float?)

    @Query("INSERT INTO file_album_cross_ref (fileId, albumId, confidence, assignedBy, assignedAt) VALUES (:fileId, :albumId, :confidence, :assignedBy, :assignedAt) ON CONFLICT(fileId, albumId) DO UPDATE SET confidence = excluded.confidence, assignedBy = excluded.assignedBy")
    suspend fun assign(fileId: Long, albumId: Long, confidence: Float, assignedBy: String, assignedAt: Long)

    @Query("SELECT * FROM file_album_cross_ref WHERE fileId = :fileId ORDER BY confidence DESC")
    suspend fun assignmentsForFile(fileId: Long): List<FileAlbumCrossRef>

    @Query("UPDATE files SET primaryAlbumId = :albumId WHERE fileId = :fileId")
    suspend fun setPrimaryAlbum(fileId: Long, albumId: Long?)

    @Query("SELECT fileId, primaryAlbumId FROM files WHERE primaryAlbumId IS NOT NULL")
    fun observePrimaryAlbumIds(): Flow<List<PrimaryAlbumRow>>

    @Query("SELECT * FROM albums WHERE parentAlbumId IS NULL ORDER BY name")
    suspend fun topLevelAlbums(): List<AlbumEntity>

    @Query("UPDATE files SET classificationConfidence = :confidence WHERE fileId = :fileId")
    suspend fun setClassificationConfidence(fileId: Long, confidence: Float?)

    @Query(
        """
        INSERT OR IGNORE INTO album_suggestions
        (fileId, sourceAlbumId, suggestedAlbumId, confidence, reasons, status, createdAt)
        VALUES (:fileId, :sourceAlbumId, :suggestedAlbumId, :confidence, :reasons, 'PENDING', :createdAt)
        """,
    )
    suspend fun insertSuggestion(
        fileId: Long,
        sourceAlbumId: Long?,
        suggestedAlbumId: Long,
        confidence: Float,
        reasons: String,
        createdAt: Long,
    )

    @Query(
        """
        SELECT s.suggestionId, s.fileId, s.sourceAlbumId, s.suggestedAlbumId, s.confidence,
               s.reasons, s.status, f.displayName
        FROM album_suggestions s
        INNER JOIN files f ON f.fileId = s.fileId
        WHERE s.status = 'PENDING'
        ORDER BY s.createdAt DESC
        """,
    )
    fun observePendingSuggestions(): Flow<List<SuggestionWithFileRow>>

    @Query("SELECT COUNT(*) FROM album_suggestions WHERE fileId = :fileId AND status = 'PENDING'")
    suspend fun pendingSuggestionCountForFile(fileId: Long): Int

    @Query("SELECT COUNT(*) FROM album_suggestions WHERE fileId = :fileId AND suggestedAlbumId = :albumId AND status = 'REJECTED'")
    suspend fun rejectedSuggestionCount(fileId: Long, albumId: Long): Int

    @Query(
        "UPDATE album_suggestions SET status = 'ACCEPTED' WHERE fileId = :fileId AND suggestedAlbumId = :albumId AND status = 'PENDING'",
    )
    suspend fun markSuggestionAccepted(fileId: Long, albumId: Long)

    @Query(
        "UPDATE album_suggestions SET status = 'REJECTED' WHERE fileId = :fileId AND suggestedAlbumId = :albumId AND status = 'PENDING'",
    )
    suspend fun markSuggestionRejected(fileId: Long, albumId: Long)
}

data class AlbumWithCount(
    val albumId: Long,
    val name: String,
    val parentAlbumId: Long?,
    val type: String,
    val confidence: Float?,
    val createdAutomatically: Boolean,
    val centroidEmbedding: ByteArray?,
    val iconOrEmoji: String?,
    val createdAt: Long,
    val fileCount: Int,
)

data class PrimaryAlbumRow(val fileId: Long, val primaryAlbumId: Long? = null)

data class SuggestionWithFileRow(
    val suggestionId: Long,
    val fileId: Long,
    val sourceAlbumId: Long?,
    val suggestedAlbumId: Long,
    val confidence: Float,
    val reasons: String,
    val status: String,
    val displayName: String,
)
