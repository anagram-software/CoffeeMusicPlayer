package com.udeshcoffee.android.ui.player.queue

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.common.viewmodels.SongContainingViewModel
import com.udeshcoffee.android.utils.PreferenceUtil
import com.udeshcoffee.android.utils.SingleLiveEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 9/16/2017.
 */
class QueueViewModel(
        application: Application,
        private val sharedPreferences: SharedPreferences
) : SongContainingViewModel(application) {

    val repeatMode = MutableLiveData<Int>()
    val isShuffle = MutableLiveData<Boolean>()
    var total = 0
    val playPosition = MutableLiveData<Int>()
    val showAddToPlaylist = SingleLiveEvent<List<Song>>()
    val showPlayerUI = SingleLiveEvent<Void>()
    val hideNowPlay = SingleLiveEvent<Void>()
    val scrollTo = SingleLiveEvent<Int>()

    private var queueDisposable: Disposable? = null
    private var loadFirstTime = true

    override fun start() {
        loadFirstTime = true
        repeatMode.value = sharedPreferences.getInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.ALL)
        isShuffle.value = sharedPreferences.getBoolean(PreferenceUtil.SHUFFLE, false)
        super.start()
    }

    override fun fetchSongs() {
        Log.d(TAG, "loadQueue")
        disposeSongs()
        getService()?.let {
            queueDisposable = it.getQueueObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe { it1 ->
                        Log.d(TAG, "loadQueue: ${it1.size}")
                        songs.value = it1
                        if (loadFirstTime) {
                            scrollTo.value = it.playPosition
                            loadFirstTime = false
                        }
                    }
            total = it.list.size
            playPosition.value = it.playPosition
        }
    }

    override fun onMetaDataChanged(service: MusicService) {
        super.onMetaDataChanged(service)
        total = service.list.size
        playPosition.value = service.playPosition
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
                    .subscribe({ it1 ->
                        showAddToPlaylist.value = it1
                    }, {})
        }
    }

    fun changeRepeatMode() {
        when(repeatMode.value) {
            MusicService.RepeatMode.NONE -> {
                sharedPreferences.edit { putInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.ALL).apply() }
                repeatMode.value = MusicService.RepeatMode.ALL
            }
            MusicService.RepeatMode.ALL -> {
                sharedPreferences.edit { putInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.ONE).apply() }
                repeatMode.value = MusicService.RepeatMode.ONE
            }
            MusicService.RepeatMode.ONE -> {
                sharedPreferences.edit { putInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.NONE).apply() }
                repeatMode.value = MusicService.RepeatMode.NONE
            }
        }
    }

    fun shuffle() {
        isShuffle.value = !isShuffle.value!!
        sharedPreferences.edit().putBoolean(PreferenceUtil.SHUFFLE, isShuffle.value!!).apply()
    }

    fun itemMoved(fromPosition: Int, toPosition: Int) {
        Log.d("Queue", "itemMoved $fromPosition $toPosition")
        val service = getService()
        service?.moveInQueue(fromPosition, toPosition)
        service?.playPosition?.let { currentPos ->
            when {
                (currentPos in (fromPosition + 1) until (toPosition + 1)) -> {
                    setCurrentPosition(currentPos - 1)
                }
                (currentPos in toPosition until fromPosition) -> {
                    setCurrentPosition(currentPos + 1)
                }
                fromPosition == currentPos -> {
                    setCurrentPosition(toPosition)
                }
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
                val currentPos = it.playPosition
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