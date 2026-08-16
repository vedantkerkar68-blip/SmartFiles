package com.smartfiles.core.database.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Tags backing the LLD §2.2 ER diagram. Tag rows are deduplicated by name; the
 * many-to-many links live in `file_tag_cross_ref`. `tagsConcat` on `files` is a
 * denormalized, space-joined tag string refreshed here so FTS can search tags
 * together with content (LLD §2.5).
 */
@Dao
interface TagDao {

    @Query("INSERT OR IGNORE INTO tags (name, category) VALUES (:name, :category)")
    suspend fun insertIfAbsent(name: String, category: String?)

    @Query("SELECT tagId FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun idByName(name: String): Long?

    @Query("INSERT OR IGNORE INTO file_tag_cross_ref (fileId, tagId, confidence) VALUES (:fileId, :tagId, :confidence)")
    suspend fun link(fileId: Long, tagId: Long, confidence: Float)

    @Query("DELETE FROM file_tag_cross_ref WHERE fileId = :fileId")
    suspend fun clearLinks(fileId: Long)

    @Query("SELECT t.name FROM tags t INNER JOIN file_tag_cross_ref x ON x.tagId = t.tagId WHERE x.fileId = :fileId ORDER BY x.confidence DESC")
    suspend fun tagNamesForFile(fileId: Long): List<String>

    @Query(
        """
        UPDATE files SET tagsConcat = (
            SELECT GROUP_CONCAT(t.name, ' ') FROM tags t
            INNER JOIN file_tag_cross_ref x ON x.tagId = t.tagId
            WHERE x.fileId = :fileId
        ) WHERE fileId = :fileId
        """,
    )
    suspend fun refreshTagsConcat(fileId: Long)
}