package com.smartfiles.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfiles.core.common.AppLogger
import com.smartfiles.domain.AlbumRepository
import com.smartfiles.domain.BackgroundWorkScheduler
import com.smartfiles.domain.FileRepository
import com.smartfiles.domain.FolderRepository
import com.smartfiles.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val workScheduler: BackgroundWorkScheduler,
    settingsRepository: SettingsRepository,
    fileRepository: FileRepository,
    albumRepository: AlbumRepository,
    private val logger: AppLogger,
) : ViewModel() {

    private val _transient = MutableStateFlow(TransientState())
    private var lastGrantedUri: String? = null

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings,
        fileRepository.observeAllFiles(),
        albumRepository.observeAlbumTree(),
        _transient,
    ) { settings, files, tree, transient ->
        HomeUiState(
            settings = settings,
            indexedFileCount = files.size,
            indexedAlbumCount = tree.size,
            isScanning = transient.isScanning,
            scanRequestedAt = transient.scanRequestedAt,
            error = transient.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private data class TransientState(
        val isScanning: Boolean = false,
        val scanRequestedAt: Long? = null,
        val error: String? = null,
    )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.FolderGranted -> {
                lastGrantedUri = event.rootUri
                persistAndScan(event.rootUri)
            }
            is HomeEvent.Rescan -> requestScan()
        }
    }

    /** Most recently granted root URI (for a manual re-scan). */
    fun currentRootUris(): List<String> = listOfNotNull(lastGrantedUri)

    private fun persistAndScan(uri: String) {
        viewModelScope.launch {
            _transient.update { it.copy(isScanning = true, error = null) }
            try {
                // Persist the grant first so the background scan (which reads
                // active folders from the repository) includes this tree.
                folderRepository.addFolder(uri)
                requestScan()
            } catch (e: Exception) {
                logger.w(TAG, "failed to persist folder grant", e)
                _transient.update {
                    it.copy(isScanning = false, error = e.message ?: "Could not save folder")
                }
            }
        }
    }

    private fun requestScan() {
        _transient.update { it.copy(isScanning = true, error = null) }
        try {
            workScheduler.scheduleImmediateMetadataScan()
            _transient.update { it.copy(isScanning = false, scanRequestedAt = System.currentTimeMillis()) }
        } catch (e: Exception) {
            logger.w(TAG, "failed to schedule scan", e)
            _transient.update {
                it.copy(isScanning = false, error = e.message ?: "Could not schedule scan")
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
