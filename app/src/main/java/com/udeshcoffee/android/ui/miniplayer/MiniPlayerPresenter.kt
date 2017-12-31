package com.udeshcoffee.android.ui.miniplayer

import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.getService
import com.udeshcoffee.android.service.MusicService
import io.reactivex.disposables.Disposable

/**
 * Created by Udathari on 8/25/2017.
 */
class MiniPlayerPresenter(val view: MiniPlayerContract.View): MiniPlayerContract.Presenter {

    val TAG = "MiniPlayerPresenter"

    var broadcastDisposable: Disposable? = null
    var progressDisposable: Disposable? = null

    override var isPlaying: Boolean = false
    init {
        view.presenter = this
    }

    override fun start() {
        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)
        filter.addAction(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED)
        filter.addAction(MusicService.InternalIntents.SERVICE_CONNECTED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            loadDetails()
                        }
                        MusicService.InternalIntents.PLAYBACK_STATE_CHANGED -> {
                            setPlaying()
                        }
                        MusicService.InternalIntents.SERVICE_CONNECTED -> {
                            loadDetails()
                        }
                    }
                }
        loadDetails()
    }

    override fun stop() {
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
        disposeProgress()
    }

    override fun playPauseToggle() {
        val service = getService()
        if (isPlaying)
            service?.pause()
        else
            service?.play()
    }

    override fun gotoNext() {
        getService()?.gotoNext()
    }

    override fun openNowPlay() {
        view.showNowPlayUI()
    }

    override fun setAlpha(alpha: Float) {
        view.setAlpha(alpha)
    }

    override fun setVisibility(visibility: Int) {
        view.setVisibility(visibility)
    }

    override fun loadDetails() {
        val service = getService()
        service?.currentSong()?.let { view.setDetails(it) }
        service?.duration?.let { view.initProgress(it) }
        service?.duration?.let { view.initProgress(it) }
        service?.currentPosition?.let {
            view.setProgress(it)
        }
        setPlaying()
    }

    override fun setPlaying(){
        isPlaying = getService()?.isPlaying() ?: false
        if (isPlaying) {
            setProgress()
        } else {
            disposeProgress()
        }
        view.setPlayOrPause(!isPlaying)
    }

    override fun setProgress() {
        disposeProgress()
        progressDisposable = getService()?.getProgressObservarable(100)
                ?.subscribe {
                    view.setProgress(it)
                }
    }

    fun disposeProgress() {
        progressDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

}
