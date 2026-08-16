package com.smartfiles.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartfiles.core.database.entity.IndexedFolderEntity
import com.smartfiles.core.model.FolderPermissionState
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM indexed_folders ORDER BY grantedAt")
    fun observeAll(): Flow<List<IndexedFolderEntity>>

    @Query("SELECT * FROM indexed_folders WHERE permissionState != 'ACTIVE' ORDER BY grantedAt")
    fun observePermissionIssues(): Flow<List<IndexedFolderEntity>>

    @Upsert
    suspend fun upsert(folder: IndexedFolderEntity)

    @Query("UPDATE indexed_folders SET permissionState = :state, lastValidatedAt = :now WHERE folderUri = :uri")
    suspend fun updatePermissionState(uri: String, state: FolderPermissionState, now: Long)

    @Query("SELECT folderUri FROM indexed_folders WHERE permissionState = 'ACTIVE'")
    suspend fun activeUris(): List<String>

    @Query("DELETE FROM indexed_folders WHERE folderUri = :uri")
    suspend fun remove(uri: String)
}
