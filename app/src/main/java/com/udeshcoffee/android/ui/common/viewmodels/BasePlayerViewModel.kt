package com.udeshcoffee.android.ui.common.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import android.content.IntentFilter
import android.util.Log
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import io.reactivex.disposables.Disposable

abstract class BasePlayerViewModel(application: Application): AndroidViewModel(application) {

    val currentSong = MutableLiveData<Song>()
    val duration = MutableLiveData<Long>()
    var isPlaying = MutableLiveData<Boolean>()

    private var broadcastDisposable: Disposable? = null
    private var isLoaded: Boolean = false

    init {
        isPlaying.value = false
    }

    open fun start() {
        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)
        filter.addAction(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED)
        filter.addAction(MusicService.InternalIntents.SERVICE_CONNECTED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when (it.action) {
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            getService()?.let { onMetadataChange(it) }
                        }
                        MusicService.InternalIntents.PLAYBACK_STATE_CHANGED -> {
                            getService()?.let { onPlaybackChange(it) }
                        }
                        MusicService.InternalIntents.SERVICE_CONNECTED -> {
                            if (!isLoaded) {
                                getService()?.let { onInitMetadata(it) }
                                isLoaded = true
                            }
                        }
                    }
                }
        val service = getService()
        if (service != null) {
            onInitMetadata(service)
            isLoaded = true
        }
    }

    open fun stop() {
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun playPauseToggle() {
        val service = getService()
        if (isPlaying.value!!)
            service?.pause()
        else
            service?.play()
    }

    open fun onInitMetadata(service: MusicService) {
        currentSong.value = service.currentSong()
        duration.value = service.duration
    }

    open fun onMetadataChange(service: MusicService) {
        Log.d("BasePlayerViewModel", "Metadata changed")
        currentSong.value = service.currentSong()
        duration.value = service.duration
    }

    open fun onPlaybackChange(service: MusicService) {
        Log.d("BasePlayerViewModel", "Playback changed")
        isPlaying.value = service.isPlaying()
    }

    abstract fun setProgress()

    abstract fun disposeProgress()
}