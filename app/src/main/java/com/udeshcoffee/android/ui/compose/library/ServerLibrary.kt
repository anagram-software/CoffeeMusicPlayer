package com.udeshcoffee.android.ui.compose.library

import com.udeshcoffee.android.ui.compose.components.PageLoadingIndicator
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.udeshcoffee.android.ui.compose.components.PageBottomPadding
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
import com.udeshcoffee.android.ui.compose.playlists.PlaylistsTab
import com.udeshcoffee.android.ui.compose.components.SortSettings
import com.udeshcoffee.android.ui.compose.components.SortGroup
import com.udeshcoffee.android.ui.compose.components.SortMenu
import com.udeshcoffee.android.ui.compose.components.SortOption
import com.udeshcoffee.android.ui.compose.components.sortedArtists

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

    /** Loads the first page, unless already loading or loaded. */
    fun start() {
        if (_state.value == PagedState<T>()) loadMore()
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

    // Nothing loads up front: each Library category page asks for its own list (start / ensure...).
    private var artistsJob: Job? = null
    private var genresJob: Job? = null

    fun ensureArtists() {
        if (_artists.value.data == null && _artists.value.error == null && artistsJob?.isActive != true) artistsJob = loadArtists()
    }

    fun ensureGenres() {
        if (_genres.value.data == null && _genres.value.error == null && genresJob?.isActive != true) genresJob = loadGenres()
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

/** A Library category page when the music source is the server: only its list loads. */
@Composable
fun ServerLibraryCategory(category: LibraryCategory, viewModel: ServerLibraryViewModel = koinViewModel()) {
    LibraryCategoryScaffold(category, actions = {
        when (category) {
            LibraryCategory.ALBUMS -> ServerAlbumSortMenu(viewModel)
            LibraryCategory.ARTISTS -> ServerArtistSortMenu()
            LibraryCategory.PLAYLISTS -> PlaylistSortMenu()
            else -> {}
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            when (category) {
                LibraryCategory.PLAYLISTS -> PlaylistsTab()
                LibraryCategory.ALBUMS -> ServerAlbumsTab(viewModel)
                LibraryCategory.ARTISTS -> ServerArtistsTab(viewModel)
                LibraryCategory.SONGS -> ServerSongsTab(viewModel)
                LibraryCategory.GENRES -> ServerGenresTab(viewModel)
            }
        }
    }
}

/** Album order for the server's album list: the server sorts it, so there's no direction to choose. */
@Composable
private fun ServerAlbumSortMenu(viewModel: ServerLibraryViewModel) {
    val sort by viewModel.albumSort.collectAsStateWithLifecycle()
    SortMenu(SortGroup(ServerAlbumSort.entries.map { SortOption(it.ordinal, it.label) }, sort.ordinal, ascending = null,
            onSelect = { viewModel.setAlbumSort(ServerAlbumSort.entries[it]) }))
}

@Composable
fun ServerError(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyState(message ?: "Couldn't load from the server", Modifier.weight(1f, fill = false), Icons.Default.CloudOff)
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PageLoadingIndicator() }

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
    LaunchedEffect(Unit) { viewModel.songs.start() }
    LoadMoreEffect(listState, viewModel.songs)
    when {
        state.isInitialLoad -> Loading()
        state.items.isEmpty() && state.error != null -> ServerError(state.error, viewModel.songs::reset)
        state.items.isEmpty() -> EmptyState("No songs on the server")
        else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = PageBottomPadding)) {
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
    if (state.loading) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { PageLoadingIndicator() } }
    state.error?.let { error ->
        item {
            ListItem(
                    trailingContent = { androidx.compose.material3.TextButton(onClick = retry) { Text("Retry") } }) { Text(error) }
        }
    }
}

@Composable
private fun ServerAlbumsTab(viewModel: ServerLibraryViewModel) {
    val state by viewModel.albums.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val gridState = rememberLazyGridState()
    LaunchedEffect(Unit) { viewModel.albums.start() }
    LoadMoreEffect(gridState, viewModel.albums)
    when {
        state.isInitialLoad -> Loading()
        state.items.isEmpty() && state.error != null -> ServerError(state.error, viewModel.albums::reset)
        state.items.isEmpty() -> EmptyState("No albums on the server")
        else -> LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), state = gridState, contentPadding = GridPadding,
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.items, key = { it.id }) { album -> ServerAlbumCard(album) }
            if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { PageLoadingIndicator() }
            }
            state.error?.let { error ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ListItem(
                            trailingContent = { androidx.compose.material3.TextButton(onClick = viewModel.albums::loadMore) { Text("Retry") } }) { Text(error) }
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
    val sort by SortSettings.artists.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val artists = remember(state.data, sort) { state.data?.sortedArtists(sort) }
    LaunchedEffect(Unit) { viewModel.ensureArtists() }
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
    LaunchedEffect(Unit) { viewModel.ensureGenres() }
    when {
        state.error != null -> ServerError(state.error, { viewModel.loadGenres() })
        genres == null -> Loading()
        genres.isEmpty() -> EmptyState("No genres on the server")
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = PageBottomPadding)) {
            items(genres, key = { it.id }) { genre ->
                ListItem(
                        onClick = { actions.openGenre(genre) },
                        supportingContent = { Text("${genre.no ?: 0} songs", color = MaterialTheme.colorScheme.onSurfaceVariant) }) { Text(genre.name) }
            }
        }
    }
}
