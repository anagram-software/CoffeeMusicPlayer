package com.udeshcoffee.android.ui.main.favorites

import android.app.Application
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongViewModel
import com.udeshcoffee.android.ui.common.viewmodels.SongContainingViewModel
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
* Created by Udathari on 9/29/2017.
*/
class FavoritesViewModel(
        application: Application,
        dataRepository: DataRepository,
        private val mediaRepository: MediaRepository
): DetailSongViewModel(application, dataRepository) {

    var disposable: Disposable? = null

    override fun fetchSongs() {
        disposeSongs()
        disposable = mediaRepository.getFavorites()
                .map({ songs ->
                    com.udeshcoffee.android.utils.SortManager.sortFavoritesSongs(songs)

                    if (!songSortAscending) {
                        java.util.Collections.reverse(songs)
                    }

                    return@map songs
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    songs.value = it
                }
    }

    override fun disposeSongs() {
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
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