package com.smartfiles.data.folders

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.database.dao.FolderDao
import com.smartfiles.core.database.entity.IndexedFolderEntity
import com.smartfiles.core.model.FolderPermissionState
import com.smartfiles.domain.FileSource
import com.smartfiles.domain.FolderRepository
import com.smartfiles.domain.IndexedFolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Room + SAF-backed [FolderRepository] (LLD §4.11). */
@Singleton
class FolderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderDao: FolderDao,
    private val fileSource: FileSource,
    private val logger: AppLogger,
) : FolderRepository {

    override fun observeFolders(): Flow<List<IndexedFolder>> =
        folderDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observePermissionIssues(): Flow<List<IndexedFolder>> =
        folderDao.observePermissionIssues().map { list -> list.map { it.toModel() } }

    override suspend fun addFolder(uri: String) {
        try {
            context.contentResolver.takePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // Some providers do not grant persistable permission (e.g. single
            // document grants). The folder is still usable for this session;
            // PermissionValidationWorker will flag it if the grant lapses.
            logger.w(TAG, "persistable permission unavailable for $uri", e)
        }
        folderDao.upsert(
            IndexedFolderEntity(
                folderUri = uri,
                grantedAt = System.currentTimeMillis(),
                lastValidatedAt = System.currentTimeMillis(),
                permissionState = FolderPermissionState.ACTIVE,
            ),
        )
    }

    override suspend fun activeUris(): List<String> = folderDao.activeUris()

    override suspend fun refreshPermissionStates() {
        val now = System.currentTimeMillis()
        for (folder in folderDao.observeAll().first()) {
            val state = if (fileSource.isTreeGranted(folder.folderUri)) {
                FolderPermissionState.ACTIVE
            } else {
                FolderPermissionState.NEEDS_REGRANT
            }
            folderDao.updatePermissionState(folder.folderUri, state, now)
        }
    }

    override suspend fun removeFolder(uri: String) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: Exception) {
            logger.w(TAG, "releasePersistableUriPermission failed for $uri", e)
        }
        folderDao.remove(uri)
    }

    private fun IndexedFolderEntity.toModel() = IndexedFolder(
        uri = folderUri,
        grantedAt = grantedAt,
        permissionState = permissionState,
    )

    companion object {
        private const val TAG = "FolderRepositoryImpl"
    }
}
