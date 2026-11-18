package com.udeshcoffee.android.ui.main.search

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.data.media.songsToArtists
import com.udeshcoffee.android.data.media.songsToAlbums
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.lifecycle.viewModelScope
import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongAlbumViewModel
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.SortManager
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Udathari on 9/12/2017.
 */
class SearchViewModel(
        application: Application,
        dataRepository: DataRepository,
        mediaRepository: MediaRepository
): DetailSongAlbumViewModel(application, dataRepository, mediaRepository) {

    val artists = MutableLiveData<List<Artist>>()

    val showArtist = SingleLiveEvent<Artist>()

    // Redundant
    override fun fetchSongs() {}
    override fun disposeSongs() {}
    override fun fetchAlbums() {}
    override fun disposeAlbums() {}

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        if (query == "") {
            songs.value = ArrayList()
            albums.value = ArrayList()
            artists.value = ArrayList()
            return
        }
        searchJob = viewModelScope.launch {
            val allSongs = mediaRepository.loadSongs()
            songs.value = allSongs.filter { it.title.contains(query, true) }
            albums.value = songsToAlbums(allSongs).filter { it.title.contains(query, true) }
            artists.value = songsToArtists(allSongs).filter { it.name.contains(query, true) }
        }
    }

    fun artistItemClicked(position: Int) {
        artists.value?.get(position)?.let { showArtist.value = it }
    }

    private suspend fun sortedArtistSongs(artist: Artist): List<Song> =
            mediaRepository.loadArtistSongs(artist.id).sortedWith(SortManager::sortArtistSongs, SortManager.artistSongsAscending)

    fun artistItemLongClicked(position: Int) {
        val artist = artists.value?.getOrNull(position) ?: return
        viewModelScope.launch { showCollectionLongDialog.value = artist.name to sortedArtistSongs(artist) }
    }

    fun artistItemOptionClicked(position: Int) {
        val artist = artists.value?.getOrNull(position) ?: return
        viewModelScope.launch { playSong(0, sortedArtistSongs(artist), true) }
    }

    // Redundant
    override var albumSortOrder: Int = 0
    override var albumSortAscending: Boolean = false
    override var songSortOrder: Int = 0
    override var songSortAscending: Boolean = false

    companion object {
        val TAG = "SearchPresenter"
    }

}