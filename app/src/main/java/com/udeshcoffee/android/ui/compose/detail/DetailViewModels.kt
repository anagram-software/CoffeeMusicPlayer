@file:OptIn(ExperimentalCoroutinesApi::class)

package com.udeshcoffee.android.ui.compose.detail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.media.songsToAlbums
import com.udeshcoffee.android.data.media.songsToArtists
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.components.SortSettings
import com.udeshcoffee.android.ui.compose.components.sortedAlbums
import com.udeshcoffee.android.ui.compose.components.sortedSongs
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
import com.udeshcoffee.android.data.subsonic.ServerSearchResults
import com.udeshcoffee.android.data.subsonic.SubsonicException
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

class AlbumDetailViewModel(albumId: Long, remoteId: String?, mediaRepository: MediaRepository, subsonic: SubsonicRepository) : SourceAwareViewModel() {
    private val source: Flow<List<Song>?> =
            if (remoteId != null) server { subsonic.albumSongs(remoteId) } else mediaRepository.observeAlbumSongs(albumId)

    val songs: StateFlow<List<Song>?> =
            combine(source, SortSettings.albumSongs.state) { s, sort -> s?.sortedSongs(sort) }.stateIn(this, null)
}

class ArtistDetailViewModel(
        private val artistId: Long,
        artistName: String,
        private val remoteId: String?,
        mediaRepository: MediaRepository,
        dataRepository: DataRepository,
        subsonic: SubsonicRepository
) : SourceAwareViewModel() {
    val isRemote = remoteId != null

    private val songSource: Flow<List<Song>?> =
            if (remoteId != null) server { subsonic.artistSongs(remoteId) } else mediaRepository.observeArtistSongs(artistId)

    val songs: StateFlow<List<Song>?> =
            combine(songSource, SortSettings.artistSongs.state) { s, sort -> s?.sortedSongs(sort) }.stateIn(this, null)

    private val albumSource: Flow<List<Album>> =
            if (remoteId != null) server { subsonic.artistAlbums(remoteId) }.map { it.orEmpty() }
            else mediaRepository.observeArtistAlbums(artistName)

    val albums: StateFlow<List<Album>> =
            combine(albumSource, SortSettings.artistAlbums.state) { a, sort -> a.sortedAlbums(sort) }.stateIn(this, emptyList())

    private val _bio = MutableStateFlow<Pair<String, List<String>>?>(null)
    /** Bio (may contain links) and tags: from the server for server artists, else Last.fm. Null until loaded or when unavailable. */
    val bio: StateFlow<Pair<String, List<String>>?> = _bio.asStateFlow()

    /** Bumped when the artist image changes so the UI reloads it. */
    val imageVersion = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            _bio.value = if (remoteId != null) {
                runCatching { subsonic.artistInfo(remoteId).biography }.getOrNull()?.let { it to emptyList() }
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
        private val subsonic: SubsonicRepository
) : SourceAwareViewModel() {

    /** Server playlists reload after edits. Their entries carry their position as [Song.playlistSongId]. */
    private val remoteSongs = MutableStateFlow<List<Song>?>(null)

    /** Playlists keep their own order. */
    val songs: StateFlow<List<Song>?> = (if (remoteId != null) remoteSongs else mediaRepository.observePlaylistSongs(playlistId, type))
            .stateIn(this, null)

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

/** Favorites: device songs favorited in the app plus songs starred on the music server. */
class FavoritesViewModel(mediaRepository: MediaRepository, subsonic: SubsonicRepository) : SourceAwareViewModel() {
    private val serverFavorites: Flow<List<Song>> = subsonic.account.flatMapLatest { account ->
        if (account == null) flowOf(emptyList())
        // Reload when stars change elsewhere (e.g. from the player).
        else combine(reload, subsonic.starredIds.map { it.size }.distinctUntilChanged()) { _, _ -> }.mapLatest {
            try { subsonic.loadStarred() } catch (e: SubsonicException) { _error.value = e.message; emptyList() }
        }.distinctUntilChanged()
    }

    val songs: StateFlow<List<Song>?> =
            combine(mediaRepository.observeFavorites(), serverFavorites, SortSettings.favoriteSongs.state) { local, remote, sort ->
                (local + remote).sortedSongs(sort)
            }.stateIn(this, null)
}

/** Device playlists, and the music server's (null while loading or signed out). */
class PlaylistsViewModel(mediaRepository: MediaRepository, private val subsonic: SubsonicRepository) : SourceAwareViewModel() {
    val playlists: StateFlow<List<Playlist>?> = mediaRepository.observePlaylists().stateIn(this, null)

    val account = subsonic.account

    val serverPlaylists: StateFlow<List<Playlist>?> = subsonic.account.flatMapLatest { account ->
        if (account == null) flowOf<List<Playlist>?>(null)
        else combine(subsonic.playlistsVersion, reload) { a, b -> a to b }.flatMapLatest {
            flow<List<Playlist>?> {
                _error.value = null
                emit(try { subsonic.playlists() } catch (e: SubsonicException) { _error.value = e.message; emptyList() })
            }
        }
    }.stateIn(this, null)
}

data class SearchResults(
        val songs: List<Song> = emptyList(),
        val albums: List<Album> = emptyList(),
        val artists: List<Artist> = emptyList(),
        /** Music server results; null when signed out, or while searching. */
        val server: ServerSearchResults? = null,
        val serverError: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(mediaRepository: MediaRepository, private val subsonic: SubsonicRepository) : ViewModel() {
    val query = MutableStateFlow("")

    private val local: Flow<SearchResults> = query.debounce(200).flatMapLatest { q ->
        if (q.isBlank()) flowOf(SearchResults())
        else mediaRepository.observeSongs().mapLatest { all ->
            SearchResults(
                    songs = all.filter { it.title.contains(q, true) },
                    albums = songsToAlbums(all).filter { it.title.contains(q, true) },
                    artists = songsToArtists(all).filter { it.name.contains(q, true) })
        }
    }

    private val remote: Flow<Pair<ServerSearchResults?, String?>> = combine(query.debounce(400), subsonic.account) { q, account -> q to account }
            .mapLatest { (q, account) ->
                if (q.isBlank() || account == null) null to null
                else try { subsonic.search(q) to null } catch (e: SubsonicException) { null to e.message }
            }

    val results: StateFlow<SearchResults> = combine(local, remote) { l, (server, error) -> l.copy(server = server, serverError = error) }
            .stateIn(this, SearchResults())
}
