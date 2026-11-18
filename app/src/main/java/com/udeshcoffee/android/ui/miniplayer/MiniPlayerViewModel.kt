package com.udeshcoffee.android.ui.miniplayer

import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.extensions.getPlayer
import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.udeshcoffee.android.ui.common.viewmodels.BasePlayerViewModel

/**
 * Created by Udathari on 8/25/2017.
 */
class MiniPlayerViewModel(application: Application): BasePlayerViewModel(application) {

    val progress = MutableLiveData<Long>()

    private var progressJob: Job? = null

    override fun start() {
        super.start()
        getPlayer()?.let {
            if (it.isPlaying()) {
                setProgress()
            }
        }
    }

    override fun stop() {
        super.stop()
        disposeProgress()
    }

//    fun gotoNext() {
//        getPlayer()?.next()
//    }

    override fun onInitMetadata(service: PlayerConnection) {
        super.onInitMetadata(service)
        progress.value = service.currentPosition
    }

    override fun onPlaybackChange(service: PlayerConnection) {
        super.onPlaybackChange(service)
        if (service.isPlaying())
            setProgress()
        else {
            disposeProgress()
            progress.value = service.currentPosition
        }
    }

    override fun setProgress() {
        disposeProgress()
        val player = getPlayer() ?: return
        progressJob = viewModelScope.launch { player.progress().collect { progress.value = it } }
    }

    override fun disposeProgress() {
        progressJob?.cancel()
    }

}
