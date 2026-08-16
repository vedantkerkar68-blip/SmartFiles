package com.smartfiles.domain

import com.smartfiles.core.model.FolderPermissionState
import kotlinx.coroutines.flow.Flow

/** A granted, indexable folder tree (LLD §4.11). */
data class IndexedFolder(
    val uri: String,
    val grantedAt: Long,
    val permissionState: FolderPermissionState,
)

/**
 * Persists SAF folder grants and tracks their permission lifecycle. A revoked
 * permission flags the folder as NEEDS_REGRANT but never deletes indexed files.
 */
interface FolderRepository {
    fun observeFolders(): Flow<List<IndexedFolder>>
    fun observePermissionIssues(): Flow<List<IndexedFolder>>
    /** Persists the grant (takePersistableUriPermission) and records the folder. */
    suspend fun addFolder(uri: String)
    suspend fun activeUris(): List<String>
    /** Re-checks every granted tree; marks lapsed grants NEEDS_REGRANT. */
    suspend fun refreshPermissionStates()
    suspend fun removeFolder(uri: String)
}
