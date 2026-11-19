@file:OptIn(ExperimentalCoroutinesApi::class)

package com.udeshcoffee.android.ui.compose.detail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.download.DownloadRepository
import com.udeshcoffee.android.data.media.songsToAlbums
import com.udeshcoffee.android.data.media.songsToArtists
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.components.SortSettings
import com.udeshcoffee.android.ui.compose.components.sortedAlbums
import com.udeshcoffee.android.ui.compose.components.sortedSongs
import com.udeshcoffee.android.ui.compose.components.sortedPlaylists
import com.udeshcoffee.android.utils.PlaylistPlays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import com.udeshcoffee.android.utils.deleteArtistArt
import com.udeshcoffee.android.utils.saveArtistImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import com.udeshcoffee.android.data.source.MusicSource
import com.udeshcoffee.android.data.source.MusicSourceStore
import kotlinx.coroutines.flow.transformLatest
import com.udeshcoffee.android.data.subsonic.AlbumTracks
import com.udeshcoffee.android.data.subsonic.ArtistReleases
import com.udeshcoffee.android.data.subsonic.ServerAlbumDetails
import com.udeshcoffee.android.data.subsonic.SubsonicException
import com.udeshcoffee.android.data.subsonic.groupReleases
import com.udeshcoffee.android.data.subsonic.SubsonicRepository

private fun <T> Flow<T>.stateIn(vm: ViewModel, initial: T): StateFlow<T> =
        stateIn(vm.viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

/**
 * Loads [load] from the music server, emitting null first (loading). Failures land in [error];
 * bumping [reload] tries again.
 */
private fun <T> serverFlow(reload: StateFlow<Int>, error: MutableStateFlow<String?>, load: suspend () -> T): Flow<T?> =
        reload.flatMapLatest {
            flow {
                emit(null)
                error.value = null
                emit(try { load() } catch (e: SubsonicException) { error.value = e.message ?: "Couldn't reach the server"; null })
            }
        }

/** Shared by detail screens that can show music server content: an error message and a retry. */
abstract class SourceAwareViewModel : ViewModel() {
    protected val reload = MutableStateFlow(0)
    protected val _error = MutableStateFlow<String?>(null)
    /** The music server request failed (shown with a retry button). */
    val error: StateFlow<String?> = _error.asStateFlow()
    fun retry() { reload.value++ }

    protected fun <T> server(load: suspend () -> T): Flow<T?> = serverFlow(reload, _error, load)
}

class AlbumDetailViewModel(albumId: Long, private val remoteId: String?, mediaRepository: MediaRepository, private val subsonic: SubsonicRepository) : SourceAwareViewModel() {
    private val details: Flow<ServerAlbumDetails?> =
            if (remoteId != null) server { subsonic.albumDetails(remoteId) }
            else mediaRepository.observeAlbumSongs(albumId).map { ServerAlbumDetails(AlbumTracks(it, emptyList()), genre = null, year = 0) }

    private val tracks: Flow<AlbumTracks?> = details.map { it?.tracks }

    /** The server's genre for the album (header pill); null for device albums. */
    val genre: StateFlow<String?> = details.map { it?.genre }.stateIn(this, null)

    /** The album's library songs (all of a streaming-provider album's), sorted. */
    val songs: StateFlow<List<Song>?> =
            combine(tracks, SortSettings.albumSongs.state) { t, sort -> t?.playable?.sortedSongs(sort) }.stateIn(this, null)

    /** Tracks Octo-Fiesta adds from the streaming provider that the library copy lacks, in album order. */
    val providerSongs: StateFlow<List<Song>> =
            tracks.map { t -> t?.takeIf { it.library.isNotEmpty() }?.provider?.sortedBy { it.track }.orEmpty() }.stateIn(this, emptyList())

    /** The album is starred on the server (the header's heart; server albums only). */
    val starred: StateFlow<Boolean> = subsonic.starredAlbumIds.map { remoteId != null && remoteId in it }.stateIn(this, false)

    fun toggleStar() {
        val id = remoteId ?: return
        viewModelScope.launch { try { subsonic.setAlbumStarred(id, !starred.value) } catch (_: SubsonicException) {} }
    }

    /** Every streaming-provider track of the album (all of a provider album's): what "Add to library" downloads. */
    val missingSongs: StateFlow<List<Song>> = tracks.map { it?.provider.orEmpty() }.stateIn(this, emptyList())

    /** "Add to library" was requested for this album this session. */
    val addedToLibrary: StateFlow<Boolean> =
            subsonic.addedAlbums.map { remoteId != null && remoteId in it }.stateIn(this, false)
}

class ArtistDetailViewModel(
        private val artistId: Long,
        private val artistName: String,
        private val remoteId: String?,
        mediaRepository: MediaRepository,
        dataRepository: DataRepository,
        private val subsonic: SubsonicRepository
) : SourceAwareViewModel() {
    val isRemote = remoteId != null

    /**
     * Device artists: all their songs. Server artists: their top songs, as loading every album's
     * songs would cost a request per album (and, behind Octo-Fiesta, a provider search each).
     */
    private val songSource: Flow<List<Song>?> =
            if (remoteId != null) server {
                // Optional (servers without Last.fm have none): a failure here shouldn't fail the page.
                try { subsonic.topSongs(remoteId, artistName) } catch (_: SubsonicException) { emptyList() }
            } else mediaRepository.observeArtistSongs(artistId)

    val songs: StateFlow<List<Song>?> =
            (if (remoteId != null) songSource
            else combine(songSource, SortSettings.artistSongs.state) { s, sort -> s?.sortedSongs(sort) }).stateIn(this, null)

    /** Device artists' albums (server artists have [releases]). */
    val albums: StateFlow<List<Album>> =
            (if (remoteId != null) flowOf(emptyList())
            else combine(mediaRepository.observeArtistAlbums(artistName), SortSettings.artistAlbums.state) { a, sort -> a.sortedAlbums(sort) })
                    .stateIn(this, emptyList())

    /** The artist is starred on the server (the header's heart; server artists only). */
    val starred: StateFlow<Boolean> = subsonic.starredArtistIds.map { remoteId != null && remoteId in it }.stateIn(this, false)

    fun toggleStar() {
        val id = remoteId ?: return
        viewModelScope.launch { try { subsonic.setArtistStarred(id, !starred.value) } catch (_: SubsonicException) {} }
    }

    /** Server artists' releases: the library's by kind, then the streaming provider's. Null until loaded or for device artists. */
    val releases: StateFlow<ArtistReleases?> =
            (if (remoteId == null) flowOf(null)
            else combine(server { subsonic.artistAlbums(remoteId) }, SortSettings.artistAlbums.state) { a, sort ->
                a?.let { groupReleases(it, artistName) { group -> group.sortedAlbums(sort) } }
            }).stateIn(this, null)

    private val _similar = MutableStateFlow<List<Artist>>(emptyList())
    /** Similar artists in the server's library (from Last.fm through the server). */
    val similar: StateFlow<List<Artist>> = _similar.asStateFlow()

    private val _bio = MutableStateFlow<Pair<String, List<String>>?>(null)
    /** Bio (may contain links) and tags: from the server for server artists, else Last.fm. Null until loaded or when unavailable. */
    val bio: StateFlow<Pair<String, List<String>>?> = _bio.asStateFlow()

    /** Bumped when the artist image changes so the UI reloads it. */
    val imageVersion = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            _bio.value = if (remoteId != null) {
                val info = runCatching { subsonic.artistInfo(remoteId) }.getOrNull()
                _similar.value = info?.similarArtists.orEmpty()
                info?.biography?.let { it to emptyList() }
            } else {
                dataRepository.getBio(artistId, artistName)?.let { (bio, tags) -> bio to tags.orEmpty().toList() }
            }
        }
    }

    fun setImage(context: Context, uri: Uri) = viewModelScope.launch {
        saveArtistImage(context, artistId, uri)
        imageVersion.value++
    }

    fun removeImage() {
        deleteArtistArt(artistId)
        imageVersion.value++
    }
}

class GenreDetailViewModel(genreId: Long, name: String, remote: Boolean, mediaRepository: MediaRepository, subsonic: SubsonicRepository) : SourceAwareViewModel() {
    private val songSource: Flow<List<Song>?> =
            if (remote) server { subsonic.genreSongs(name, count = 500) } else mediaRepository.observeGenreSongs(genreId)

    val songs: StateFlow<List<Song>?> =
            combine(songSource, SortSettings.genreSongs.state) { s, sort -> s?.sortedSongs(sort) }.stateIn(this, null)

    private val albumSource: Flow<List<Album>> =
            if (remote) server { subsonic.genreAlbums(name) }.map { it.orEmpty() } else mediaRepository.observeGenreAlbums(genreId)

    val albums: StateFlow<List<Album>> =
            combine(albumSource, SortSettings.genreAlbums.state) { a, sort -> a.sortedAlbums(sort) }.stateIn(this, emptyList())
}

class PlaylistDetailViewModel(
        val playlistId: Long,
        val type: Int,
        private val remoteId: String?,
        private val mediaRepository: MediaRepository,
        private val subsonic: SubsonicRepository,
        downloads: DownloadRepository
) : SourceAwareViewModel() {

    /** Server playlists reload after edits. Their entries carry their position as [Song.playlistSongId]. */
    private val remoteSongs = MutableStateFlow<List<Song>?>(null)

    /** Playlists keep their own order; Downloads (stored on the device, so it works offline) is newest first. */
    val songs: StateFlow<List<Song>?> = when {
        type == Playlist.DOWNLOADS -> downloads.downloadedSongs
        remoteId != null -> remoteSongs
        else -> mediaRepository.observePlaylistSongs(playlistId, type)
    }.stateIn(this, null)

    init {
        if (remoteId != null) viewModelScope.launch {
            combine(subsonic.playlistsVersion, reload) { a, b -> a to b }.collectLatest {
                _error.value = null
                try {
                    remoteSongs.value = subsonic.playlistSongs(remoteId).onEachIndexed { i, song -> song.playlistSongId = i.toLong() }
                } catch (e: SubsonicException) {
                    _error.value = e.message
                }
            }
        }
    }

    fun move(from: Int, to: Int) = viewModelScope.launch {
        if (remoteId == null) {
            mediaRepository.movePlaylistItem(playlistId, from, to)
            return@launch
        }
        // Subsonic can only replace the whole list: reorder locally, then send it.
        val list = remoteSongs.value?.toMutableList() ?: return@launch
        list.add(to, list.removeAt(from))
        remoteSongs.value = list.onEachIndexed { i, song -> song.playlistSongId = i.toLong() }
        try {
            subsonic.setPlaylistSongs(remoteId, list.mapNotNull { it.remoteId })
        } catch (e: SubsonicException) {
            _error.value = e.message
            reload.value++
        }
    }
}

/** Add or remove library songs to/from a user playlist. */
class PlaylistAddSongsViewModel(private val playlistId: Long, private val mediaRepository: MediaRepository) : ViewModel() {
    val songs: StateFlow<List<Song>> = mediaRepository.observeSongs().stateIn(this, emptyList())

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    init {
        viewModelScope.launch { _selected.value = mediaRepository.getPlaylistSongIds(playlistId).toSet() }
    }

    fun toggle(song: Song) = viewModelScope.launch {
        if (song.id in _selected.value) {
            mediaRepository.removeSongFromPlaylist(playlistId, song.id)
            _selected.value = _selected.value - song.id
        } else {
            mediaRepository.addSongToPlaylist(playlistId, song.id)
            _selected.value = _selected.value + song.id
        }
    }
}

/** Favorites: device songs favorited in the app, or songs starred on the server (by [MusicSource]). */
class FavoritesViewModel(sourceStore: MusicSourceStore, mediaRepository: MediaRepository, subsonic: SubsonicRepository) : SourceAwareViewModel() {
    private val favorites: Flow<List<Song>> = if (sourceStore.isServer) {
        // Reload when stars change elsewhere (e.g. from the player).
        combine(reload, subsonic.starredIds.map { it.size }.distinctUntilChanged()) { _, _ -> }.mapLatest {
            _error.value = null
            try { subsonic.loadStarred() } catch (e: SubsonicException) { _error.value = e.message; emptyList() }
        }.distinctUntilChanged()
    } else mediaRepository.observeFavorites()

    val songs: StateFlow<List<Song>?> =
            combine(favorites, SortSettings.favoriteSongs.state) { songs, sort -> songs.sortedSongs(sort) }.stateIn(this, null)
}

/**
 * Playlists of the current [MusicSource]: the app's own (including the automatic ones built from
 * play stats) on the device; on a server, Downloads (offline songs, shown even when the server
 * can't be reached) followed by the server's. Null while loading.
 */
class PlaylistsViewModel(
        sourceStore: MusicSourceStore,
        mediaRepository: MediaRepository,
        subsonic: SubsonicRepository,
        downloads: DownloadRepository
) : SourceAwareViewModel() {
    val isServer = sourceStore.isServer

    private val serverPlaylists = combine(subsonic.playlistsVersion, reload) { a, b -> a to b }.flatMapLatest {
        flow {
            _error.value = null
            emit(try { subsonic.playlists() } catch (e: SubsonicException) { _error.value = e.message; emptyList() })
        }
    }

    private val unsorted: Flow<List<Playlist>> = if (isServer) {
        combine(serverPlaylists, downloads.downloadedIds) { server, downloaded ->
            listOf(Playlist(Playlist.DOWNLOADS).also { it.songCount = downloaded.size }) + server
        }
    } else mediaRepository.observePlaylists()

    /** In the chosen order ("Recently played" uses [PlaylistPlays], loaded off the main thread first). */
    val playlists: StateFlow<List<Playlist>?> =
            combine(unsorted, SortSettings.playlists.state, PlaylistPlays.plays.filterNotNull()) { list, sort, plays ->
                list.sortedPlaylists(sort) { plays[PlaylistPlays.key(it)] }
            }.stateIn(this, null)

    init {
        viewModelScope.launch(Dispatchers.IO) { PlaylistPlays.load() }
    }
}

data class SearchResults(
        val songs: List<Song> = emptyList(),
        val albums: List<Album> = emptyList(),
        val artists: List<Artist> = emptyList(),
        /** A server search is in flight. */
        val searching: Boolean = false,
        /** The server search failed. */
        val error: String? = null
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

/** Searches the current [MusicSource]: the device library in memory, or the server's search3. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(sourceStore: MusicSourceStore, mediaRepository: MediaRepository, subsonic: SubsonicRepository) : ViewModel() {
    val query = MutableStateFlow("")

    private val device: Flow<SearchResults> = query.debounce(200).flatMapLatest { q ->
        if (q.isBlank()) flowOf(SearchResults())
        else mediaRepository.observeSongs().mapLatest { all ->
            SearchResults(
                    songs = all.filter { it.title.contains(q, true) },
                    albums = songsToAlbums(all).filter { it.title.contains(q, true) },
                    artists = songsToArtists(all).filter { it.name.contains(q, true) })
        }
    }

    private val server: Flow<SearchResults> = query.debounce(400).transformLatest { q ->
        if (q.isBlank()) {
            emit(SearchResults())
            return@transformLatest
        }
        emit(SearchResults(searching = true))
        emit(try {
            subsonic.search(q).let { SearchResults(it.songs, it.albums, it.artists) }
        } catch (e: SubsonicException) {
            SearchResults(error = e.message ?: "Couldn't reach the server")
        })
    }

    val results: StateFlow<SearchResults> = (if (sourceStore.isServer) server else device).stateIn(this, SearchResults())
}
