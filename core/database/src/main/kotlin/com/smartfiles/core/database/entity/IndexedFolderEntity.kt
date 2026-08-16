package com.smartfiles.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartfiles.core.model.FolderPermissionState

/** A user-granted SAF tree the app is allowed to index (LLD 4.11). */
@Entity(tableName = "indexed_folders")
data class IndexedFolderEntity(
    @PrimaryKey val folderUri: String,
    val grantedAt: Long,
    val lastValidatedAt: Long,
    val permissionState: FolderPermissionState = FolderPermissionState.ACTIVE,
)
