package com.udeshcoffee.android.ui.player.player

import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.extensions.getPlayer
import android.app.Application
import androidx.lifecycle.MutableLiveData
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.common.viewmodels.BasePlayerViewModel
import com.udeshcoffee.android.utils.PreferenceUtil
import com.udeshcoffee.android.utils.SingleLiveEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Created by Udathari on 8/25/2017.
 */
class PlayerViewModel(
        application: Application,
        private val sharedPreferences: SharedPreferences,
        private val dataRepository: DataRepository
): BasePlayerViewModel(application){


    val current = MutableLiveData<Long>()
    val progress = MutableLiveData<Long>()
    val isFav = MutableLiveData<Boolean>()
    val seekTime = MutableLiveData<Long?>()
    var isSeekTimeShowing = SingleLiveEvent<Boolean>()
    var isLyricsShowing = SingleLiveEvent<Boolean>()

    private var isSeeking: Boolean = false

    private var currentDisposable: Disposable? = null
    private var progressDisposable: Disposable? = null
    /** Song whose favourite state is loaded, so frequent metadata events don't re-query it. */
    private var favCheckedSongId: Long = -1

    override fun start() {
        super.start()
        // Init Lyrics
        isLyricsShowing.value = sharedPreferences.getBoolean(PreferenceUtil.LYRICS, PreferenceUtil.DEFAULT_LYRICS)

        getPlayer()?.let {
            if (it.isPlaying()) {
                setCurrent()
                setProgress()
            }
        }
    }

    override fun stop() {
        super.stop()
        disposeCurrent()
        disposeProgress()
    }

    override fun onInitMetadata(service: PlayerConnection) {
        super.onInitMetadata(service)
        current.value = service.currentPosition
        progress.value = service.currentPosition
    }

    @Suppress("CheckResult") // Single-shot lookup.
    override fun onMetadataChange(service: PlayerConnection) {
        super.onMetadataChange(service)
        service.currentSong()?.let {
            if (it.id != favCheckedSongId) {
                favCheckedSongId = it.id
                dataRepository.isFavorite(it.id)
                        .subscribe ({
                            isFav.value = it
                        }, {})
            }
        }
        current.value = service.currentPosition
        progress.value = service.currentPosition
    }

    override fun onPlaybackChange(service: PlayerConnection) {
        super.onPlaybackChange(service)
        if (service.isPlaying()) {
            setCurrent()
            setProgress()
        } else {
            disposeCurrent()
            disposeProgress()
        }
    }

    /* Progress */
    override fun setProgress(){
        disposeProgress()
        progressDisposable = getPlayer()?.progressObservable()
                ?.subscribe{
                    if (!isSeeking)
                        progress.value = it
                }
    }

    override fun disposeProgress(){
        progressDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    /* Current */
    private fun setCurrent(){
        disposeCurrent()
        currentDisposable = getPlayer()?.progressObservable(1000)
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe{
                    current.value = it
                }
    }

    private fun disposeCurrent(){
        currentDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun startSeek() {
        disposeProgress()
        isSeeking = true
        getPlayer()?.currentPosition?.let {
            seekTime.value = it
        }
        isSeekTimeShowing.value = true
    }

    fun seeking(progress: Long) {
        seekTime.value = progress
    }

    fun seeked(progress: Long) {
        getPlayer()?.let {
            it.currentPosition = progress
            isSeeking = false
            if (isPlaying.value!!)
                setProgress()
            else {
                this.progress.value = progress
                this.current.value = progress
            }
        }
        isSeekTimeShowing.value = false
        seekTime.value = null
    }

    fun gotoNext() {
        getPlayer()?.next()
    }

    fun gotoBack() {
        getPlayer()?.previous()
    }

    fun favToggle() {
        Log.d("toggleFavorite", "favToggle")
        getPlayer()?.currentSong()?.let {
            dataRepository.toggleFavorite(it.id){
                isFav.value = it
                Log.d("toggleFavorite", "subscribe $it")
            }
        }
    }

    fun lyricsToggle() {
        isLyricsShowing.value = !isLyricsShowing.value!!
        sharedPreferences.edit().putBoolean(PreferenceUtil.LYRICS, isLyricsShowing.value!!).apply()
    }

}