package com.smartfiles.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { viewModel.onEvent(HomeEvent.FolderGranted(it.toString())) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SmartFiles") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Indexed files", style = MaterialTheme.typography.labelMedium)
                    Text("${state.indexedFileCount}", style = MaterialTheme.typography.headlineMedium)
                    Text("Auto-organize threshold: ${state.settings.autoClassifyThreshold}", style = MaterialTheme.typography.bodyMedium)
                    Text("Organized albums: ${state.indexedAlbumCount}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Pick a folder")
            }

            if (state.indexedFileCount > 0) {
                OutlinedButton(
                    onClick = {
                        viewModel.onEvent(HomeEvent.Rescan(viewModel.currentRootUris()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Re-scan")
                }
            }

            if (state.isScanning) {
                CircularProgressIndicator()
            }
            state.scanRequestedAt?.let {
                Text("Scan scheduled in the background", style = MaterialTheme.typography.bodyMedium)
            }
            state.error?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
