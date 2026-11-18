package com.udeshcoffee.android.ui.main.detail.playlistdetail

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import android.app.Application
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongViewModel

/**
 * Created by Udathari on 9/12/2017.
 */

class PlaylistDetailViewModel(
        application: Application,
        dataRepository: DataRepository,
        private val mediaRepository: MediaRepository
): DetailSongViewModel(application, dataRepository) {

    var playlistId: Long = -1
    var playlistType: Int = -1
    var playlistTitle: String = ""


    override fun fetchSongs() {
        // Playlists keep their own order.
        collectSongs(mediaRepository.observePlaylistSongs(playlistId, playlistType), {}) { true }
    }

    fun itemMoved(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch { mediaRepository.movePlaylistItem(playlistId, fromPosition, toPosition) }
    }

    fun itemRemoved(id: Long) {
        viewModelScope.launch { mediaRepository.removePlaylistItem(playlistId, id) }
    }

    // Redundant
    override var songSortOrder: Int = 0
    override var songSortAscending: Boolean = false

}