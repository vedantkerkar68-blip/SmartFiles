package com.smartfiles.feature.albums

import com.smartfiles.core.model.AlbumNode
import com.smartfiles.core.model.AlbumSuggestion

data class AlbumsUiState(
    val tree: List<AlbumNode> = emptyList(),
    val suggestions: List<AlbumSuggestion> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface AlbumsEvent {
    data class Accept(val suggestion: AlbumSuggestion) : AlbumsEvent
    data class Reject(val fileId: Long, val suggestedAlbumId: Long) : AlbumsEvent
    data class CreateAlbum(val name: String, val parentAlbumId: Long?) : AlbumsEvent
}