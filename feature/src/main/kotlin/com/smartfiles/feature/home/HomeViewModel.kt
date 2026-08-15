package com.smartfiles.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartfiles.core.common.AppLogger
import com.smartfiles.core.common.CoroutineDispatchers
import com.smartfiles.domain.FileRepository
import com.smartfiles.domain.ScanFilesUseCase
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
    private val scanFiles: ScanFilesUseCase,
    settingsRepository: SettingsRepository,
    fileRepository: FileRepository,
    dispatchers: CoroutineDispatchers,
    private val logger: AppLogger,
) : ViewModel() {

    private val coroutineDispatchers = dispatchers

    private val _transient = MutableStateFlow(TransientState())
    private var rootUris: List<String> = emptyList()

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings,
        fileRepository.observeAllFiles(),
        _transient,
    ) { settings, files, transient ->
        HomeUiState(
            settings = settings,
            indexedFileCount = files.size,
            indexedAlbumCount = 0,
            isScanning = transient.isScanning,
            lastScanEnqueued = transient.lastScanEnqueued,
            error = transient.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private data class TransientState(
        val isScanning: Boolean = false,
        val lastScanEnqueued: Int? = null,
        val error: String? = null,
    )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.FolderGranted -> {
                rootUris = listOf(event.rootUri)
                runScan(rootUris)
            }
            is HomeEvent.Rescan -> runScan(event.rootUris)
        }
    }

    /** Currently granted root URIs (for a manual re-scan). */
    fun currentRootUris(): List<String> = rootUris

    private fun runScan(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(coroutineDispatchers.io) {
            _transient.update { it.copy(isScanning = true, error = null) }
            try {
                val enqueued = scanFiles(uris)
                _transient.update { it.copy(isScanning = false, lastScanEnqueued = enqueued) }
            } catch (e: Exception) {
                logger.w(TAG, "scan failed", e)
                _transient.update {
                    it.copy(isScanning = false, error = e.message ?: "Scan failed")
                }
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
