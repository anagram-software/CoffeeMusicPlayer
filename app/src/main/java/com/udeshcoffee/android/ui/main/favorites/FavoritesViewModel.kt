package com.udeshcoffee.android.ui.main.favorites

import android.app.Application
import android.util.Log
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongViewModel
import com.udeshcoffee.android.ui.common.viewmodels.SongContainingViewModel
import com.udeshcoffee.android.utils.SortManager

/**
* Created by Udathari on 9/29/2017.
*/
class FavoritesViewModel(
        application: Application,
        dataRepository: DataRepository,
        private val mediaRepository: MediaRepository
): DetailSongViewModel(application, dataRepository) {


    override fun fetchSongs() {
        collectSongs(mediaRepository.observeFavorites(), SortManager::sortFavoritesSongs) { songSortAscending }
    }

    override var songSortOrder: Int
        get() = SortManager.favoritesSongsSortOrder
        set(value) {
            SortManager.favoritesSongsSortOrder = value}

    override var songSortAscending: Boolean
        get() = SortManager.favoritesSongsAscending
        set(value) {
            SortManager.favoritesSongsAscending = value}

}