package com.udeshcoffee.android.ui.common.viewmodels

import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.playback.PlaybackEvents
import com.udeshcoffee.android.extensions.getPlayer
import com.udeshcoffee.android.extensions.playbackEvents
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import android.util.Log
import com.udeshcoffee.android.model.Song
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class BasePlayerViewModel(application: Application): AndroidViewModel(application) {

    val currentSong = MutableLiveData<Song?>()
    val duration = MutableLiveData<Long>()
    var isPlaying = MutableLiveData<Boolean>()

    private var eventsJob: Job? = null
    private var isLoaded: Boolean = false

    init {
        isPlaying.value = false
    }

    open fun start() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            playbackEvents().collect {
                    when (it) {
                        PlaybackEvents.METADATA_CHANGED -> {
                            getPlayer()?.let { onMetadataChange(it) }
                        }
                        PlaybackEvents.PLAYBACK_STATE_CHANGED -> {
                            getPlayer()?.let { onPlaybackChange(it) }
                        }
                        PlaybackEvents.SERVICE_CONNECTED -> {
                            if (!isLoaded) {
                                getPlayer()?.let { onInitMetadata(it) }
                                isLoaded = true
                            }
                        }
                    }
            }
        }
        val service = getPlayer()
        if (service != null) {
            onInitMetadata(service)
            isLoaded = true
        }
    }

    open fun stop() {
        eventsJob?.cancel()
    }

    fun playPauseToggle() {
        val player = getPlayer() ?: return
        if (player.isPlaying()) player.pause() else player.play()
    }

    open fun onInitMetadata(service: PlayerConnection) {
        currentSong.value = service.currentSong()
        duration.value = service.duration
    }

    open fun onMetadataChange(service: PlayerConnection) {
        Log.d("BasePlayerViewModel", "Metadata changed")
        currentSong.value = service.currentSong()
        duration.value = service.duration
    }

    open fun onPlaybackChange(service: PlayerConnection) {
        Log.d("BasePlayerViewModel", "Playback changed")
        isPlaying.value = service.isPlaying()
    }

    abstract fun setProgress()

    abstract fun disposeProgress()
}