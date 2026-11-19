package com.udeshcoffee.android.ui.compose.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.components.SortSettings
import com.udeshcoffee.android.ui.compose.components.sortedAlbums
import com.udeshcoffee.android.ui.compose.components.sortedArtists
import com.udeshcoffee.android.ui.compose.components.sortedSongs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Library tabs. Lists are null until first loaded (to tell "loading" from "empty"). */
class LibraryViewModel(mediaRepository: MediaRepository) : ViewModel() {

    private fun <T> Flow<T>.state(): StateFlow<T?> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<List<Song>?> =
            combine(mediaRepository.observeSongs(), SortSettings.songs.state) { s, sort -> s.sortedSongs(sort) }.state()

    val albums: StateFlow<List<Album>?> =
            combine(mediaRepository.observeAlbums(), SortSettings.albums.state) { a, sort -> a.sortedAlbums(sort) }.state()

    val artists: StateFlow<List<Artist>?> =
            combine(mediaRepository.observeArtists(), SortSettings.artists.state) { a, sort -> a.sortedArtists(sort) }.state()

    val genres: StateFlow<List<Genre>?> = mediaRepository.observeGenres().state()
}
