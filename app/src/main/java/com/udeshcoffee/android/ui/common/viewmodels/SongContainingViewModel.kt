package com.udeshcoffee.android.ui.common.viewmodels

import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.playback.PlaybackEvents
import com.udeshcoffee.android.extensions.getPlayer
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.udeshcoffee.android.extensions.playbackEvents
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.shuffle
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.SingleLiveEvent
import io.reactivex.disposables.Disposable

abstract class SongContainingViewModel(
        application: Application
) : AndroidViewModel(application) {

    private var broadcastDisposable: Disposable? = null
    val currentSongId = MutableLiveData<Long>()

    val songs = MutableLiveData<List<Song>>()

    // Events
    val showSongLongDialog = SingleLiveEvent<Song>()

    open fun start() {
        fetchSongs()

        broadcastDisposable = playbackEvents()
                .filter { it == PlaybackEvents.METADATA_CHANGED || it == PlaybackEvents.SERVICE_CONNECTED }
                .subscribe { getPlayer()?.let { player -> onMetaDataChanged(player) } }
        currentSongId.value = getPlayer()?.currentSong()?.id ?: -1
    }

    open fun stop() {
        disposeSongs()

        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    open fun onMetaDataChanged(service: PlayerConnection) {
        currentSongId.value = service.currentSong()?.id ?: -1
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