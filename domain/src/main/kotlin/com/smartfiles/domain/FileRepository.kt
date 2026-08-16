package com.smartfiles.domain

import com.smartfiles.core.model.DiscoveredFile
import com.smartfiles.core.model.FileItem
import kotlinx.coroutines.flow.Flow

/** Repository contract for metadata storage + change detection. */
interface FileRepository {
    /**
     * Transactionally syncs discovered files against cached signatures using
     * the injected [changeDetector], upserting metadata and returning the ids
     * of files that are new or changed and therefore need deep processing.
     */
    suspend fun syncScan(files: List<DiscoveredFile>): List<Long>
    suspend fun upsertDiscoveredFiles(files: List<DiscoveredFile>)
    fun observeFile(fileId: Long): Flow<FileItem?>
    fun observeAllFiles(): Flow<List<FileItem>>
    fun observeFilesByAlbum(albumId: Long): Flow<List<FileItem>>
    suspend fun markDeletedIfMissing(existingUris: Set<String>)
    suspend fun countIndexedFiles(): Long
    /** Persists the result of a Level-2 content extraction pass. */
    suspend fun updateProcessingResult(fileId: Long, result: ExtractionResult)
    /** Classification source text (name + extracted content) for a file. */
    suspend fun classificationSource(fileId: Long): ClassificationSource?
    /** Advances a file to Level-3 (CLASSIFIED) once classification succeeds. */
    suspend fun markClassified(fileId: Long)
}

/** Input to the classification engine (LLD §4.4 RepresentativeTextBuilder input). */
data class ClassificationSource(
    val displayName: String,
    val extractedText: String?,
)
