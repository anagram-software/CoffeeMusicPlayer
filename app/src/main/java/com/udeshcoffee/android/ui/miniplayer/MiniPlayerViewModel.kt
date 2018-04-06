package com.udeshcoffee.android.ui.miniplayer

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.common.viewmodels.BasePlayerViewModel
import io.reactivex.disposables.Disposable

/**
 * Created by Udathari on 8/25/2017.
 */
class MiniPlayerViewModel(application: Application): BasePlayerViewModel(application) {

    val progress = MutableLiveData<Long>()

    private var progressDisposable: Disposable? = null

    override fun stop() {
        super.stop()
        disposeProgress()
    }

    fun gotoNext() {
        getService()?.gotoNext()
    }

    override fun onInitMetadata(service: MusicService) {
        super.onInitMetadata(service)
        progress.value = service.currentPosition
    }

    override fun onPlaybackChange(service: MusicService) {
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
        progressDisposable = getService()?.getProgressObservable()
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
