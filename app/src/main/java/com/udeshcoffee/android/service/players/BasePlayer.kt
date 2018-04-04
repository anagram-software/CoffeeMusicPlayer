package com.udeshcoffee.android.service.players

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udesh Kumarasinghe on 8/22/2017.
 */

abstract class BasePlayer(protected val musicService: MusicService) {

    protected var song: Song? = null

    companion object {
        private const val MAX_VOLUME = 1
        const val FADE_INTERVAL = 50

        // Fade In
        private const val FADE_IN_DURATION = 500
        private const val FADE_IN_STEPS = FADE_IN_DURATION / FADE_INTERVAL
        const val FADE_IN_DELTA = MAX_VOLUME / FADE_IN_STEPS.toFloat()

        // Fade Out
        private const val FADE_OUT_DURATION = 250
        private const val FADE_OUT_STEPS = FADE_OUT_DURATION / FADE_INTERVAL
        const val FADE_OUT_DELTA = MAX_VOLUME / FADE_OUT_STEPS.toFloat()
    }

    abstract fun initSong(song: Song, shouldPlay:Boolean, startPos: Int)

    abstract fun start()

    abstract fun stop()

    abstract fun release()

    abstract fun pause()

    abstract fun getPosition(): Long

    abstract fun seekTo(whereto: Long): Long

    abstract fun isPlaying(): Boolean

    abstract fun getAudioSessionId(): Int

    protected abstract fun fadeIn()

    protected abstract fun fadeOut(callback: (() -> Unit)? = null)

    protected fun updatePlayCount() {
        song?.let {
            Observable.just(it)
                    .observeOn(Schedulers.io())
                    .take(1)
                    .subscribe {
                        musicService.dataRepository.increasePlayCount(it)
                    }
        }
    }
}