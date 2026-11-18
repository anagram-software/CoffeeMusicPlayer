package com.udeshcoffee.android.ui.compose.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.udeshcoffee.android.ui.compose.components.FolderSortOptions
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
import org.koin.compose.viewmodel.koinViewModel

private val tabs = listOf("Songs", "Albums", "Artists", "Folders", "Genres")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel = koinViewModel(), foldersViewModel: FoldersViewModel = koinViewModel()) {
    val context = LocalContext.current
    val startTab = remember {
        PreferenceManager.getDefaultSharedPreferences(context).getString(PreferenceUtil.PREF_LIB_START, "0")?.toIntOrNull() ?: 0
    }
    val pager = rememberPagerState(initialPage = startTab.coerceIn(0, tabs.lastIndex)) { tabs.size }
    val scope = rememberCoroutineScope()
    val actions = LocalAppActions.current

    Scaffold(topBar = {
        TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = actions::openSearch) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    LibrarySortMenu(pager.currentPage)
                })
    }) { padding ->
        Box(Modifier.padding(padding)) {
            androidx.compose.foundation.layout.Column {
                PrimaryScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 8.dp) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = pager.currentPage == index, onClick = { scope.launch { pager.animateScrollToPage(index) } },
                                text = { Text(title) })
                    }
                }
                HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    when (page) {
                        0 -> SongsTab(viewModel)
                        1 -> AlbumsTab(viewModel)
                        2 -> ArtistsTab(viewModel)
                        3 -> FoldersTab(foldersViewModel, isCurrentPage = pager.currentPage == 3)
                        else -> GenresTab(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySortMenu(page: Int) {
    when (page) {
        0 -> { val s by SortSettings.songs.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.songs.group(null, SongSortOptions, s)) }
        1 -> { val s by SortSettings.albums.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.albums.group(null, AlbumSortOptions, s)) }
        2 -> { val s by SortSettings.artists.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.artists.group(null, ArtistSortOptions, s)) }
        3 -> {
            val f by SortSettings.folders.state.collectAsStateWithLifecycle()
            val s by SortSettings.folderSongs.state.collectAsStateWithLifecycle()
            SortMenu(SortSettings.folders.group("Folders", FolderSortOptions, f), SortSettings.folderSongs.group("Songs", SongSortOptions, s))
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoldersTab(viewModel: FoldersViewModel, isCurrentPage: Boolean) {
    val dir by viewModel.currentDir.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val currentId = rememberCurrentSongId()
    val scope = rememberCoroutineScope()
    BackHandler(enabled = isCurrentPage && dir != null) { viewModel.up() }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ListItem(
                    leadingContent = {
                        if (dir != null) IconButton(onClick = { viewModel.up() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up") }
                    },
                    headlineContent = { Text(viewModel.pathLabel(dir), style = MaterialTheme.typography.titleSmall) },
                    trailingContent = {
                        if (songs.isNotEmpty()) IconButton(onClick = { actions.play(songs) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play folder")
                        }
                    })
        }
        when {
            folders == null -> item { Loading() }
            else -> items(folders!!, key = { "folder:" + it.file.path }) { folder ->
                ListItem(
                        modifier = Modifier.combinedClickable(onClick = { viewModel.open(folder) },
                                onLongClick = { scope.launch { actions.collectionOptions(folder.title, viewModel.folderSongs(folder)) } }),
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                        headlineContent = { Text(folder.title) },
                        supportingContent = { Text(songCount(folder.songCount)) },
                        trailingContent = {
                            IconButton(onClick = { scope.launch { actions.collectionOptions(folder.title, viewModel.folderSongs(folder)) } }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Folder options")
                            }
                        })
            }
        }
        items(songs, key = { "song:" + it.id }) { song ->
            SongRow(song, onClick = { actions.play(songs, songs.indexOf(song)) }, onMore = { actions.songOptions(song) },
                    isCurrent = song.id == currentId)
        }
        if (folders?.isEmpty() == true && songs.isEmpty()) item { EmptyState("No music here") }
    }
}
