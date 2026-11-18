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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

abstract class SongContainingViewModel(
        application: Application
) : AndroidViewModel(application) {

    private var eventsJob: Job? = null
    private var songsJob: Job? = null
    val currentSongId = MutableLiveData<Long>()

    val songs = MutableLiveData<List<Song>>()

    // Events
    val showSongLongDialog = SingleLiveEvent<Song>()

    open fun start() {
        fetchSongs()

        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            playbackEvents()
                    .filter { it == PlaybackEvents.METADATA_CHANGED || it == PlaybackEvents.SERVICE_CONNECTED }
                    .collect { getPlayer()?.let { player -> onMetaDataChanged(player) } }
        }
        currentSongId.value = getPlayer()?.currentSong()?.id ?: -1
    }

    open fun stop() {
        disposeSongs()

        eventsJob?.cancel()
    }

    open fun onMetaDataChanged(service: PlayerConnection) {
        currentSongId.value = service.currentSong()?.id ?: -1
    }

    abstract fun fetchSongs()

    /** Stops the collection started by [collectSongs]. Override if a subclass loads songs differently. */
    open fun disposeSongs() {
        songsJob?.cancel()
    }

    /** Collects [flow] into [songs], sorted with [sort] and reversed unless ascending, until stopped. */
    protected fun collectSongs(flow: Flow<List<Song>>, sort: (List<Song>) -> Unit, ascending: () -> Boolean) {
        songsJob?.cancel()
        songsJob = viewModelScope.launch {
            flow.collect { songs.value = it.sortedWith(sort, ascending()) }
        }
    }

    /** A sorted copy (SortManager sorts in place). */
    protected fun <T> List<T>.sortedWith(sort: (List<T>) -> Unit, ascending: Boolean): List<T> =
            toMutableList().also { list ->
                sort(list)
                if (!ascending) list.reverse()
            }

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