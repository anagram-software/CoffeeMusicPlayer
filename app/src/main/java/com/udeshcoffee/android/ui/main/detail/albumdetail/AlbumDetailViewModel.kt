package com.udeshcoffee.android.ui.main.detail.albumdetail

import android.app.Application
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongViewModel
import com.udeshcoffee.android.utils.SortManager
import java.util.*

class AlbumDetailViewModel(
        application: Application,
        dataRepository: DataRepository,
        private val mediaRepository: MediaRepository
): DetailSongViewModel(application, dataRepository) {

    var albumId: Long = -1

    override fun fetchSongs() {
        collectSongs(mediaRepository.observeAlbumSongs(albumId), SortManager::sortAlbumSongs) { songSortAscending }
    }

    override var songSortOrder: Int
        get() = SortManager.albumSongsSortOrder
        set(value) {
            SortManager.albumSongsSortOrder = value}

    override var songSortAscending: Boolean
        get() = SortManager.albumSongsAscending
        set(value) {
            SortManager.albumSongsAscending = value}
}