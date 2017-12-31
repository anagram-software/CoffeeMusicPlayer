package com.udeshcoffee.android.ui.player.player

import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.getService
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.utils.PreferenceUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 8/25/2017.
 */
class PlayerPresenter(val view: PlayerContract.View,
                      private val sharedPreferences: SharedPreferences,
                      private val dataRepository: DataRepository):
        PlayerContract.Presenter {

    val TAG = "PlayerPresenter"

    override var isPlaying: Boolean = false
    override var isSeeking: Boolean = false
    override var isLyricsShowing: Boolean = false

//    private var listeningSong: Song? = null

    private var broadcastDisposable: Disposable? = null
    private var queueDisposable: Disposable? = null
    private var currentDisposable: Disposable? = null
    private var progressDisposable: Disposable? = null

    override var repeatMode: Int = sharedPreferences.getInt(PreferenceUtil.REPEAT_MODE, MusicService.RepeatMode.ALL)
        set(value) {
            field = value
            sharedPreferences.edit().putInt(PreferenceUtil.REPEAT_MODE, value).apply()
        }

    override var isShuffle: Boolean = sharedPreferences.getBoolean(PreferenceUtil.SHUFFLE, false)
        set(value) {
            field = value
            sharedPreferences.edit().putBoolean(PreferenceUtil.SHUFFLE, value).apply()
        }

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
                            loadDetails(true)
                        }
                        MusicService.InternalIntents.PLAYBACK_STATE_CHANGED -> {
                            setPlaying()
                        }
                        MusicService.InternalIntents.SERVICE_CONNECTED -> {
                            loadDetails(false)
                            setQueue()
                        }
                    }
                }

        loadDetails(false)
        view.setRepeatMode(repeatMode)
        view.setShuffle(isShuffle)

        // Init Lyrics
        isLyricsShowing = sharedPreferences.getBoolean(PreferenceUtil.LYRICS, PreferenceUtil.DEFAULT_LYRICS)
        setLyrics()
        setQueue()
    }

    override fun stop() {
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
        disposeCurrent()
        disposeQueue()
    }

    private fun loadDetails(animate: Boolean) {
        getService()?.let {
            it.currentSong()?.let {
                view.setDetails(it, animate)
                dataRepository.isFavorite(it.id)
                        .subscribe ({
                            view.setFav(it)
                        }, {})
            }
            view.setQueueSize("${it.playPosition + 1}/${it.list.size}")
            view.setPlayPosition(it.playPosition, true)
            view.initProgress(it.duration)
            view.setProgress(it.currentPosition)
            view.setCurrent(it.currentPosition)
        }
        setPlaying()
    }

    /* Progress */
    private fun disposeProgress(){
        progressDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun setProgress(){
        disposeProgress()
        progressDisposable = getService()?.getProgressObservarable(100)
                ?.subscribe{
                    if (!isSeeking)
                        view.setProgress(it)
                }
    }

    /* Current */
    private fun disposeCurrent(){
        currentDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun setCurrent(){
        disposeCurrent()
        currentDisposable = getService()?.getProgressObservarable(1000)
                ?.subscribe{
                    view.setCurrent(it)
                }
    }

    /* Queue */
    private fun disposeQueue(){
        queueDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun setQueue() {
        disposeQueue()
        getService()?.let {
            queueDisposable = it.getQueueObservable()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe { it1 ->
                        view.setPlayerArtPager(it1)
                        view.setPlayPosition(it.playPosition, false)
                    }
        }
    }

    private fun setPlaying(){
        isPlaying = getService()?.isPlaying() ?: false
        if (isPlaying) {
            setCurrent()
            setProgress()
        } else {
            disposeCurrent()
            disposeProgress()
        }
        view.setPlayOrPause(!isPlaying)
    }

    override fun artScrolled(position: Int) {
        getService()?.apply {
            if (position > playPosition)
                gotoNext()
            else if (position < playPosition)
                gotoBack()
        }
    }

    override fun showMore() {
        getService()?.currentSong()?.let { view.showMoreDialog(it) }
    }

    override fun playPauseToggle() {
        val service = getService()
        if (isPlaying)
            service?.pause()
        else
            service?.play()
    }

    override fun startSeek() {
        disposeProgress()
        getService()?.currentPosition?.let { view.setSeekTime(it) }
        view.showHideSeekTime(true)
    }

    override fun seeking(progress: Long) {
        view.setSeekTime(progress)
    }

    override fun seeked(progress: Long) {
        getService()?.let {
            it.currentPosition = progress
            isSeeking = false
            if (isPlaying)
                setProgress()
            else {
                view.setProgress(progress)
                view.setCurrent(progress)
            }
        }
        view.showHideSeekTime(false)
    }

    override fun gotoNext() {
        getService()?.gotoNext()
    }

    override fun gotoBack() {
        getService()?.gotoBack()
    }

    override fun changeRepeatMode() {
        when(repeatMode) {
            MusicService.RepeatMode.NONE -> {
                repeatMode = MusicService.RepeatMode.ALL
            }
            MusicService.RepeatMode.ALL -> {
                repeatMode = MusicService.RepeatMode.ONE
            }
            MusicService.RepeatMode.ONE -> {
                repeatMode = MusicService.RepeatMode.NONE
            }
        }
        view.setRepeatMode(repeatMode)
    }

    override fun shuffle() {
        isShuffle = !isShuffle
        view.setShuffle(isShuffle)
    }

    override fun favToggle() {
        Log.d("toggleFavorite", "favToggle")
        getService()?.currentSong()?.let {
            dataRepository.toggleFavorite(it.id){
                view.setFav(it)
                Log.d("toggleFavorite", "subscribe $it")
            }
        }
    }

    override fun lyricsToggle() {
        isLyricsShowing = !isLyricsShowing
        sharedPreferences.edit().putBoolean(PreferenceUtil.LYRICS, isLyricsShowing).apply()
        setLyrics()
    }

    private fun setLyrics() {
        if (isLyricsShowing)
            view.showLyricUI()
        else
            view.hideLyricUI()
    }

    override fun openQueue() {
        view.showQueueUI()
    }

    override fun closeNowPlay() {
        view.showMainUI()
    }
}