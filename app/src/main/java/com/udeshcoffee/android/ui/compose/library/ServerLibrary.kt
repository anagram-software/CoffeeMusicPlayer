package com.udeshcoffee.android.ui.compose.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.subsonic.ServerAlbumSort
import com.udeshcoffee.android.data.subsonic.SubsonicException
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.AlbumCard
import com.udeshcoffee.android.ui.compose.components.ArtistCard
import com.udeshcoffee.android.ui.compose.components.EmptyState
import com.udeshcoffee.android.ui.compose.components.GridPadding
import com.udeshcoffee.android.ui.compose.components.ShuffleRow
import com.udeshcoffee.android.ui.compose.components.SongRow
import com.udeshcoffee.android.ui.compose.rememberCurrentSongId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** A list loaded from the server a page at a time. */
data class PagedState<T>(
        val items: List<T> = emptyList(),
        val loading: Boolean = false,
        val endReached: Boolean = false,
        val error: String? = null
) {
    val isInitialLoad get() = items.isEmpty() && (loading || (!endReached && error == null))
}

/** Loads pages on demand ([loadMore]) and keeps them in [state]. */
class ServerPager<T>(private val scope: CoroutineScope, private val pageSize: Int, private val load: suspend (offset: Int) -> List<T>) {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()
    private var job: Job? = null

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.endReached) return
        _state.value = s.copy(loading = true, error = null)
        job = scope.launch {
            try {
                val page = load(s.items.size)
                _state.update { it.copy(items = it.items + page, loading = false, endReached = page.size < pageSize) }
            } catch (e: SubsonicException) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun reset() {
        job?.cancel()
        _state.value = PagedState()
        loadMore()
    }
}

/** A list loaded from the server in one go. */
data class LoadState<T>(val data: T? = null, val error: String? = null)

class ServerLibraryViewModel(private val subsonic: SubsonicRepository) : ViewModel() {

    val songs = ServerPager(viewModelScope, SubsonicRepository.PAGE_SIZE) { subsonic.songs(it) }

    private val _albumSort = MutableStateFlow(ServerAlbumSort.NEWEST)
    val albumSort: StateFlow<ServerAlbumSort> = _albumSort.asStateFlow()
    val albums = ServerPager(viewModelScope, SubsonicRepository.PAGE_SIZE) { subsonic.albums(_albumSort.value, it) }

    private val _artists = MutableStateFlow(LoadState<List<Artist>>())
    val artists: StateFlow<LoadState<List<Artist>>> = _artists.asStateFlow()

    private val _genres = MutableStateFlow(LoadState<List<Genre>>())
    val genres: StateFlow<LoadState<List<Genre>>> = _genres.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        songs.reset()
        albums.reset()
        loadArtists()
        loadGenres()
    }

    fun setAlbumSort(sort: ServerAlbumSort) {
        if (sort == _albumSort.value) return
        _albumSort.value = sort
        albums.reset()
    }

    fun loadArtists() = viewModelScope.launch {
        _artists.value = LoadState()
        _artists.value = try { LoadState(subsonic.artists()) } catch (e: SubsonicException) { LoadState(error = e.message) }
    }

    fun loadGenres() = viewModelScope.launch {
        _genres.value = LoadState()
        _genres.value = try { LoadState(subsonic.genres()) } catch (e: SubsonicException) { LoadState(error = e.message) }
    }

    /** A random selection from the whole server library (shuffling only the loaded page would be lopsided). */
    suspend fun randomSongs(): List<Song> = try { subsonic.randomSongs() } catch (e: SubsonicException) { emptyList() }
}

/** Server library tabs (no Folders: Subsonic browses by tags). */
private val serverTabs = listOf("Songs", "Albums", "Artists", "Genres")

/** The Library tab when the music source is the server. [startTab] is a tab name from the device list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerLibraryScreen(startTab: String, viewModel: ServerLibraryViewModel = koinViewModel()) {
    val pager = rememberPagerState(initialPage = serverTabs.indexOf(startTab).coerceAtLeast(0)) { serverTabs.size }
    val scope = rememberCoroutineScope()
    val actions = LocalAppActions.current
    Scaffold(topBar = {
        TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = actions::openSearch) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    if (pager.currentPage == 1) ServerAlbumSortMenu(viewModel)
                })
    }) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 8.dp) {
                serverTabs.forEachIndexed { index, title ->
                    Tab(selected = pager.currentPage == index, onClick = { scope.launch { pager.animateScrollToPage(index) } },
                            text = { Text(title) })
                }
            }
            HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                when (page) {
                    0 -> ServerSongsTab(viewModel)
                    1 -> ServerAlbumsTab(viewModel)
                    2 -> ServerArtistsTab(viewModel)
                    else -> ServerGenresTab(viewModel)
                }
            }
        }
    }
}

/** Album order for the server's album list (shown in the Library top bar). */
@Composable
private fun ServerAlbumSortMenu(viewModel: ServerLibraryViewModel) {
    val sort by viewModel.albumSort.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort albums") }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text("Albums") }, onClick = {}, enabled = false)
            ServerAlbumSort.entries.forEach { option ->
                DropdownMenuItem(text = { Text(option.label) }, onClick = { expanded = false; viewModel.setAlbumSort(option) },
                        trailingIcon = { if (option == sort) Icon(Icons.Default.Check, contentDescription = "Selected") })
            }
        }
    }
}

@Composable
fun ServerError(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyState(message ?: "Couldn't load from the server", Modifier.weight(1f, fill = false), Icons.Default.CloudOff)
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

/** Asks for the next page when the list scrolls near its end. */
@Composable
private fun LoadMoreEffect(state: LazyListState, pager: ServerPager<*>) {
    LaunchedEffect(state, pager) {
        snapshotFlow { state.layoutInfo.let { it.totalItemsCount > 0 && (it.visibleItemsInfo.lastOrNull()?.index ?: 0) >= it.totalItemsCount - 20 } }
                .distinctUntilChanged().filter { it }.collect { pager.loadMore() }
    }
}

@Composable
private fun LoadMoreEffect(state: LazyGridState, pager: ServerPager<*>) {
    LaunchedEffect(state, pager) {
        snapshotFlow { state.layoutInfo.let { it.totalItemsCount > 0 && (it.visibleItemsInfo.lastOrNull()?.index ?: 0) >= it.totalItemsCount - 20 } }
                .distinctUntilChanged().filter { it }.collect { pager.loadMore() }
    }
}

@Composable
private fun ServerSongsTab(viewModel: ServerLibraryViewModel) {
    val state by viewModel.songs.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val currentId = rememberCurrentSongId()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LoadMoreEffect(listState, viewModel.songs)
    when {
        state.isInitialLoad -> Loading()
        state.items.isEmpty() && state.error != null -> ServerError(state.error, viewModel.songs::reset)
        state.items.isEmpty() -> EmptyState("No songs on the server")
        else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 8.dp)) {
            item { ShuffleRow(0, subtitle = "A random mix from the server") { scope.launch { actions.play(viewModel.randomSongs()) } } }
            items(state.items, key = { it.id }) { song ->
                SongRow(song, onClick = { actions.play(state.items, state.items.indexOf(song)) }, onMore = { actions.songOptions(song) },
                        isCurrent = song.id == currentId)
            }
            pagerFooter(state, viewModel.songs::loadMore)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.pagerFooter(state: PagedState<*>, retry: () -> Unit) {
    if (state.loading) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    state.error?.let { error ->
        item {
            ListItem(headlineContent = { Text(error) }, trailingContent = { androidx.compose.material3.TextButton(onClick = retry) { Text("Retry") } })
        }
    }
}

@Composable
private fun ServerAlbumsTab(viewModel: ServerLibraryViewModel) {
    val state by viewModel.albums.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val gridState = rememberLazyGridState()
    LoadMoreEffect(gridState, viewModel.albums)
    when {
        state.isInitialLoad -> Loading()
        state.items.isEmpty() && state.error != null -> ServerError(state.error, viewModel.albums::reset)
        state.items.isEmpty() -> EmptyState("No albums on the server")
        else -> LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), state = gridState, contentPadding = GridPadding,
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.items, key = { it.id }) { album -> ServerAlbumCard(album) }
            if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            state.error?.let { error ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ListItem(headlineContent = { Text(error) },
                            trailingContent = { androidx.compose.material3.TextButton(onClick = viewModel.albums::loadMore) { Text("Retry") } })
                }
            }
        }
    }
}

@Composable
private fun ServerAlbumCard(album: Album) {
    val actions = LocalAppActions.current
    AlbumCard(album, onClick = { actions.openAlbum(album) }, onLongClick = { actions.albumOptions(album) }, onPlay = { actions.playAlbum(album) })
}

@Composable
private fun ServerArtistsTab(viewModel: ServerLibraryViewModel) {
    val state by viewModel.artists.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val artists = state.data
    when {
        state.error != null -> ServerError(state.error, { viewModel.loadArtists() })
        artists == null -> Loading()
        artists.isEmpty() -> EmptyState("No artists on the server")
        else -> LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), contentPadding = GridPadding,
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(artists, key = { it.id }) { artist ->
                ArtistCard(artist, onClick = { actions.openArtist(artist) }, onLongClick = { actions.artistOptions(artist) },
                        onPlay = { actions.playArtist(artist) })
            }
        }
    }
}

@Composable
private fun ServerGenresTab(viewModel: ServerLibraryViewModel) {
    val state by viewModel.genres.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val genres = state.data
    when {
        state.error != null -> ServerError(state.error, { viewModel.loadGenres() })
        genres == null -> Loading()
        genres.isEmpty() -> EmptyState("No genres on the server")
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(genres, key = { it.id }) { genre ->
                ListItem(
                        modifier = Modifier.clickable { actions.openGenre(genre) },
                        headlineContent = { Text(genre.name) },
                        supportingContent = { Text("${genre.no ?: 0} songs", color = MaterialTheme.colorScheme.onSurfaceVariant) })
            }
        }
    }
}
