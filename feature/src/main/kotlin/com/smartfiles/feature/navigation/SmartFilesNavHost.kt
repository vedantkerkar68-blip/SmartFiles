package com.smartfiles.feature.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartfiles.feature.home.HomeScreen

/** App-level destinations. */
object Destinations {
    const val HOME = "home"
    const val ALBUMS = "albums"
    const val SEARCH = "search"
}

@Composable
fun SmartFilesNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) { HomeScreen() }
        composable(Destinations.ALBUMS) { PlaceholderScreen("Albums (Phase 2)") }
        composable(Destinations.SEARCH) { PlaceholderScreen("Search (Phase 3)") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("This screen ships in a later phase.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
