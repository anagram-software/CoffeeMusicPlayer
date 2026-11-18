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

private fun <T> Flow<T>.stateIn(vm: ViewModel, initial: T): StateFlow<T> =
        stateIn(vm.viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

class AlbumDetailViewModel(albumId: Long, mediaRepository: MediaRepository) : ViewModel() {
    val songs: StateFlow<List<Song>?> =
            combine(mediaRepository.observeAlbumSongs(albumId), SortSettings.albumSongs.state) { s, sort -> s.sortedSongs(sort) }
                    .stateIn(this, null)
}

class ArtistDetailViewModel(
        private val artistId: Long,
        artistName: String,
        mediaRepository: MediaRepository,
        dataRepository: DataRepository
) : ViewModel() {
    val songs: StateFlow<List<Song>?> =
            combine(mediaRepository.observeArtistSongs(artistId), SortSettings.artistSongs.state) { s, sort -> s.sortedSongs(sort) }
                    .stateIn(this, null)

    val albums: StateFlow<List<Album>> =
            combine(mediaRepository.observeArtistAlbums(artistName), SortSettings.artistAlbums.state) { a, sort -> a.sortedAlbums(sort) }
                    .stateIn(this, emptyList())

    private val _bio = MutableStateFlow<Pair<String, List<String>>?>(null)
    /** Last.fm bio (may contain links) and tags; null until loaded or when unavailable. */
    val bio: StateFlow<Pair<String, List<String>>?> = _bio.asStateFlow()

    /** Bumped when the artist image changes so the UI reloads it. */
    val imageVersion = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            dataRepository.getBio(artistId, artistName)?.let { (bio, tags) -> _bio.value = bio to tags.orEmpty().toList() }
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

class GenreDetailViewModel(genreId: Long, mediaRepository: MediaRepository) : ViewModel() {
    val songs: StateFlow<List<Song>?> =
            combine(mediaRepository.observeGenreSongs(genreId), SortSettings.genreSongs.state) { s, sort -> s.sortedSongs(sort) }
                    .stateIn(this, null)

    val albums: StateFlow<List<Album>> =
            combine(mediaRepository.observeGenreAlbums(genreId), SortSettings.genreAlbums.state) { a, sort -> a.sortedAlbums(sort) }
                    .stateIn(this, emptyList())
}

class PlaylistDetailViewModel(
        val playlistId: Long,
        val type: Int,
        private val mediaRepository: MediaRepository
) : ViewModel() {
    /** Playlists keep their own order. */
    val songs: StateFlow<List<Song>?> = mediaRepository.observePlaylistSongs(playlistId, type).stateIn(this, null)

    fun move(from: Int, to: Int) = viewModelScope.launch { mediaRepository.movePlaylistItem(playlistId, from, to) }
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

class FavoritesViewModel(mediaRepository: MediaRepository) : ViewModel() {
    val songs: StateFlow<List<Song>?> =
            combine(mediaRepository.observeFavorites(), SortSettings.favoriteSongs.state) { s, sort -> s.sortedSongs(sort) }
                    .stateIn(this, null)
}

class PlaylistsViewModel(mediaRepository: MediaRepository) : ViewModel() {
    val playlists: StateFlow<List<Playlist>?> = mediaRepository.observePlaylists().stateIn(this, null)
}

data class SearchResults(val songs: List<Song> = emptyList(), val albums: List<Album> = emptyList(), val artists: List<Artist> = emptyList())

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(mediaRepository: MediaRepository) : ViewModel() {
    val query = MutableStateFlow("")

    val results: StateFlow<SearchResults> = query.debounce(200).flatMapLatest { q ->
        if (q.isBlank()) flowOf(SearchResults())
        else mediaRepository.observeSongs().mapLatest { all ->
            SearchResults(
                    songs = all.filter { it.title.contains(q, true) },
                    albums = songsToAlbums(all).filter { it.title.contains(q, true) },
                    artists = songsToArtists(all).filter { it.name.contains(q, true) })
        }
    }.stateIn(this, SearchResults())
}
