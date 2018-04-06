package com.udeshcoffee.android.ui.main.library.nested.track

import android.app.Application
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.SongContainingViewModel
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*

/**
* Created by Udathari on 9/5/2017.
*/
class TrackViewModel(
        application: Application,
        private val mediaRepository: MediaRepository
): SongContainingViewModel(application) {

    private var disposable: Disposable? = null

    override fun fetchSongs() {
        disposeSongs()
        disposable = mediaRepository.getSongs()
                .observeOn(AndroidSchedulers.mainThread())
                .map({ songs ->
                    SortManager.sortSongs(songs)

                    if (!songSortAscending) {
                        Collections.reverse(songs)
                    }

                    return@map songs
                })
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
        get() = SortManager.songsSortOrder
        set(value) {SortManager.songsSortOrder = value}

    override var songSortAscending: Boolean
        get() = SortManager.songsAscending
        set(value) {SortManager.songsAscending = value}

//    companion object {
//        private const val TAG = "TrackPresenter"
//    }
}