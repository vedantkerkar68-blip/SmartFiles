package com.smartfiles.domain

import com.smartfiles.core.model.AlbumItem
import com.smartfiles.core.model.AlbumNode
import com.smartfiles.core.model.AlbumSuggestion
import com.smartfiles.core.model.AssignmentSource
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun observeAlbumTree(): Flow<List<AlbumNode>>
    fun observeSuggestions(): Flow<List<AlbumSuggestion>>
    suspend fun ensureSeedAlbums()
    suspend fun assign(fileId: Long, albumId: Long, confidence: Float, assignedBy: AssignmentSource)
    suspend fun createAlbum(name: String, parentAlbumId: Long?): AlbumItem
    suspend fun acceptSuggestion(suggestion: AlbumSuggestion)
    suspend fun rejectSuggestion(fileId: Long, suggestedAlbumId: Long)
    suspend fun offerSuggestion(suggestion: AlbumSuggestion)
    /** Cluster sweep: auto-create/suggest new sub-albums from emerging clusters (LLD §4.3b). */
    suspend fun reconcileDynamicAlbums()
}