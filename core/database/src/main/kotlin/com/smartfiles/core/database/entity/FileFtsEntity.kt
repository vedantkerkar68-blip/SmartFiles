package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * SQLite FTS4 virtual table (LLD §2.5). Uses an external-content table tied to
 * [FileEntity] so indexed text is not duplicated on disk and stays in sync via
 * the triggers Room generates. All three columns exist on FileEntity so the
 * external-content mapping is valid.
 *
 * Column set is chosen to let a single MATCH search filename + content + tags.
 */
@Fts4(contentEntity = FileEntity::class)
@Entity(tableName = "files_fts")
data class FileFtsEntity(
    val displayName: String,
    val extractedText: String? = null,
    val tagsConcat: String? = null,
)
