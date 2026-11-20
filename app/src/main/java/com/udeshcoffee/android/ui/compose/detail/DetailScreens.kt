package com.udeshcoffee.android.ui.compose.detail

import com.udeshcoffee.android.ui.compose.OptionsExtra
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.HideImage
import com.udeshcoffee.android.ui.compose.components.rememberOptionsAnchor
import com.udeshcoffee.android.ui.compose.components.PageLoadingIndicator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.udeshcoffee.android.ui.compose.theme.Section
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.runtime.mutableIntStateOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.AlbumArtwork
import com.udeshcoffee.android.ui.compose.components.AlbumCard
import com.udeshcoffee.android.ui.compose.components.ArtistCard
import com.udeshcoffee.android.ui.compose.components.albumCount
import com.udeshcoffee.android.data.subsonic.ArtistReleases
import com.udeshcoffee.android.data.subsonic.externalProvider
import com.udeshcoffee.android.data.subsonic.isExternal
import com.udeshcoffee.android.ui.compose.components.AlbumSortOptions
import com.udeshcoffee.android.ui.compose.components.ArtistArtwork
import com.udeshcoffee.android.ui.compose.components.EmptyState
import com.udeshcoffee.android.ui.compose.components.SectionHeader
import com.udeshcoffee.android.ui.compose.components.SongRow
import com.udeshcoffee.android.ui.compose.components.SongArtwork
import com.udeshcoffee.android.ui.compose.components.SongSortOptions
import com.udeshcoffee.android.ui.compose.components.SortMenu
import com.udeshcoffee.android.ui.compose.components.SortSettings
import com.udeshcoffee.android.ui.compose.components.formatDuration
import com.udeshcoffee.android.ui.compose.components.songCount
import com.udeshcoffee.android.ui.compose.rememberCurrentSongId
import com.udeshcoffee.android.ui.compose.components.TopLevelScaffold
import com.udeshcoffee.android.ui.compose.components.withPageBottom
import org.koin.compose.viewmodel.koinViewModel
import com.udeshcoffee.android.ui.compose.components.ArtworkImage
import com.udeshcoffee.android.ui.compose.components.DownloadButton
import com.udeshcoffee.android.utils.CoverArt
import com.udeshcoffee.android.utils.PlaylistPlays
import com.udeshcoffee.android.utils.artworkData
import com.udeshcoffee.android.ui.compose.components.icon
import com.udeshcoffee.android.utils.AlbumArt
import com.udeshcoffee.android.utils.artistImageFile
import com.udeshcoffee.android.ui.compose.theme.ArtworkTint
import com.udeshcoffee.android.ui.compose.theme.rememberArtworkSeed
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Collapsing top bar with back navigation; [content] gets the inner padding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScaffold(title: String, actions: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit) {
    val appActions = LocalAppActions.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            IconButton(onClick = { appActions.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                        },
                        actions = { actions() },
                        scrollBehavior = scrollBehavior)
            },
            content = { content(it.withPageBottom()) })
}

/**
 * Scaffold with a plain (non-collapsing) top bar. With a [listState] (pages with a [DetailHeader]:
 * album, artist, playlist) the title fades in once the header has scrolled away, as the header
 * shows it until then; without one it always shows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderScaffold(title: String, listState: LazyListState?, actions: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit) {
    val appActions = LocalAppActions.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val showTitle by remember(listState) { derivedStateOf { listState == null || listState.firstVisibleItemIndex > 0 } }
    Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                        title = {
                            AnimatedVisibility(showTitle, enter = fadeIn(), exit = fadeOut()) {
                                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { appActions.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                        },
                        actions = { actions() },
                        scrollBehavior = scrollBehavior)
            },
            content = { content(it.withPageBottom()) })
}

private val HeaderArtShape = RoundedCornerShape(12.dp)

/**
 * Album / artist header, as in the web player on mobile: the artwork centred on a rounded square,
 * then the [kind] label, the title, a [subtitle] (tappable with [onSubtitleClick]), [pills] (year,
 * genre...), a [details] line (song count, length) and the [buttons] row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(
        artwork: @Composable (Modifier, androidx.compose.ui.graphics.Shape) -> Unit,
        kind: String,
        title: String,
        subtitle: String? = null,
        onSubtitleClick: (() -> Unit)? = null,
        pills: List<String> = emptyList(),
        details: String? = null,
        buttons: @Composable RowScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
        artwork(Modifier.size(200.dp).shadow(16.dp, HeaderArtShape), HeaderArtShape)
        Spacer(Modifier.height(20.dp))
        Text(kind.uppercase(), style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = if (onSubtitleClick != null) Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onSubtitleClick).padding(horizontal = 4.dp)
                    else Modifier)
        }
        if (pills.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pills.forEach { pill ->
                    Text(pill.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
        details?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
                content = buttons)
    }
}

/** The header's Play and Shuffle buttons (pill-shaped, as in the web player). */
@Composable
private fun RowScope.HeaderPlayButtons(enabled: Boolean, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Button(onClick = onPlay, enabled = enabled, shapes = ButtonDefaults.shapes()) {
        Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("PLAY", letterSpacing = 1.sp)
    }
    OutlinedButton(onClick = onShuffle, enabled = enabled, shapes = ButtonDefaults.shapes()) {
        Icon(Icons.Default.Shuffle, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("SHUFFLE", letterSpacing = 1.sp)
    }
}

/** Heart for a starred album / artist, in the Favorites pink when on. */
@Composable
private fun HeaderHeart(starred: Boolean, onToggle: () -> Unit) {
    val pink = if (isSystemInDarkTheme()) Section.FAVORITES.accent else Section.FAVORITES.lightInk
    IconButton(onClick = onToggle) {
        if (starred) Icon(Icons.Default.Favorite, contentDescription = "Remove from favorites", tint = pink)
        else Icon(Icons.Default.FavoriteBorder, contentDescription = "Add to favorites", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { PageLoadingIndicator() }

/** Songs, or a spinner while loading, or the music server error with a retry button. */
private fun LazyListScope.songsOrState(songs: List<Song>?, error: String?, onRetry: () -> Unit, content: LazyListScope.(List<Song>) -> Unit) {
    when {
        songs != null -> content(songs)
        error != null -> item {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Button(onClick = onRetry) { Text("Try again") }
            }
        }
        else -> item { Loading() }
    }
}

/** Play / Shuffle buttons for a song collection. */
@Composable
private fun PlayButtons(songs: List<Song>) {
    val actions = LocalAppActions.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { actions.play(songs) }, enabled = songs.isNotEmpty(), modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.PlayArrow, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Play")
        }
        OutlinedButton(onClick = { actions.shuffle(songs) }, enabled = songs.size > 1, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Shuffle, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Shuffle")
        }
    }
}

/** Overflow button opening the collection options menu (play next, queue, add to playlist...). */
@Composable
private fun CollectionMenuButton(title: String, songs: List<Song>) {
    val actions = LocalAppActions.current
    val anchor = rememberOptionsAnchor()
    IconButton(onClick = anchor.open { actions.collectionOptions(title, songs) }, enabled = songs.isNotEmpty(), modifier = anchor.modifier) {
        Icon(Icons.Default.MoreVert, contentDescription = "More options")
    }
}

/** [onPlayed] runs when a song is started from the list (playlists record it for their "Recently played" sort). */
private fun LazyListScope.songItems(songs: List<Song>, currentId: Long?, playlist: Playlist? = null, showArtwork: Boolean = true,
                                    onPlayed: () -> Unit = {}) {
    items(songs, key = { if (playlist != null) "${it.playlistSongId}" else "${it.id}" }) { song ->
        val actions = LocalAppActions.current
        SongRow(song, onClick = { actions.play(songs, songs.indexOf(song)); onPlayed() }, onMore = { actions.songOptions(song, playlist) },
                isCurrent = song.id == currentId, showArtwork = showArtwork)
    }
}

@Composable
private fun AlbumsRow(albums: List<Album>) {
    val actions = LocalAppActions.current
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(album, onClick = { actions.openAlbum(album) }, onLongClick = { actions.albumOptions(album) },
                    onPlay = { actions.playAlbum(album) }, modifier = Modifier.width(150.dp))
        }
    }
}

// ---------------- Album ----------------

@Composable
fun AlbumDetailScreen(albumId: Long, title: String, remoteId: String? = null, coverArt: String? = null) {
    val viewModel: AlbumDetailViewModel = koinViewModel(key = "album:$albumId") { parametersOf(albumId, remoteId) }
    // Server art: the cover art ID from the server's response (or the list it was opened from), which
    // changes when the artwork does; the album ID itself would keep showing a cached old image.
    val serverCoverArt by viewModel.coverArt.collectAsStateWithLifecycle()
    val artwork = remember(albumId, remoteId, serverCoverArt, coverArt) {
        if (remoteId != null) CoverArt(serverCoverArt ?: coverArt ?: remoteId) else AlbumArt(albumId)
    }
    ArtworkTint(rememberArtworkSeed(artwork)) { AlbumDetailContent(viewModel, artwork, albumId, title, remoteId) }
}

@Composable
private fun AlbumDetailContent(viewModel: AlbumDetailViewModel, artwork: Any, albumId: Long, title: String, remoteId: String?) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val providerSongs by viewModel.providerSongs.collectAsStateWithLifecycle()
    val missingSongs by viewModel.missingSongs.collectAsStateWithLifecycle()
    val addedToLibrary by viewModel.addedToLibrary.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val genre by viewModel.genre.collectAsStateWithLifecycle()
    val starred by viewModel.starred.collectAsStateWithLifecycle()
    val sort by SortSettings.albumSongs.state.collectAsStateWithLifecycle()
    val currentId = rememberCurrentSongId()
    val actions = LocalAppActions.current
    val listState = rememberLazyListState()
    HeaderScaffold(title, listState, actions = {
        if (remoteId != null) DownloadButton(songs.orEmpty())
        SortMenu(SortSettings.albumSongs.group(SongSortOptions, sort))
        CollectionMenuButton(title, songs.orEmpty())
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item(key = "header") {
                val list = songs.orEmpty()
                val first = list.firstOrNull()
                val year = list.firstNotNullOfOrNull { it.year.takeIf { y -> y > 0 } }
                DetailHeader(
                        artwork = { modifier, shape ->
                            if (remoteId != null) ArtworkImage(artwork, Icons.Default.Album, modifier, shape)
                            else AlbumArtwork(albumId, modifier, shape)
                        },
                        kind = "Album",
                        title = title,
                        subtitle = first?.artistName,
                        onSubtitleClick = first?.let { { actions.openArtist(it) } },
                        pills = listOfNotNull(year?.toString(), genre),
                        details = songs?.let { "${songCount(it.size)} • ${formatDuration(it.sumOf { s -> s.duration })}" }) {
                    HeaderPlayButtons(list.isNotEmpty(), onPlay = { actions.play(list) }, onShuffle = { actions.shuffle(list) })
                    if (remoteId != null && missingSongs.isNotEmpty()) AddToLibraryButton(remoteId, title, missingSongs, addedToLibrary)
                    if (remoteId != null) HeaderHeart(starred, viewModel::toggleStar)
                }
            }
            songsOrState(songs, error, viewModel::retry) { songItems(it, currentId, showArtwork = false) }
            if (providerSongs.isNotEmpty()) providerSongItems(providerSongs, currentId, showArtwork = false)
        }
    }
}

/**
 * "Add to library" for an album with streaming-provider tracks (Octo-Fiesta): has the server
 * download them. Shows a check once requested (the server reports no progress).
 */
@Composable
private fun AddToLibraryButton(albumId: String, title: String, missing: List<Song>, added: Boolean) {
    val actions = LocalAppActions.current
    IconButton(onClick = { actions.addAlbumToLibrary(albumId, title, missing) }, enabled = !added) {
        if (added) Icon(Icons.Default.LibraryAddCheck, contentDescription = "Added to library")
        else Icon(Icons.Default.LibraryAdd, contentDescription = "Add to library")
    }
}

/**
 * Streaming-provider songs (Octo-Fiesta) next to library ones. Tapping one plays only it: playing
 * makes the server download the song, so queueing its neighbours would download them too.
 */
private fun LazyListScope.providerSongItems(songs: List<Song>, currentId: Long?, showArtwork: Boolean = true) {
    val provider = externalProvider(songs.first().remoteId) ?: "the streaming service"
    item(key = "provider-header") {
        SectionHeader("Not in your library")
        Text("From $provider. Playing a song adds it to your server.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp))
    }
    items(songs, key = { "provider-${it.id}" }) { song ->
        val actions = LocalAppActions.current
        SongRow(song, onClick = { actions.play(listOf(song)) }, onMore = { actions.songOptions(song) },
                isCurrent = song.id == currentId, showArtwork = showArtwork)
    }
}

// ---------------- Artist ----------------

@Composable
fun ArtistDetailScreen(artistId: Long, name: String, remoteId: String? = null, coverArt: String? = null) {
    val viewModel: ArtistDetailViewModel = koinViewModel(key = "artist:$artistId") { parametersOf(artistId, name, remoteId) }
    // As for albums: the server's current cover art ID, not the artist ID (a stale cached image).
    val serverCoverArt by viewModel.coverArt.collectAsStateWithLifecycle()
    val currentCoverArt = serverCoverArt ?: coverArt
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val imageVersion by viewModel.imageVersion.collectAsStateWithLifecycle()
    // The artist's image: the server's, or the stored one (checked off the main thread), else their first album's art.
    val artwork by produceState<Any?>(null, remoteId, currentCoverArt, imageVersion, albums.firstOrNull()?.id) {
        value = remoteId?.let { CoverArt(currentCoverArt ?: it) }
                ?: withContext(Dispatchers.IO) { artistImageFile(artistId).takeIf { it.exists() } }
                ?: albums.firstOrNull()?.let { AlbumArt(it.id) }
    }
    // imageVersion: a replaced image file is the same File.
    ArtworkTint(rememberArtworkSeed(artwork, key = imageVersion)) { ArtistDetailContent(viewModel, artistId, name, remoteId, currentCoverArt) }
}

@Composable
private fun ArtistDetailContent(viewModel: ArtistDetailViewModel, artistId: Long, name: String, remoteId: String?, coverArt: String?) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val releases by viewModel.releases.collectAsStateWithLifecycle()
    val similar by viewModel.similar.collectAsStateWithLifecycle()
    val bio by viewModel.bio.collectAsStateWithLifecycle()
    val imageVersion by viewModel.imageVersion.collectAsStateWithLifecycle()
    val songSort by SortSettings.artistSongs.state.collectAsStateWithLifecycle()
    val albumSort by SortSettings.artistAlbums.state.collectAsStateWithLifecycle()
    val currentId = rememberCurrentSongId()
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.setImage(context, it) }
    }
    val starred by viewModel.starred.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val artist = Artist(artistId, name, remoteId, coverArt)
    val listState = rememberLazyListState()

    HeaderScaffold(name, listState, actions = {
        // Server artists' songs are their top songs, in rank order.
        if (viewModel.isRemote) SortMenu(SortSettings.artistAlbums.group(AlbumSortOptions, albumSort))
        else SortMenu(SortSettings.artistAlbums.group(AlbumSortOptions, albumSort),
                SortSettings.artistSongs.group(SongSortOptions, songSort))
        val anchor = rememberOptionsAnchor()
        IconButton(modifier = anchor.modifier, onClick = anchor.open {
            if (viewModel.isRemote) actions.artistOptions(artist)
            // Device artists also get their image actions (server artists use the server's image).
            else actions.collectionOptions(name, songs.orEmpty(), extras = listOf(
                    OptionsExtra(Icons.Default.Image, "Choose image") {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    OptionsExtra(Icons.Default.HideImage, "Remove image") { viewModel.removeImage() }))
        }) { Icon(Icons.Default.MoreVert, contentDescription = "More options") }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item(key = "header") {
                val details = if (viewModel.isRemote) releases?.let { albumCount(it.libraryCount) }
                else songs?.let { "${songCount(it.size)} • ${albumCount(albums.size)}" }
                DetailHeader(
                        artwork = { modifier, shape ->
                            androidx.compose.runtime.key(imageVersion) { ArtistArtwork(artist, modifier, shape) }
                        },
                        kind = "Artist",
                        title = name,
                        pills = bio?.second.orEmpty().take(3),
                        details = details) {
                    // Server artists: their library songs, loaded when pressed (a request per album).
                    if (viewModel.isRemote) {
                        HeaderPlayButtons(releases != null, onPlay = { actions.playArtist(artist) }, onShuffle = { actions.shuffleArtist(artist) })
                        HeaderHeart(starred, viewModel::toggleStar)
                    } else {
                        val list = songs.orEmpty()
                        HeaderPlayButtons(list.isNotEmpty(), onPlay = { actions.play(list) }, onShuffle = { actions.shuffle(list) })
                    }
                }
                bio?.first?.let { BioText(it) }
            }
            if (viewModel.isRemote) serverArtistItems(releases, songs, similar, error, viewModel::retry, currentId)
            else {
                if (albums.isNotEmpty()) {
                    item { SectionHeader("Albums") }
                    item { AlbumsRow(albums) }
                }
                item { SectionHeader("Songs") }
                songsOrState(songs, error, viewModel::retry) { songItems(it, currentId) }
            }
        }
    }
}

/**
 * A server artist, as in the web player: top songs, the library's releases by kind, then the
 * streaming provider's (Octo-Fiesta), then similar artists.
 */
private fun LazyListScope.serverArtistItems(
        releases: ArtistReleases?, topSongs: List<Song>?, similar: List<Artist>, error: String?, onRetry: () -> Unit, currentId: Long?
) {
    if (releases == null) {
        songsOrState(null, error, onRetry) {}
        return
    }
    val (library, provider) = topSongs.orEmpty().partition { !it.isExternal }
    if (library.isNotEmpty()) {
        item(key = "top-header") { SectionHeader("Top songs") }
        songItems(library, currentId)
    }
    listOf("Albums" to releases.albums, "Singles & EPs" to releases.singles, "Appears on" to releases.appearsOn).forEach { (title, list) ->
        if (list.isNotEmpty()) {
            item(key = "header-$title") { SectionHeader(title) }
            item(key = "row-$title") { AlbumsRow(list) }
        }
    }
    if (releases.provider.isNotEmpty()) {
        item(key = "provider-albums-header") {
            SectionHeader("On ${releases.providerName ?: "streaming"}")
            val hidden = if (releases.hiddenCovers > 0) " Covers and karaoke versions are hidden." else ""
            Text("Not in your library. Playing adds songs to your server.$hidden", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp))
        }
        item(key = "provider-albums") { AlbumsRow(releases.provider) }
    }
    if (provider.isNotEmpty()) providerSongItems(provider, currentId)
    if (similar.isNotEmpty()) {
        item(key = "similar-header") { SectionHeader("Similar artists") }
        item(key = "similar") { ArtistsRow(similar) }
    }
    if (releases.libraryCount == 0 && releases.provider.isEmpty() && topSongs.isNullOrEmpty()) {
        item {
            Text("No albums on the server", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun ArtistsRow(artists: List<Artist>) {
    val actions = LocalAppActions.current
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(artists, key = { it.id }) { artist ->
            ArtistCard(artist, onClick = { actions.openArtist(artist) }, onLongClick = { actions.artistOptions(artist) },
                    onPlay = { actions.playArtist(artist) }, modifier = Modifier.width(150.dp))
        }
    }
}

/** Artist bio (HTML with a "read more" link), collapsed to a few lines until tapped. */
@Composable
private fun BioText(html: String) {
    var expanded by remember { mutableStateOf(false) }
    val text = remember(html) { AnnotatedString.fromHtml(html.replace("\n", "<br>")) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).animateContentSize().clickable { expanded = !expanded }) {
        Text("About", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------- Genre ----------------

@Composable
fun GenreDetailScreen(genreId: Long, name: String, remote: Boolean = false) {
    val viewModel: GenreDetailViewModel = koinViewModel(key = "genre:$genreId") { parametersOf(genreId, name, remote) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val songSort by SortSettings.genreSongs.state.collectAsStateWithLifecycle()
    val albumSort by SortSettings.genreAlbums.state.collectAsStateWithLifecycle()
    val currentId = rememberCurrentSongId()
    val actions = LocalAppActions.current
    HeaderScaffold(name, listState = null, actions = {
        SortMenu(SortSettings.genreAlbums.group(AlbumSortOptions, albumSort),
                SortSettings.genreSongs.group(SongSortOptions, songSort))
        CollectionMenuButton(name, songs.orEmpty())
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item {
                val list = songs.orEmpty()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    HeaderPlayButtons(list.isNotEmpty(), onPlay = { actions.play(list) }, onShuffle = { actions.shuffle(list) })
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionHeader("Albums") }
                item { AlbumsRow(albums) }
            }
            item { SectionHeader("Songs") }
            songsOrState(songs, error, viewModel::retry) { songItems(it, currentId) }
        }
    }
}

// ---------------- Favorites ----------------

@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel = koinViewModel()) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val sort by SortSettings.favoriteSongs.state.collectAsStateWithLifecycle()
    val currentId = rememberCurrentSongId()
    TopLevelScaffold("Favorites", actions = {
        SortMenu(SortSettings.favoriteSongs.group(SongSortOptions, sort))
        CollectionMenuButton("Favorites", songs.orEmpty())
    }) { padding ->
        when {
            songs == null -> Loading()
            songs!!.isEmpty() && error != null -> Column(Modifier.padding(padding)) { ServerNotice("Couldn't load favorites: $error", viewModel::retry) }
            songs!!.isEmpty() -> EmptyState("Songs you favorite appear here", Modifier.padding(padding), Icons.Default.FavoriteBorder)
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                error?.let { item { ServerNotice("Couldn't load favorites: $it", viewModel::retry) } }
                item { PlayButtons(songs!!) }
                songItems(songs!!, currentId)
            }
        }
    }
}

// ---------------- Playlist ----------------

@Composable
fun PlaylistDetailScreen(playlistId: Long, type: Int, title: String, remoteId: String? = null, readonly: Boolean = false, coverArt: String? = null) {
    val viewModel: PlaylistDetailViewModel = koinViewModel(key = "playlist:$type:$playlistId") { parametersOf(playlistId, type, remoteId) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    // Header art: the server playlist's cover (by its cover art ID, which changes with the artwork), else its first song's artwork.
    val artwork = remoteId?.let { CoverArt(coverArt ?: it) } ?: songs?.firstOrNull()?.artworkData()
    ArtworkTint(rememberArtworkSeed(artwork)) { PlaylistDetailContent(viewModel, artwork, playlistId, type, title, remoteId, readonly) }
}

@Composable
private fun PlaylistDetailContent(viewModel: PlaylistDetailViewModel, artwork: Any?, playlistId: Long, type: Int, title: String, remoteId: String?, readonly: Boolean) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val currentId = rememberCurrentSongId()
    val playlist = remember(playlistId, type, title) { Playlist(type, playlistId, title, remoteId, readonly) }
    // Editable: device user playlists, and server playlists the server lets us change.
    val isUser = type == Playlist.USER && !readonly
    val listState = rememberLazyListState()

    HeaderScaffold(title, listState, actions = {
        // Picking from the device library only fits device playlists; server playlists get songs via "Add to playlist".
        if (isUser && remoteId == null) IconButton(onClick = { actions.openPlaylistAddSongs(playlist) }) { Icon(Icons.Default.Add, contentDescription = "Add songs") }
        // Server playlists download for offline listening; the Downloads list offers removing them all.
        if (remoteId != null || type == Playlist.DOWNLOADS) DownloadButton(songs.orEmpty())
        val anchor = rememberOptionsAnchor()
        IconButton(onClick = anchor.open { actions.playlistPageOptions(playlist, songs.orEmpty()) }, modifier = anchor.modifier) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
    }) { padding ->
        val list = songs
        val header: @Composable (List<Song>) -> Unit = { shown ->
            DetailHeader(
                    artwork = { modifier, shape ->
                        if (artwork != null) ArtworkImage(artwork, playlist.icon, modifier, shape)
                        else Box(modifier.background(MaterialTheme.colorScheme.secondaryContainer, shape), contentAlignment = Alignment.Center) {
                            Icon(playlist.icon, contentDescription = null, modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    },
                    // Device smart playlists (recently added / played...), Downloads and read-only server playlists.
                    kind = if (type == Playlist.USER && !readonly) "Playlist" else "Smart playlist",
                    title = title,
                    pills = listOfNotNull("Read-only".takeIf { readonly && remoteId != null }),
                    details = list?.let { "${songCount(shown.size)} • ${formatDuration(shown.sumOf { it.duration })}" }) {
                HeaderPlayButtons(shown.isNotEmpty(),
                        onPlay = { actions.play(shown); PlaylistPlays.record(playlist) },
                        onShuffle = { actions.shuffle(shown); PlaylistPlays.record(playlist) })
            }
        }
        if (list != null && list.isNotEmpty() && isUser) {
            ReorderablePlaylist(list, playlist, viewModel, currentId, padding, listState, header)
            return@HeaderScaffold
        }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item(key = "header") { header(list.orEmpty()) }
            when {
                list == null -> songsOrState(null, error, viewModel::retry) {}
                list.isEmpty() -> item {
                    Text(when {
                        type == Playlist.DOWNLOADS -> "Downloaded songs appear here. Download songs, albums and playlists from their options."
                        remoteId != null -> "Add songs with \"Add to playlist\" in a song's options"
                        isUser -> "Add songs with the + button"
                        else -> "Nothing here yet"
                    }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp))
                }
                else -> songItems(list, currentId) { PlaylistPlays.record(playlist) }
            }
        }
    }
}

/** User playlist with drag handles below the [header]; the new order is saved when a drag ends. */
@Composable
private fun ReorderablePlaylist(songs: List<Song>, playlist: Playlist, viewModel: PlaylistDetailViewModel, currentId: Long?, padding: PaddingValues,
                                listState: LazyListState, header: @Composable (List<Song>) -> Unit) {
    val actions = LocalAppActions.current
    val items = remember { mutableStateListOf<Song>() }
    var dragFrom by remember { mutableIntStateOf(-1) }
    LaunchedEffect(songs) { if (dragFrom < 0) { items.clear(); items.addAll(songs) } }
    val headerCount = 1
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val f = from.index - headerCount
        val t = to.index - headerCount
        if (f in items.indices && t in items.indices) items.add(t, items.removeAt(f))
    }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
        item(key = "header") { header(items) }
        items(items, key = { it.playlistSongId }) { song ->
            ReorderableItem(reorderState, key = song.playlistSongId) { _ ->
                SongRow(song,
                        onClick = { actions.play(items.toList(), items.indexOf(song)); PlaylistPlays.record(playlist) },
                        onMore = { actions.songOptions(song, playlist) },
                        isCurrent = song.id == currentId,
                        // The handle is on the left, away from the options button and the right-hand scrolling thumb.
                        leading = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {}, modifier = Modifier.draggableHandle(
                                        onDragStarted = { dragFrom = items.indexOf(song) },
                                        onDragStopped = {
                                            val to = items.indexOf(song)
                                            if (dragFrom >= 0 && to != dragFrom) viewModel.move(dragFrom, to)
                                            dragFrom = -1
                                        })) {
                                    Icon(Icons.Default.DragHandle, contentDescription = "Reorder")
                                }
                                SongArtwork(song, Modifier.size(48.dp))
                            }
                        })
            }
        }
    }
}

@Composable
fun PlaylistAddSongsScreen(playlistId: Long, title: String) {
    val viewModel: PlaylistAddSongsViewModel = koinViewModel(key = "add:$playlistId") { parametersOf(playlistId) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    DetailScaffold("Add to $title") { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            items(songs, key = { it.id }) { song ->
                ListItem(
                        onClick = { viewModel.toggle(song) },
                        leadingContent = { com.udeshcoffee.android.ui.compose.components.SongArtwork(song, Modifier.size(48.dp)) },
                        supportingContent = { Text(song.artistName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = { Checkbox(checked = song.id in selected, onCheckedChange = { viewModel.toggle(song) }) }) { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** A one-line problem with the music server, with a retry. */
@Composable
fun ServerNotice(message: String, onRetry: () -> Unit) {
    ListItem(
            leadingContent = { Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            trailingContent = { androidx.compose.material3.TextButton(onClick = onRetry) { Text("Retry") } }) { Text(message, style = MaterialTheme.typography.bodyMedium) }
}
