package com.udeshcoffee.android.ui.player.queue

import android.app.Application
import android.util.Log
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.common.viewmodels.SongContainingViewModel
import com.udeshcoffee.android.utils.SingleLiveEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 9/16/2017.
 */
class QueueViewModel(application: Application): SongContainingViewModel(application) {

    val showAddToPlaylist = SingleLiveEvent<List<Song>>()
    val showPlayerUI = SingleLiveEvent<Void>()
    val hideNowPlay = SingleLiveEvent<Void>()
    val scrollTo = SingleLiveEvent<Int>()

    private var queueDisposable: Disposable? = null
    private var loadFirstTime = true

    override fun start() {
        loadFirstTime = true
        super.start()
    }

    override fun fetchSongs() {
        Log.d(Companion.TAG, "loadQueue")
        disposeSongs()
        getService()?.let {
            queueDisposable = it.getQueueObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe { it1 ->
                        Log.d(Companion.TAG, "loadQueue: ${it1.size}")
                        songs.value = it1
                        if (loadFirstTime) {
                            scrollTo.value = it.playPosition
                            loadFirstTime = false
                        }
                    }
        }
    }

    override fun disposeSongs() {
        queueDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun closeQueue() {
        showPlayerUI.call()
    }

    fun addToPlaylist() {
        getService()?.let {
            it.getQueueObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .firstOrError()
                    .subscribe ({ it1 ->
                        showAddToPlaylist.value = it1
                    }, {})
        }
    }

    fun itemMoved(fromPosition: Int, toPosition: Int) {
        val service = getService()
        service?.playPosition?.let { currentPos ->
            when {
                (currentPos in (fromPosition + 1)..toPosition) -> setCurrentPosition(currentPos - 1)
                (currentPos in toPosition..(fromPosition - 1)) -> setCurrentPosition(currentPos + 1)
                fromPosition == currentPos -> setCurrentPosition(toPosition)
                else -> setCurrentPosition(currentPos)
            }
        }
    }

    private fun setCurrentPosition(position: Int) {
        getService()?.playPosition = position
    }

    fun queueItemClicked(position: Int) {
        getService()?.initSong(position, true)
    }

    fun itemRemoved(position: Int) {
        getService()?.let {
            it.list.remove(position)
            if (it.list.size >= 1) {
                val currentPos =  it.playPosition
                if (position == currentPos) {
                    it.initSong(position, true)
                } else if (position < it.playPosition) {
                    it.playPosition = currentPos - 1
                }
            } else {
                clearQueue()
            }
        }
    }

    fun clearQueue() {
        getService()?.clearSongList()
        hideNowPlay.call()
        closeQueue()
    }

    // Redundant
    override var songSortOrder: Int = 0
    override var songSortAscending: Boolean = false

    companion object {
        private const val TAG = "QueuePresenter"
    }

}