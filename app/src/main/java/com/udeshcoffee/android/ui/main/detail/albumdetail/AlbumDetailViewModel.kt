package com.udeshcoffee.android.ui.main.detail.albumdetail

import android.app.Application
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongViewModel
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class AlbumDetailViewModel(
        application: Application,
        dataRepository: DataRepository,
        private val mediaRepository: MediaRepository
): DetailSongViewModel(application, dataRepository) {
    private var disposable: Disposable? = null

    var albumId: Long = -1

    override fun fetchSongs() {
        disposeSongs()
        disposable = mediaRepository.getAlbumSongs(albumId)
                .map({ songs ->
                    SortManager.sortAlbumSongs(songs)

                    if (!songSortAscending) {
                        Collections.reverse(songs)
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
        get() = SortManager.albumSongsSortOrder
        set(value) {
            SortManager.albumSongsSortOrder = value}

    override var songSortAscending: Boolean
        get() = SortManager.albumSongsAscending
        set(value) {
            SortManager.albumSongsAscending = value}
}