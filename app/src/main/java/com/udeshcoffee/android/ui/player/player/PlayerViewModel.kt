package com.udeshcoffee.android.ui.player.player

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
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

    val repeatMode = MutableLiveData<Int>()
    val isShuffle = MutableLiveData<Boolean>()
    val current = MutableLiveData<Long>()
    val progress = MutableLiveData<Long>()
    val isFav = MutableLiveData<Boolean>()
    val playPosition = MutableLiveData<Int>()
    val queue = MutableLiveData<List<Song>>()
    val seekTime = MutableLiveData<Long>()
    var isSeekTimeShowing = SingleLiveEvent<Boolean>()
    var isLyricsShowing = SingleLiveEvent<Boolean>()

    private var isSeeking: Boolean = false

    private var queueDisposable: Disposable? = null
    private var currentDisposable: Disposable? = null
    private var progressDisposable: Disposable? = null

    override fun start() {
        super.start()
        repeatMode.value = sharedPreferences.getInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.ALL)
        isShuffle.value = sharedPreferences.getBoolean(PreferenceUtil.SHUFFLE, false)

        // Init Lyrics
        isLyricsShowing.value = sharedPreferences.getBoolean(PreferenceUtil.LYRICS, PreferenceUtil.DEFAULT_LYRICS)
    }

    override fun stop() {
        disposeCurrent()
        disposeQueue()
    }

    override fun onInitMetadata(service: MusicService) {
        super.onInitMetadata(service)
        current.value = service.currentPosition
        progress.value = service.currentPosition
        fetchQueue()
    }

    override fun onMetadataChange(service: MusicService) {
        super.onMetadataChange(service)
        service.currentSong()?.let {
            dataRepository.isFavorite(it.id)
                    .subscribe ({
                        isFav.value = it
                    }, {})

        }
        current.value = service.currentPosition
        progress.value = service.currentPosition
        playPosition.value = service.playPosition
    }

    override fun onPlaybackChange(service: MusicService) {
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
        progressDisposable = getService()?.getProgressObservable()
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
        currentDisposable = getService()?.getCurrentObservable()
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

    /* Queue */
    private fun fetchQueue() {
        disposeQueue()
        getService()?.let {
            queueDisposable = it.getQueueObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe { it1 ->
                        queue.value = it1
                        playPosition.value = it.playPosition
                    }
        }
    }

    private fun disposeQueue(){
        queueDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun artScrolled(position: Int) {
        getService()?.apply {
            if (position > playPosition)
                gotoNext()
            else if (position < playPosition)
                gotoBack()
        }
    }

    fun startSeek() {
        disposeProgress()
        isSeeking = true
        getService()?.currentPosition?.let {
            seekTime.value = it
        }
        isSeekTimeShowing.value = true
    }

    fun seeking(progress: Long) {
        seekTime.value = progress
    }

    fun seeked(progress: Long) {
        getService()?.let {
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
        getService()?.gotoNext()
    }

    fun gotoBack() {
        getService()?.gotoBack()
    }

    fun changeRepeatMode() {
        when(repeatMode.value) {
            MusicService.RepeatMode.NONE -> {
                sharedPreferences.edit { putInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.ALL).apply() }
                repeatMode.value = MusicService.RepeatMode.ALL
            }
            MusicService.RepeatMode.ALL -> {
                sharedPreferences.edit { putInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.ONE).apply() }
                repeatMode.value = MusicService.RepeatMode.ONE
            }
            MusicService.RepeatMode.ONE -> {
                sharedPreferences.edit { putInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.NONE).apply() }
                repeatMode.value = MusicService.RepeatMode.NONE
            }
        }
    }

    fun shuffle() {
        isShuffle.value = !isShuffle.value!!
        sharedPreferences.edit().putBoolean(PreferenceUtil.SHUFFLE, isShuffle.value!!).apply()
    }

    fun favToggle() {
        Log.d("toggleFavorite", "favToggle")
        getService()?.currentSong()?.let {
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