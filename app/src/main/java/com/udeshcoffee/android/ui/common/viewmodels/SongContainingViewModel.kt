package com.udeshcoffee.android.ui.common.viewmodels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.shuffle
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.utils.SingleLiveEvent
import io.reactivex.disposables.Disposable

abstract class SongContainingViewModel (
        application: Application
): AndroidViewModel(application) {

    private var broadcastDisposable: Disposable? = null
    val currentSongId = MutableLiveData<Long>()

    val songs = MutableLiveData<List<Song>>()

    // Events
    val showSongLongDialog = SingleLiveEvent<Song>()

    open fun start() {
        fetchSongs()

        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        val service = getService()
        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            currentSongId.value = service?.currentSong()?.id ?: -1
                        }
                    }
                }
        currentSongId.value = service?.currentSong()?.id ?: -1
    }

    open fun stop() {
        disposeSongs()

        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    abstract fun fetchSongs()

    abstract fun disposeSongs()

    fun songItemClicked(position: Int) {
        songs.value?.let { playSong(position, it, true) }
    }

    fun songItemLongClicked(position: Int) {
        songs.value?.get(position)?.let { showSongLongDialog.value = it }
    }

    fun shuffleClicked() {
        songs.value?.let { shuffle(it) }
    }

    abstract var songSortOrder: Int

    abstract var songSortAscending: Boolean

}