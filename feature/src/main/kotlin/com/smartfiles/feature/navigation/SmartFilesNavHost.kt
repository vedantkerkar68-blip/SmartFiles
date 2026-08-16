package com.smartfiles.feature.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartfiles.feature.albums.AlbumsScreen
import com.smartfiles.feature.home.HomeScreen

/** App-level destinations. */
object Destinations {
    const val HOME = "home"
    const val ALBUMS = "albums"
    const val SEARCH = "search"
}

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

@Composable
fun SmartFilesNavHost() {
    val navController = rememberNavController()
    val destinations = listOf(
        BottomDestination(Destinations.HOME, "Home", Icons.Filled.Home),
        BottomDestination(Destinations.ALBUMS, "Albums", Icons.Filled.Collections),
        BottomDestination(Destinations.SEARCH, "Search", Icons.Filled.Search),
    )

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                destinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destinations.HOME) { HomeScreen() }
            composable(Destinations.ALBUMS) { AlbumsScreen() }
            composable(Destinations.SEARCH) { PlaceholderScreen("Search (Phase 3)") }
        }
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