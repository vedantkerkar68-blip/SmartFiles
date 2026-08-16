package com.smartfiles.data.files

import androidx.room.withTransaction
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.common.FileChangeDetector
import com.smartfiles.core.database.AppDatabase
import com.smartfiles.core.database.dao.FileDao
import com.smartfiles.core.database.entity.FileEntity
import com.smartfiles.core.model.DiscoveredFile
import com.smartfiles.core.model.FileItem
import com.smartfiles.domain.FileRepository
import com.smartfiles.domain.ExtractionResult
import com.smartfiles.domain.ExtractionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room + SAF-backed [FileRepository]. The DB is the virtual index only; files
 * are never moved or rewritten. */
class FileRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val fileDao: FileDao,
    private val logger: AppLogger,
) : FileRepository {

    override suspend fun syncScan(files: List<DiscoveredFile>): List<Long> =
        db.withTransaction {
            val changed = mutableListOf<Long>()
            val now = System.currentTimeMillis()
            for (f in files) {
                val cached = fileDao.getChangeSignature(f.uri)
                val existing = fileDao.getByUri(f.uri)
                val entity = if (existing != null) {
                    // Preserve processing columns (status/level/text/album/hash)
                    // while refreshing only the change-detection metadata.
                    existing.copy(
                        displayName = f.displayName,
                        mimeType = f.mimeType,
                        sizeBytes = f.sizeBytes,
                        dateModifiedSource = f.dateModifiedSource,
                    )
                } else {
                    FileEntity(
                        uri = f.uri,
                        displayName = f.displayName,
                        mimeType = f.mimeType,
                        sizeBytes = f.sizeBytes,
                        dateModifiedSource = f.dateModifiedSource,
                        dateFirstIndexed = now,
                        docType = f.docType,
                    )
                }
                val id = fileDao.upsert(entity)
                if (FileChangeDetector.shouldReprocess(cached, f)) {
                    changed += id
                }
            }
            changed
        }

    override suspend fun upsertDiscoveredFiles(files: List<DiscoveredFile>) {
        syncScan(files)
    }

    override fun observeFile(fileId: Long): Flow<FileItem?> =
        fileDao.observeFile(fileId).map { it?.toItem() }

    override fun observeAllFiles(): Flow<List<FileItem>> =
        fileDao.observeAll().map { list -> list.map { it.toItem() } }

    override fun observeFilesByAlbum(albumId: Long): Flow<List<FileItem>> =
        fileDao.observeFilesByAlbum(albumId).map { list -> list.map { it.toItem() } }

    override suspend fun markDeletedIfMissing(existingUris: Set<String>) {
        if (existingUris.isEmpty()) return
        fileDao.markMissingAsDeleted(existingUris.toList())
    }

    override suspend fun countIndexedFiles(): Long =
        fileDao.observeCount().first()

    override suspend fun updateProcessingResult(fileId: Long, result: ExtractionResult) {
        fileDao.updateContent(
            fileId = fileId,
            text = result.text,
            ocrApplied = result.source == ExtractionSource.OCR,
            ocrConfidence = result.ocrConfidenceAvg,
            width = result.widthPx,
            height = result.heightPx,
            perceptualHash = result.perceptualHash,
        )
    }

    private fun FileEntity.toItem() = FileItem(
        fileId = fileId,
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        dateModifiedSource = dateModifiedSource,
        docType = docType,
        processingStatus = processingStatus,
        processingLevel = processingLevel,
        primaryAlbumId = primaryAlbumId,
        classificationConfidence = classificationConfidence,
        extractedTextPreview = extractedText?.take(512),
    )
}
