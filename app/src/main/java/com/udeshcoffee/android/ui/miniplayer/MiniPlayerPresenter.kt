package com.udeshcoffee.android.ui.miniplayer

import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.service.MusicService
import io.reactivex.disposables.Disposable
import org.koin.standalone.KoinComponent

/**
 * Created by Udathari on 8/25/2017.
 */
class MiniPlayerPresenter(): MiniPlayerContract.Presenter, KoinComponent {

    val TAG = "MiniPlayerPresenter"

    override lateinit var view: MiniPlayerContract.View

    var broadcastDisposable: Disposable? = null
    private var progressDisposable: Disposable? = null

    override var isPlaying: Boolean = false

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
        getService()?.let {
            isPlaying = it.isPlaying()
            if (isPlaying)
                setProgress()
            else {
                disposeProgress()
                view.setProgress(it.currentPosition)
            }
        }
        view.setPlayOrPause(!isPlaying)
    }

    override fun setProgress() {
        disposeProgress()
        progressDisposable = getService()?.getProgressObservable()
                ?.subscribe {
                    view.setProgress(it)
                }
    }

    private fun disposeProgress() {
        progressDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

}
