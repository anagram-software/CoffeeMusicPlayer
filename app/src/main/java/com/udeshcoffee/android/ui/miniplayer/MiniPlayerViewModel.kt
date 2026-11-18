package com.udeshcoffee.android.ui.miniplayer

import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.extensions.getPlayer
import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.udeshcoffee.android.ui.common.viewmodels.BasePlayerViewModel
import io.reactivex.disposables.Disposable

/**
 * Created by Udathari on 8/25/2017.
 */
class MiniPlayerViewModel(application: Application): BasePlayerViewModel(application) {

    val progress = MutableLiveData<Long>()

    private var progressDisposable: Disposable? = null

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
        progressDisposable = getPlayer()?.progressObservable()
                ?.subscribe {
                    progress.value = it
                }
    }

    override fun disposeProgress() {
        progressDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

}
