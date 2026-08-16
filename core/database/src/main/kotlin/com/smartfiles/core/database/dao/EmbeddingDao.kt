package com.smartfiles.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartfiles.core.database.entity.EmbeddingEntity

@Dao
interface EmbeddingDao {
    @Upsert
    suspend fun upsert(embedding: EmbeddingEntity)

    @Query("SELECT fileId, vector, dim FROM embeddings WHERE modelVersion = :modelVersion ORDER BY fileId LIMIT :limit OFFSET :offset")
    suspend fun getVectorPage(modelVersion: String, limit: Int, offset: Int): List<EmbeddingRow>

    @Query("SELECT COUNT(*) FROM embeddings WHERE modelVersion = :modelVersion")
    suspend fun count(modelVersion: String): Long

    @Query("SELECT count(*) FROM embeddings")
    fun observeTotalCount(): kotlinx.coroutines.flow.Flow<Long>

    @Query("SELECT fileId, vector, dim FROM embeddings WHERE fileId = :fileId AND modelVersion = :modelVersion LIMIT 1")
    suspend fun getForFile(fileId: Long, modelVersion: String): EmbeddingRow?

    @Query("SELECT e.fileId, e.vector, e.dim FROM embeddings e INNER JOIN file_album_cross_ref x ON x.fileId = e.fileId WHERE x.albumId = :albumId AND e.modelVersion = :modelVersion")
    suspend fun embeddingsForAlbum(albumId: Long, modelVersion: String): List<EmbeddingRow>

    @Query("DELETE FROM embeddings WHERE modelVersion = :modelVersion")
    suspend fun deleteAllForModel(modelVersion: String)
}

data class EmbeddingRow(val fileId: Long, val vector: ByteArray, val dim: Int)
