package com.udeshcoffee.android.ui.compose.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.media.songsToAlbums
import com.udeshcoffee.android.data.subsonic.ServerAlbumSort
import com.udeshcoffee.android.data.subsonic.SubsonicException
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.ui.compose.components.MediaCard
import com.udeshcoffee.android.utils.artworkData
import com.udeshcoffee.android.ui.compose.components.TopLevelScaffold
import com.udeshcoffee.android.ui.compose.detail.ServerNotice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.AlbumArtwork
import com.udeshcoffee.android.ui.compose.components.AlbumCard
import com.udeshcoffee.android.ui.compose.components.AlbumSortOptions
import com.udeshcoffee.android.ui.compose.components.ArtistCard
import com.udeshcoffee.android.ui.compose.components.DeviceArtistSortOptions
import com.udeshcoffee.android.ui.compose.components.ServerArtistSortOptions
import com.udeshcoffee.android.ui.compose.components.artistNaturalAscending
import com.udeshcoffee.android.ui.compose.components.playlistNaturalAscending
import com.udeshcoffee.android.ui.compose.components.EmptyState
import com.udeshcoffee.android.ui.compose.components.GridPadding
import com.udeshcoffee.android.ui.compose.components.PageBottomPadding
import com.udeshcoffee.android.ui.compose.components.ShuffleRow
import com.udeshcoffee.android.ui.compose.components.SongRow
import com.udeshcoffee.android.ui.compose.components.SongSortOptions
import com.udeshcoffee.android.ui.compose.components.SortMenu
import com.udeshcoffee.android.ui.compose.components.PlaylistSortOptions
import com.udeshcoffee.android.ui.compose.components.SortSettings
import com.udeshcoffee.android.ui.compose.components.songCount
import com.udeshcoffee.android.ui.compose.rememberCurrentSongId
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.ui.compose.playlists.PlaylistsTab

/** The Library's categories: listed on the Library page, each opening its own page ([LibraryCategoryRoute]). */
enum class LibraryCategory(val label: String, val icon: ImageVector) {
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic),
    ALBUMS("Albums", Icons.Default.Album),
    ARTISTS("Artists", Icons.Default.Person),
    SONGS("Songs", Icons.Default.MusicNote),
    GENRES("Genres", Icons.Default.Category),
}

private const val RECENT_COUNT = 12

/**
 * The Library page for the current music source: the categories as a grouped list, then recently
 * played albums. Nothing else loads until a category is opened (on a server, each list is network calls).
 */
class LibraryOverviewViewModel(sourceStore: MusicSourceStore, mediaRepository: MediaRepository, subsonic: SubsonicRepository) : ViewModel() {
    val isServer = sourceStore.isServer
    private val reload = MutableStateFlow(0)
    fun retry() { reload.value++ }

    /** Device: albums of the recently played songs (the app's play stats). Server: getAlbumList2 recent. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val recentlyPlayed: StateFlow<LoadState<List<Album>>> = (
            if (isServer) reload.flatMapLatest {
                flow {
                    emit(LoadState())
                    emit(try {
                        LoadState(subsonic.albums(ServerAlbumSort.RECENT, offset = 0, count = RECENT_COUNT))
                    } catch (e: SubsonicException) {
                        LoadState(error = e.message ?: "Couldn't reach the server")
                    })
                }
            } else mediaRepository.observePlaylistSongs(0, Playlist.RECENTLY_PLAYED).map { LoadState(songsToAlbums(it).take(RECENT_COUNT)) }
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState())
}

/** The Library tab: categories of this device's music or the server's, then recently played albums. */
@Composable
fun LibraryScreen(viewModel: LibraryOverviewViewModel = koinViewModel()) {
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    TopLevelScaffold("Library") { padding ->
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "categories", span = { GridItemSpan(maxLineSpan) }) { CategoryList { actions.openLibraryCategory(it) } }
            item(key = "recent-header", span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recently played", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    // On the device the row comes from the smart playlist, which "See all" opens.
                    if (!viewModel.isServer && !recent.data.isNullOrEmpty()) {
                        TextButton(onClick = { actions.openPlaylist(Playlist(Playlist.RECENTLY_PLAYED)) }) { Text("See all") }
                    }
                }
            }
            val albums = recent.data
            when {
                recent.error != null -> item(key = "recent-error", span = { GridItemSpan(maxLineSpan) }) {
                    ServerNotice("Couldn't load: ${recent.error}", viewModel::retry)
                }
                albums == null -> item(key = "recent-loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                albums.isEmpty() -> item(key = "recent-empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Albums you play show up here", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> items(albums, key = { it.id }) { album ->
                    // Just the artist: a song count would only count the played songs.
                    MediaCard(album.title, album.artist,
                            onClick = { actions.openAlbum(album) }, onLongClick = { actions.albumOptions(album) }, onPlay = { actions.playAlbum(album) },
                            artwork = { AlbumArtwork(album, it, MaterialTheme.shapes.extraSmall) }, tintKey = album.artworkData())
                }
            }
        }
    }
}

/**
 * The categories as a segmented list (Material 3 Expressive style): one rounded group of tonal
 * rows with small gaps, the outer corners rounder than the inner ones.
 */
@Composable
private fun CategoryList(onOpen: (LibraryCategory) -> Unit) {
    val categories = LibraryCategory.entries
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        categories.forEachIndexed { index, category ->
            val outer = 20.dp
            val inner = 4.dp
            val shape = RoundedCornerShape(
                    topStart = if (index == 0) outer else inner, topEnd = if (index == 0) outer else inner,
                    bottomEnd = if (index == categories.lastIndex) outer else inner, bottomStart = if (index == categories.lastIndex) outer else inner)
            ListItem(
                    modifier = Modifier.clip(shape).clickable { onOpen(category) },
                    // A hint of the section's accent over the tonal surface.
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            .compositeOver(MaterialTheme.colorScheme.surfaceContainer)),
                    leadingContent = {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center) {
                            Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    headlineContent = { Text(category.label, style = MaterialTheme.typography.titleMedium) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) })
        }
    }
}

/** Top bar for a category page: back, the LIBRARY pill and the category's name. */
@Composable
internal fun LibraryCategoryScaffold(category: LibraryCategory, actions: @Composable () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    val appActions = LocalAppActions.current
    TopLevelScaffold("Library", pageTitle = category.label, actions = actions, onBack = { appActions.back() }, showSettings = false, content = content)
}

/** A Library category's page (albums, songs...): only this list loads. */
@Composable
fun LibraryCategoryScreen(category: LibraryCategory) {
    if (koinInject<MusicSourceStore>().isServer) ServerLibraryCategory(category) else DeviceLibraryCategory(category)
}

@Composable
private fun DeviceLibraryCategory(category: LibraryCategory, viewModel: LibraryViewModel = koinViewModel()) {
    LibraryCategoryScaffold(category, actions = { LibrarySortMenu(category) }) { padding ->
        Box(Modifier.padding(padding)) {
            when (category) {
                LibraryCategory.PLAYLISTS -> PlaylistsTab()
                LibraryCategory.ALBUMS -> AlbumsTab(viewModel)
                LibraryCategory.ARTISTS -> ArtistsTab(viewModel)
                LibraryCategory.SONGS -> SongsTab(viewModel)
                LibraryCategory.GENRES -> GenresTab(viewModel)
            }
        }
    }
}

@Composable
private fun LibrarySortMenu(category: LibraryCategory) {
    when (category) {
        LibraryCategory.ALBUMS -> { val s by SortSettings.albums.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.albums.group("Albums", AlbumSortOptions, s)) }
        LibraryCategory.ARTISTS -> { val s by SortSettings.artists.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.artists.group("Artists", DeviceArtistSortOptions, s, ::artistNaturalAscending)) }
        LibraryCategory.SONGS -> { val s by SortSettings.songs.state.collectAsStateWithLifecycle(); SortMenu(SortSettings.songs.group("Songs", SongSortOptions, s)) }
        LibraryCategory.PLAYLISTS -> PlaylistSortMenu()
        else -> {}
    }
}

/** Playlists (both sources): recently played (default), name, song count or recently added. */
@Composable
internal fun PlaylistSortMenu() {
    val s by SortSettings.playlists.state.collectAsStateWithLifecycle()
    SortMenu(SortSettings.playlists.group("Playlists", PlaylistSortOptions, s, ::playlistNaturalAscending))
}

/** Server artists: alphabetical (default) or most albums. */
@Composable
internal fun ServerArtistSortMenu() {
    val s by SortSettings.artists.state.collectAsStateWithLifecycle()
    SortMenu(SortSettings.artists.group("Artists", ServerArtistSortOptions, s, ::artistNaturalAscending))
}

@Composable
private fun Loading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
fun SongList(songs: List<Song>, modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null, showShuffle: Boolean = true) {
    val actions = LocalAppActions.current
    val currentId = rememberCurrentSongId()
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = PageBottomPadding)) {
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
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = PageBottomPadding)) {
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
