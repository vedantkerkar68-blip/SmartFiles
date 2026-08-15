package com.smartfiles.domain

import com.smartfiles.core.model.AlbumItem
import com.smartfiles.core.model.AlbumNode
import com.smartfiles.core.model.AlbumSuggestion
import com.smartfiles.core.model.AssignmentSource
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    fun observeAlbumTree(): Flow<List<AlbumNode>>
    suspend fun ensureSeedAlbums()
    suspend fun assign(fileId: Long, albumId: Long, confidence: Float, assignedBy: AssignmentSource)
    suspend fun createAlbum(name: String, parentAlbumId: Long?): AlbumItem
    suspend fun pendingSuggestions(): List<AlbumSuggestion>
    suspend fun acceptSuggestion(suggestion: AlbumSuggestion)
    suspend fun rejectSuggestion(suggestionId: String)
}
