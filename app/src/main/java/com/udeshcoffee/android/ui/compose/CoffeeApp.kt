package com.udeshcoffee.android.ui.compose

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.components.AppDialogHost
import com.udeshcoffee.android.ui.compose.components.OptionsSheetHost
import com.udeshcoffee.android.ui.compose.detail.AlbumDetailScreen
import com.udeshcoffee.android.ui.compose.detail.ArtistDetailScreen
import com.udeshcoffee.android.ui.compose.detail.FavoritesScreen
import com.udeshcoffee.android.ui.compose.detail.GenreDetailScreen
import com.udeshcoffee.android.ui.compose.detail.PlaylistAddSongsScreen
import com.udeshcoffee.android.ui.compose.detail.PlaylistDetailScreen
import com.udeshcoffee.android.ui.compose.editor.EditorScreen
import com.udeshcoffee.android.ui.compose.home.HomeScreen
import com.udeshcoffee.android.ui.compose.library.LibraryScreen
import com.udeshcoffee.android.ui.compose.navigation.AlbumRoute
import com.udeshcoffee.android.ui.compose.navigation.ArtistRoute
import com.udeshcoffee.android.ui.compose.navigation.EditorRoute
import com.udeshcoffee.android.ui.compose.navigation.FavoritesRoute
import com.udeshcoffee.android.ui.compose.navigation.GenreRoute
import com.udeshcoffee.android.ui.compose.navigation.HomeRoute
import com.udeshcoffee.android.ui.compose.navigation.LibraryRoute
import com.udeshcoffee.android.ui.compose.navigation.PlaylistAddSongsRoute
import com.udeshcoffee.android.ui.compose.navigation.PlaylistRoute
import com.udeshcoffee.android.ui.compose.navigation.SearchRoute
import com.udeshcoffee.android.ui.compose.navigation.SettingsRoute
import com.udeshcoffee.android.ui.compose.player.MiniPlayer
import com.udeshcoffee.android.ui.compose.player.PlayerScreen
import com.udeshcoffee.android.ui.compose.player.PlayerSheetHost
import com.udeshcoffee.android.ui.compose.player.playerSheetDraggable
import com.udeshcoffee.android.ui.compose.player.rememberPlayerSheetFling
import com.udeshcoffee.android.ui.compose.player.rememberPlayerSheetState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import com.udeshcoffee.android.ui.compose.search.SearchScreen
import com.udeshcoffee.android.ui.compose.settings.SettingsScreen
import com.udeshcoffee.android.ui.compose.theme.CoffeeTheme
import com.udeshcoffee.android.ui.compose.theme.rememberArtworkSeed
import org.koin.compose.koinInject

private data class TopLevel(val label: String, val icon: ImageVector, val route: Any)

private val topLevels = listOf(
        TopLevel("Home", Icons.Default.Home, HomeRoute),
        TopLevel("Favorites", Icons.Default.Favorite, FavoritesRoute),
        TopLevel("Library", Icons.Default.LibraryMusic, LibraryRoute),
        TopLevel("Search", Icons.Default.Search, SearchRoute))

/** Root of the Compose UI. [onDeleteSongs] asks the user to confirm deleting files (activity result). */
@Composable
fun CoffeeApp(onDeleteSongs: (List<Song>) -> Unit, playerExpandedRequest: Boolean = false) {
    val playerState by rememberPlayerState()
    val seed = rememberArtworkSeed(playerState.song)

    CoffeeTheme(seedColor = seed) {
        val navController = rememberNavController()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val playerSheet = rememberPlayerSheetState(initiallyExpanded = playerExpandedRequest)
        val dataRepository: DataRepository = koinInject()
        val mediaRepository: MediaRepository = koinInject()
        val subsonic: com.udeshcoffee.android.data.subsonic.SubsonicRepository = koinInject()
        val sourceStore: com.udeshcoffee.android.data.source.MusicSourceStore = koinInject()
        val actions = remember(navController) {
            AppActions(context, sourceStore.isServer, navController, scope, dataRepository, mediaRepository, subsonic, snackbar, onDeleteSongs) { expand ->
                scope.launch { if (expand) playerSheet.expand() else playerSheet.collapse() }
            }
        }
        // Until a music source is set up, open Settings (sign-in form) over Home and keep the music screens empty.
        val configured = remember { sourceStore.isConfigured }
        var setupOpened by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!configured && !setupOpened) {
                setupOpened = true
                actions.openSettings()
            }
        }
        val navigateTop: (Any) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        val needsSetup: @Composable (@Composable () -> Unit) -> Unit = { screen ->
            if (configured) screen() else SetupNeeded(onOpenSettings = actions::openSettings)
        }
        val backStack by navController.currentBackStackEntryAsState()
        val destination = backStack?.destination
        // Detail screens keep the section they were opened from highlighted.
        var currentTop by rememberSaveable { mutableIntStateOf(0) }
        topLevels.indexOfFirst { top -> destination?.hierarchy?.any { it.hasRoute(top.route::class) } == true }
                .takeIf { it >= 0 }?.let { currentTop = it }

        // Nothing to show: put the player away without animating.
        LaunchedEffect(playerState.song == null) { if (playerState.song == null && playerState.connected) playerSheet.snapCollapsed() }
        val sheetFling = rememberPlayerSheetFling(playerSheet)

        CompositionLocalProvider(LocalAppActions provides actions) {
            Box(Modifier.fillMaxSize().onSizeChanged { playerSheet.updateHeight(it.height.toFloat()) }) {
                NavigationSuiteScaffold(navigationSuiteItems = {
                    topLevels.forEachIndexed { index, top ->
                        item(
                                selected = index == currentTop,
                                onClick = { navigateTop(top.route) },
                                icon = { Icon(top.icon, contentDescription = null) },
                                label = { Text(top.label) })
                    }
                }) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            NavHost(navController, startDestination = HomeRoute) {
                                composable<HomeRoute> { needsSetup { HomeScreen() } }
                                composable<FavoritesRoute> { needsSetup { FavoritesScreen() } }
                                composable<LibraryRoute> { needsSetup { LibraryScreen() } }
                                composable<SearchRoute> { needsSetup { SearchScreen() } }
                                composable<SettingsRoute> { SettingsScreen() }
                                composable<AlbumRoute> { val r = it.toRoute<AlbumRoute>(); AlbumDetailScreen(r.albumId, r.title, r.remoteId) }
                                composable<ArtistRoute> { val r = it.toRoute<ArtistRoute>(); ArtistDetailScreen(r.artistId, r.name, r.remoteId) }
                                composable<GenreRoute> { val r = it.toRoute<GenreRoute>(); GenreDetailScreen(r.genreId, r.name, r.remote) }
                                composable<PlaylistRoute> { val r = it.toRoute<PlaylistRoute>(); PlaylistDetailScreen(r.playlistId, r.type, r.title, r.remoteId, r.readonly) }
                                composable<PlaylistAddSongsRoute> { val r = it.toRoute<PlaylistAddSongsRoute>(); PlaylistAddSongsScreen(r.playlistId, r.title) }
                                composable<EditorRoute> { EditorScreen(it.toRoute<EditorRoute>().songId) }
                            }
                            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(8.dp))
                        }
                        // Drag up to pull the full player out of it; fades as the player covers it.
                        MiniPlayer(onExpand = { scope.launch { playerSheet.expand() } },
                                modifier = Modifier
                                        .graphicsLayer { alpha = 1f - playerSheet.progress }
                                        .playerSheetDraggable(playerSheet, sheetFling))
                    }
                }
                if (playerState.song != null) {
                    PlayerSheetHost(playerSheet) { PlayerScreen(onCollapse = { scope.launch { playerSheet.collapse() } }) }
                }
                OptionsSheetHost(actions)
                AppDialogHost(actions)
            }
        }
    }
}

/** Shown on the music screens until a music source is set up in Settings. */
@Composable
private fun SetupNeeded(onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Choose where your music comes from", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text("Connect to your music server, or use the music on this device.", textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onOpenSettings) { Text("Open settings") }
    }
}
