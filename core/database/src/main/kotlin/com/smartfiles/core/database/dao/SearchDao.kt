package com.smartfiles.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.smartfiles.core.database.entity.FileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Keyword search over the FTS index, plus tag/album lookups.
 *
 * Note: FTS4 external-content tables do not expose the hidden `rank` column to
 * Room, so result ranking here falls back to rowid order. True relevance
 * ranking (statistical/bm25-style + semantic blend) is introduced in Phase 3.
 */
@Dao
interface SearchDao {
    @Query(
        """
        SELECT files.* FROM files_fts
        JOIN files ON files.fileId = files_fts.rowid
        WHERE files_fts MATCH :ftsQuery AND files.isDeletedFromSource = 0
        ORDER BY files_fts.rowid
        LIMIT :limit
        """
    )
    suspend fun keywordSearch(ftsQuery: String, limit: Int): List<FileEntity>

    @Query(
        """
        SELECT files.* FROM files_fts
        JOIN files ON files.fileId = files_fts.rowid
        WHERE files_fts MATCH :ftsQuery AND files.isDeletedFromSource = 0
        ORDER BY files_fts.rowid
        LIMIT :limit
        """
    )
    fun observeKeywordSearch(ftsQuery: String, limit: Int): Flow<List<FileEntity>>

    @Query("SELECT files.* FROM files WHERE files.displayName LIKE '%' || :term || '%' AND files.isDeletedFromSource = 0 LIMIT :limit")
    suspend fun filenameSearch(term: String, limit: Int): List<FileEntity>

    @Query("SELECT DISTINCT f.* FROM files f JOIN file_album_cross_ref cr ON cr.fileId = f.fileId WHERE cr.albumId = :albumId AND f.isDeletedFromSource = 0 ORDER BY f.displayName")
    suspend fun filesInAlbum(albumId: Long): List<FileEntity>

    @Query("SELECT f.* FROM files f JOIN file_tag_cross_ref tcr ON tcr.fileId = f.fileId JOIN tags t ON t.tagId = tcr.tagId WHERE t.name = :tagName COLLATE NOCASE AND f.isDeletedFromSource = 0 LIMIT :limit")
    suspend fun filesWithTag(tagName: String, limit: Int): List<FileEntity>
}
