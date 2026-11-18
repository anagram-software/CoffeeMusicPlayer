package com.udeshcoffee.android.ui.compose

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.udeshcoffee.android.ui.compose.equalizer.EqualizerScreen
import com.udeshcoffee.android.ui.compose.library.LibraryScreen
import com.udeshcoffee.android.ui.compose.navigation.AlbumRoute
import com.udeshcoffee.android.ui.compose.navigation.ArtistRoute
import com.udeshcoffee.android.ui.compose.navigation.EditorRoute
import com.udeshcoffee.android.ui.compose.navigation.EqualizerRoute
import com.udeshcoffee.android.ui.compose.navigation.FavoritesRoute
import com.udeshcoffee.android.ui.compose.navigation.GenreRoute
import com.udeshcoffee.android.ui.compose.navigation.LibraryRoute
import com.udeshcoffee.android.ui.compose.navigation.PlaylistAddSongsRoute
import com.udeshcoffee.android.ui.compose.navigation.PlaylistRoute
import com.udeshcoffee.android.ui.compose.navigation.PlaylistsRoute
import com.udeshcoffee.android.ui.compose.navigation.SearchRoute
import com.udeshcoffee.android.ui.compose.navigation.SettingsRoute
import com.udeshcoffee.android.ui.compose.player.MiniPlayer
import com.udeshcoffee.android.ui.compose.player.PlayerScreen
import com.udeshcoffee.android.ui.compose.playlists.PlaylistsScreen
import com.udeshcoffee.android.ui.compose.search.SearchScreen
import com.udeshcoffee.android.ui.compose.settings.SettingsScreen
import com.udeshcoffee.android.ui.compose.theme.CoffeeTheme
import com.udeshcoffee.android.ui.compose.theme.rememberArtworkSeed
import org.koin.compose.koinInject

private data class TopLevel(val label: String, val icon: ImageVector, val route: Any)

private val topLevels = listOf(
        TopLevel("Library", Icons.Default.LibraryMusic, LibraryRoute),
        TopLevel("Favorites", Icons.Default.Favorite, FavoritesRoute),
        TopLevel("Playlists", Icons.AutoMirrored.Filled.QueueMusic, PlaylistsRoute),
        TopLevel("Equalizer", Icons.Default.Equalizer, EqualizerRoute),
        TopLevel("Settings", Icons.Default.Settings, SettingsRoute))

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
        var playerExpanded by rememberSaveable { mutableStateOf(playerExpandedRequest) }
        val dataRepository: DataRepository = koinInject()
        val mediaRepository: MediaRepository = koinInject()
        val actions = remember(navController) {
            AppActions(context, navController, scope, dataRepository, mediaRepository, snackbar, onDeleteSongs) { playerExpanded = it }
        }
        val backStack by navController.currentBackStackEntryAsState()
        val destination = backStack?.destination
        // Detail screens keep the section they were opened from highlighted.
        var currentTop by rememberSaveable { mutableIntStateOf(0) }
        topLevels.indexOfFirst { top -> destination?.hierarchy?.any { it.hasRoute(top.route::class) } == true }
                .takeIf { it >= 0 }?.let { currentTop = it }

        CompositionLocalProvider(LocalAppActions provides actions) {
            Box(Modifier.fillMaxSize()) {
                NavigationSuiteScaffold(navigationSuiteItems = {
                    topLevels.forEachIndexed { index, top ->
                        item(
                                selected = index == currentTop,
                                onClick = {
                                    navController.navigate(top.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(top.icon, contentDescription = null) },
                                label = { Text(top.label) })
                    }
                }) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            NavHost(navController, startDestination = LibraryRoute) {
                                composable<LibraryRoute> { LibraryScreen() }
                                composable<FavoritesRoute> { FavoritesScreen() }
                                composable<PlaylistsRoute> { PlaylistsScreen() }
                                composable<EqualizerRoute> { EqualizerScreen() }
                                composable<SettingsRoute> { SettingsScreen() }
                                composable<SearchRoute> { SearchScreen() }
                                composable<AlbumRoute> { val r = it.toRoute<AlbumRoute>(); AlbumDetailScreen(r.albumId, r.title) }
                                composable<ArtistRoute> { val r = it.toRoute<ArtistRoute>(); ArtistDetailScreen(r.artistId, r.name) }
                                composable<GenreRoute> { val r = it.toRoute<GenreRoute>(); GenreDetailScreen(r.genreId, r.name) }
                                composable<PlaylistRoute> { val r = it.toRoute<PlaylistRoute>(); PlaylistDetailScreen(r.playlistId, r.type, r.title) }
                                composable<PlaylistAddSongsRoute> { val r = it.toRoute<PlaylistAddSongsRoute>(); PlaylistAddSongsScreen(r.playlistId, r.title) }
                                composable<EditorRoute> { EditorScreen(it.toRoute<EditorRoute>().songId) }
                            }
                            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(8.dp))
                        }
                        MiniPlayer(onExpand = { playerExpanded = true })
                    }
                }
                AnimatedVisibility(playerExpanded && playerState.song != null,
                        enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                    Surface(color = MaterialTheme.colorScheme.surface) { PlayerScreen(onCollapse = { playerExpanded = false }) }
                }
                OptionsSheetHost(actions)
                AppDialogHost(actions)
            }
        }
    }
}
