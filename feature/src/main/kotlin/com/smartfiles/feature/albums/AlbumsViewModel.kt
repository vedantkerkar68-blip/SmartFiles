package com.smartfiles.feature.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfiles.core.common.AppLogger
import com.smartfiles.domain.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val logger: AppLogger,
) : ViewModel() {

    val uiState: StateFlow<AlbumsUiState> = combine(
        albumRepository.observeAlbumTree(),
        albumRepository.observeSuggestions(),
    ) { tree, suggestions ->
        AlbumsUiState(tree = tree, suggestions = suggestions, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumsUiState())

    init {
        viewModelScope.launch {
            try {
                // Idempotent: seeds the predefined taxonomy on first run, then
                // opportunistically reconciles dynamic sub-album clusters.
                albumRepository.ensureSeedAlbums()
                albumRepository.reconcileDynamicAlbums()
            } catch (e: Exception) {
                logger.w(TAG, "initial album setup failed", e)
            }
        }
    }

    fun onEvent(event: AlbumsEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is AlbumsEvent.Accept -> albumRepository.acceptSuggestion(event.suggestion)
                    is AlbumsEvent.Reject -> albumRepository.rejectSuggestion(event.fileId, event.suggestedAlbumId)
                    is AlbumsEvent.CreateAlbum -> albumRepository.createAlbum(event.name, event.parentAlbumId)
                }
            } catch (e: Exception) {
                logger.w(TAG, "album action failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "AlbumsViewModel"
    }
}