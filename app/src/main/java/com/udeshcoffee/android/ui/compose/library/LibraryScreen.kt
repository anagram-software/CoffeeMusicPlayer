package com.udeshcoffee.android.ui.compose.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.AlbumArtwork
import com.udeshcoffee.android.ui.compose.components.AlbumCard
import com.udeshcoffee.android.ui.compose.components.AlbumSortOptions
import com.udeshcoffee.android.ui.compose.components.ArtistCard
import com.udeshcoffee.android.ui.compose.components.ArtistSortOptions
import com.udeshcoffee.android.ui.compose.components.EmptyState
import com.udeshcoffee.android.ui.compose.components.GridPadding
import com.udeshcoffee.android.ui.compose.components.ShuffleRow
import com.udeshcoffee.android.ui.compose.components.SongRow
import com.udeshcoffee.android.ui.compose.components.SongSortOptions
import com.udeshcoffee.android.ui.compose.components.SortMenu
import com.udeshcoffee.android.ui.compose.components.SortSettings
import com.udeshcoffee.android.ui.compose.components.songCount
import com.udeshcoffee.android.ui.compose.rememberCurrentSongId
import com.udeshcoffee.android.utils.PreferenceUtil
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.ui.compose.components.SectionPill
import com.udeshcoffee.android.ui.compose.components.SettingsButton
import com.udeshcoffee.android.ui.compose.playlists.PlaylistsTab

/** Library tabs, the same for both music sources; [PreferenceUtil.PREF_LIB_START] stores an index into this list. */
val libraryTabs = listOf("Playlists", "Albums", "Artists", "Songs", "Genres")

private const val TAB_PLAYLISTS = 0
private const val TAB_ALBUMS = 1
private const val TAB_ARTISTS = 2
private const val TAB_SONGS = 3

/** The Library tab: this device's music or the server's, following the music source setting. */
@Composable
fun LibraryScreen() {
    val sourceStore: MusicSourceStore = koinInject()
    val context = LocalContext.current
    val startTab = remember {
        val index = PreferenceManager.getDefaultSharedPreferences(context).getString(PreferenceUtil.PREF_LIB_START, "0")?.toIntOrNull() ?: 0
        index.coerceIn(0, libraryTabs.lastIndex)
    }
    if (sourceStore.isServer) ServerLibraryScreen(startTab) else DeviceLibraryScreen(startTab)
}

/** Library scaffold shared by both sources: title, sort menu, settings, the tab row and the pages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScaffold(startTab: Int, sortMenu: @Composable (page: Int) -> Unit, page: @Composable (page: Int) -> Unit) {
    val pager = rememberPagerState(initialPage = startTab) { libraryTabs.size }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = {
        TopAppBar(
                title = { SectionPill("Library") },
                actions = {
                    sortMenu(pager.currentPage)
                    SettingsButton()
                })
    }) { padding ->
        androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
            PrimaryScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 8.dp) {
                libraryTabs.forEachIndexed { index, title ->
                    Tab(selected = pager.currentPage == index, onClick = { scope.launch { pager.animateScrollToPage(index) } },
                            text = { Text(title) })
                }
            }
            HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page(it) }
        }
    }
}

@Composable
private fun DeviceLibraryScreen(startTab: Int, viewModel: LibraryViewModel = koinViewModel()) {
    LibraryScaffold(startTab, sortMenu = { LibrarySortMenu(it) }) { page ->
        when (page) {
            TAB_PLAYLISTS -> PlaylistsTab()
            TAB_ALBUMS -> AlbumsTab(viewModel)
            TAB_ARTISTS -> ArtistsTab(viewModel)
            TAB_SONGS -> SongsTab(viewModel)
            else -> GenresTab(viewModel)
        }
    }
}

@Composable
private fun LibrarySortMenu(page: Int) {
    when (page) {
        TAB_ALBUMS -> { val s by SortSettings.albums.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.albums.group(null, AlbumSortOptions, s)) }
        TAB_ARTISTS -> { val s by SortSettings.artists.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.artists.group(null, ArtistSortOptions, s)) }
        TAB_SONGS -> { val s by SortSettings.songs.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.songs.group(null, SongSortOptions, s)) }
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
fun SongList(songs: List<Song>, modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null, showShuffle: Boolean = true) {
    val actions = LocalAppActions.current
    val currentId = rememberCurrentSongId()
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
        header?.let { item { it() } }
        if (showShuffle && songs.size > 1) item { ShuffleRow(songs.size) { actions.shuffle(songs) } }
        items(songs, key = { it.id }) { song ->
            SongRow(song, onClick = { actions.play(songs, songs.indexOf(song)) }, onMore = { actions.songOptions(song) },
                    isCurrent = song.id == currentId)
        }
    }
}

@Composable
private fun SongsTab(viewModel: LibraryViewModel) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    when {
        songs == null -> Loading()
        songs!!.isEmpty() -> EmptyState("No songs found")
        else -> SongList(songs!!)
    }
}

@Composable
private fun AlbumsTab(viewModel: LibraryViewModel) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    when {
        albums == null -> Loading()
        albums!!.isEmpty() -> EmptyState("No albums found")
        else -> LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), contentPadding = GridPadding,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            items(albums!!, key = { it.id }) { album ->
                AlbumCard(album, onClick = { actions.openAlbum(album) }, onLongClick = { actions.albumOptions(album) },
                        onPlay = { actions.playAlbum(album) })
            }
        }
    }
}

@Composable
private fun ArtistsTab(viewModel: LibraryViewModel) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    when {
        artists == null -> Loading()
        artists!!.isEmpty() -> EmptyState("No artists found")
        else -> LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), contentPadding = GridPadding,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            items(artists!!, key = { it.id }) { artist ->
                ArtistCard(artist, onClick = { actions.openArtist(artist) }, onLongClick = { actions.artistOptions(artist) },
                        onPlay = { actions.playArtist(artist) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GenresTab(viewModel: LibraryViewModel) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    when {
        genres == null -> Loading()
        genres!!.isEmpty() -> EmptyState("No genres found")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(genres!!, key = { it.id }) { genre -> GenreRow(genre, { actions.openGenre(genre) }, { actions.genreOptions(genre) }) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GenreRow(genre: Genre, onClick: () -> Unit, onLongClick: () -> Unit) {
    ListItem(
            modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
            headlineContent = { Text(genre.name) },
            supportingContent = { Text(songCount(genre.no ?: 0)) },
            trailingContent = {
                // Up to three album covers from the genre.
                Row {
                    genre.arts.orEmpty().take(3).forEach { albumId -> AlbumArtwork(albumId, Modifier.padding(start = 4.dp).size(40.dp)) }
                }
            })
}
