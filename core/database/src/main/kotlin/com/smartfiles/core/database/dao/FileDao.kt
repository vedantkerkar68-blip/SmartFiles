package com.smartfiles.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartfiles.core.database.entity.FileEntity
import com.smartfiles.core.model.ChangeSignature
import com.smartfiles.core.model.DocType
import com.smartfiles.core.model.ProcessingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE fileId = :fileId")
    fun observeFile(fileId: Long): Flow<FileEntity?>

    @Query("SELECT * FROM files ORDER BY dateFirstIndexed DESC LIMIT 500")
    fun observeRecent(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isDeletedFromSource = 0 ORDER BY displayName")
    fun observeAll(): Flow<List<FileEntity>>

    @Query("SELECT COUNT(*) FROM files WHERE isDeletedFromSource = 0")
    fun observeCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM files WHERE processingStatus = 'INDEXED' AND isDeletedFromSource = 0")
    fun observeIndexedCount(): Flow<Long>

    @Query("SELECT sizeBytes, dateModifiedSource, sha256Hash FROM files WHERE uri = :uri")
    suspend fun getChangeSignature(uri: String): ChangeSignature?

    @Query("SELECT fileId FROM files WHERE uri = :uri")
    suspend fun getFileIdByUri(uri: String): Long?

    @Upsert
    suspend fun upsert(file: FileEntity): Long

    @Query("UPDATE files SET processingStatus = :status, processingLevel = :level, lastError = NULL WHERE fileId = :fileId")
    suspend fun updateStatus(fileId: Long, status: ProcessingStatus, level: Int)

    @Query("UPDATE files SET extractedText = :text, ocrApplied = :ocrApplied, ocrConfidenceAvg = :ocrConfidence, widthPx = :width, heightPx = :height, perceptualHash = :perceptualHash, processingStatus = 'CONTENT_DONE', processingLevel = 2 WHERE fileId = :fileId")
    suspend fun updateContent(
        fileId: Long,
        text: String?,
        ocrApplied: Boolean,
        ocrConfidence: Float?,
        width: Int?,
        height: Int?,
        perceptualHash: Long?,
    )

    @Query("UPDATE files SET sha256Hash = :hash WHERE fileId = :fileId")
    suspend fun updateHash(fileId: Long, hash: String?)

    @Query("UPDATE files SET processingStatus = 'FAILED', lastError = :error WHERE fileId = :fileId")
    suspend fun markFailed(fileId: Long, error: String)

    @Query("UPDATE files SET isDeletedFromSource = 1 WHERE uri NOT IN (:currentUris)")
    suspend fun markMissingAsDeleted(currentUris: List<String>)

    @Query("UPDATE files SET isDeletedFromSource = 0 WHERE uri IN (:currentUris)")
    suspend fun unmarkPresent(currentUris: List<String>)

    @Query("SELECT * FROM files WHERE processingStatus != 'INDEXED' AND isDeletedFromSource = 0 ORDER BY dateFirstIndexed ASC LIMIT :limit")
    suspend fun getUnprocessedBatch(limit: Int): List<FileEntity>

    @Query("SELECT fileId FROM files WHERE docType = :docType AND sha256Hash IS NOT NULL")
    suspend fun getFileIdsWithHash(docType: DocType?): List<Long>

    @Query("SELECT fileId, perceptualHash FROM files WHERE perceptualHash IS NOT NULL")
    suspend fun getPerceptualHashes(): List<PerceptualHashRow>

    @Query("SELECT * FROM files WHERE uri = :uri")
    suspend fun getByUri(uri: String): FileEntity?

    @Query("SELECT f.* FROM files f INNER JOIN file_album_cross_ref x ON x.fileId = f.fileId WHERE x.albumId = :albumId AND f.isDeletedFromSource = 0 ORDER BY f.displayName")
    fun observeFilesByAlbum(albumId: Long): Flow<List<FileEntity>>

    @Query("SELECT fileId FROM files WHERE uri = :uri AND fileId != :excludeId")
    suspend fun findSameUri(uri: String, excludeId: Long): Long?
}

data class PerceptualHashRow(val fileId: Long, val perceptualHash: Long)
