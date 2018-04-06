package com.udeshcoffee.android.ui.main.detail.playlistdetail

import android.app.Application
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

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

    private var disposable: Disposable? = null

    override fun fetchSongs() {
        disposeSongs()
        disposable = mediaRepository.getPlaylistSongs(playlistId, playlistType)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe{
                    songs.value = it
                }
    }

    override fun disposeSongs() {
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun itemMoved(fromPosition: Int, toPosition: Int) {
        mediaRepository.movePlaylistItem(playlistId, fromPosition, toPosition)
    }

    fun itemRemoved(id: Long) {
        mediaRepository.removePlayistItem(playlistId, id)
    }

    // Redundant
    override var songSortOrder: Int = 0
    override var songSortAscending: Boolean = false


}