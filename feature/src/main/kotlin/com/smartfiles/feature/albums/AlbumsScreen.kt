package com.smartfiles.feature.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
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
import com.smartfiles.core.model.AlbumNode
import com.smartfiles.core.model.AlbumSuggestion
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(viewModel: AlbumsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Albums") }) }) { innerPadding ->
        if (state.isLoading && state.tree.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val albumNameById = state.tree.flatMap { node ->
            listOf(node.album.albumId to node.album.name) + node.children.map { it.album.albumId to it.album.name }
        }.toMap()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Organized albums", style = MaterialTheme.typography.titleMedium)
            }
            items(state.tree, key = { it.album.albumId }) { node ->
                AlbumRow(node = node, depth = 0)
            }

            if (state.suggestions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Suggestions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Files the classifier is fairly but not fully sure about — accept to organize them.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                items(state.suggestions, key = { "${it.fileId}:${it.suggestedAlbumId}" }) { suggestion ->
                    SuggestionCard(suggestion, albumNameById[suggestion.suggestedAlbumId], viewModel)
                }
            }

            if (state.suggestions.isEmpty() && state.tree.isNotEmpty()) {
                item {
                    Text(
                        "No pending suggestions. New files are auto-organized when confidence is high enough.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(node: AlbumNode, depth: Int) {
    val album = node.album
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = (depth * 20).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(album.iconOrEmoji ?: "📁")
        Text(
            album.name,
            style = if (depth == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.weight(1f))
        Text("${album.fileCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    node.children.forEach { AlbumRow(it, depth + 1) }
}

@Composable
private fun SuggestionCard(suggestion: AlbumSuggestion, albumName: String?, viewModel: AlbumsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(suggestion.fileName ?: "File #${suggestion.fileId}", style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("→ ${albumName ?: "Album"}") })
                Text(
                    "${(suggestion.confidence * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            suggestion.reasons.take(3).forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.onEvent(AlbumsEvent.Accept(suggestion)) }) { Text("Accept") }
                OutlinedButton(
                    onClick = { viewModel.onEvent(AlbumsEvent.Reject(suggestion.fileId, suggestion.suggestedAlbumId)) },
                ) { Text("Not this album") }
            }
        }
    }
}