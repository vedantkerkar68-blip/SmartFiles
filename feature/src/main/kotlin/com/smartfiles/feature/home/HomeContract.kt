package com.smartfiles.feature.home

import com.smartfiles.domain.AppSettings

/** UI state for the Home screen. */
data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val indexedFileCount: Int = 0,
    val indexedAlbumCount: Int = 0,
    val isScanning: Boolean = false,
    /** Epoch millis when the last background scan was requested. */
    val scanRequestedAt: Long? = null,
    val error: String? = null,
)

sealed interface HomeEvent {
    /** User granted access to a folder tree; triggers a metadata scan. */
    data class FolderGranted(val rootUri: String) : HomeEvent
    /** User-initiated re-scan of the current granted tree. */
    data class Rescan(val rootUris: List<String>) : HomeEvent
}
