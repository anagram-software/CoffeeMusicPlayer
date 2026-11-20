@file:OptIn(ExperimentalCoroutinesApi::class)

package com.udeshcoffee.android.ui.compose.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.RecentItem
import com.udeshcoffee.android.data.mergeRecent
import com.udeshcoffee.android.data.observeRecentlyPlayed
import com.udeshcoffee.android.utils.PlaylistPlays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import com.udeshcoffee.android.data.media.songsToAlbums
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.data.subsonic.ServerAlbumSort
import com.udeshcoffee.android.data.subsonic.SubsonicException
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The curated Home sections. Albums are newest / most recent / most played first; recently played mixes in playlists. */
data class HomeContent(
        val recentlyAdded: List<Album> = emptyList(),
        val recentlyPlayed: List<RecentItem> = emptyList(),
        val mostPlayed: List<Album> = emptyList(),
        val playlists: List<Playlist> = emptyList(),
        /** The server couldn't be reached (server source only). */
        val error: String? = null
) {
    val isEmpty: Boolean get() = recentlyAdded.isEmpty() && recentlyPlayed.isEmpty() && mostPlayed.isEmpty() && playlists.isEmpty()
}

/**
 * Home for the current music source. Device: built from MediaStore dates and the app's play stats
 * (albums of the recently added / played / most played songs). Server: getAlbumList2 (newest,
 * recent, frequent), which the server keeps from its own play counts. Recently played also has the
 * playlists played in this app ([PlaylistPlays]).
 */
class HomeViewModel(sourceStore: MusicSourceStore, mediaRepository: MediaRepository, private val subsonic: SubsonicRepository) : ViewModel() {

    val isServer = sourceStore.isServer

    private val reload = MutableStateFlow(0)

    fun retry() { reload.value++ }

    init {
        viewModelScope.launch(Dispatchers.IO) { PlaylistPlays.load() }
    }

    /** Null while loading. */
    val content: StateFlow<HomeContent?> = (if (isServer) serverContent() else deviceContent(mediaRepository))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun deviceContent(mediaRepository: MediaRepository): Flow<HomeContent> {
        fun albumsOf(type: Int) = mediaRepository.observePlaylistSongs(0, type).map { albums(it) }
        return combine(
                albumsOf(Playlist.RECENTLY_ADDED),
                mediaRepository.observeRecentlyPlayed(SECTION_SIZE),
                albumsOf(Playlist.MOST_PLAYED),
                mediaRepository.observePlaylists().map { all -> all.filter { it.type == Playlist.USER } }
        ) { added, played, most, playlists -> HomeContent(added, played, most, playlists) }
    }

    /** What the server returns, before the playlists played in the app are merged into recently played. */
    private class ServerHome(val added: List<Album>, val played: List<RecentItem.OfAlbum>, val most: List<Album>, val playlists: List<Playlist>)

    private fun serverContent(): Flow<HomeContent> = combine(reload, subsonic.playlistsVersion) { a, b -> a to b }.flatMapLatest {
        flow {
            val home = try {
                coroutineScope {
                    val added = async { subsonic.albums(ServerAlbumSort.NEWEST, offset = 0, count = SECTION_SIZE) }
                    val played = async { subsonic.recentAlbums(SECTION_SIZE) }
                    val most = async { subsonic.albums(ServerAlbumSort.FREQUENT, offset = 0, count = SECTION_SIZE) }
                    val playlists = async { subsonic.playlists() }
                    ServerHome(added.await(), played.await(), most.await(), playlists.await())
                }
            } catch (e: SubsonicException) {
                emit(HomeContent(error = e.message ?: "Couldn't reach the server"))
                return@flow
            }
            // Downloads can be played from too, though it isn't one of the server's playlists.
            val playable = listOf(Playlist(Playlist.DOWNLOADS)) + home.playlists
            emitAll(PlaylistPlays.plays.filterNotNull().map { plays ->
                HomeContent(home.added, mergeRecent(home.played, playable, plays, SECTION_SIZE), home.most, home.playlists)
            })
        }
    }

    private fun albums(songs: List<Song>): List<Album> = songsToAlbums(songs).take(SECTION_SIZE)

    companion object {
        const val SECTION_SIZE = 20
    }
}
