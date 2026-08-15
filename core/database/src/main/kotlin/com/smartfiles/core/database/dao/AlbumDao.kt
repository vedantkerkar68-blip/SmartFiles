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
