package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartfiles.core.model.DuplicateGroupStatus
import com.smartfiles.core.model.DuplicateGroupType

/** LLD §2.2 — DuplicateGroupEntity. */
@Entity(tableName = "duplicate_groups")
data class DuplicateGroupEntity(
    @PrimaryKey(autoGenerate = true) val groupId: Long = 0,
    val groupType: DuplicateGroupType,
    val representativeFileId: Long? = null,
    val status: DuplicateGroupStatus = DuplicateGroupStatus.PENDING_REVIEW,
    val createdAt: Long = System.currentTimeMillis(),
)
