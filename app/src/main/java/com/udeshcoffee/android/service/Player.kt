package com.udeshcoffee.android.service

import android.content.ContentUris
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.udeshcoffee.android.model.Song
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * Created by Udesh Kumarasinghe on 8/22/2017.
 */

class Player(private val musicService: MusicService) : MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private val TAG = "Player1997"

    private var mPlayer = MediaPlayer()
    private var isPrepared = false
    private var shouldPlay = true
    private var pausedTime: Int = 0
    private var song: Song? = null
    private var currentVolume = 1.0f

    init {
        mPlayer.setWakeMode(musicService, PowerManager.PARTIAL_WAKE_LOCK)
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mPlayer.setOnCompletionListener(this)
        mPlayer.setOnErrorListener(this)
        mPlayer.setOnPreparedListener(this)
    }

    fun initSong(song: Song, shouldPlay:Boolean, startPos: Int){
        Log.d(TAG, "preparing" + song.title)
        this.song = song
        this.pausedTime = startPos
        this.shouldPlay = shouldPlay
        mPlayer.reset()

        val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        try {
            mPlayer.setDataSource(musicService.applicationContext, trackUri)
            mPlayer.prepareAsync()
            Log.d(TAG, "preparingAsync")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting data source", e)
            Toast.makeText(musicService.applicationContext, "Error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        return false
    }

    override fun onCompletion(p0: MediaPlayer?) {
        updatePlayCount()
        musicService.gotoNextOnFinish()
    }

    override fun onPrepared(p0: MediaPlayer?) {
        isPrepared = true
        musicService.duration = mPlayer.duration.toLong()
        mPlayer.seekTo(pausedTime)
        musicService.notifyManger.notifyChange(MusicService.InternalIntents.METADATA_CHANGED, true)
        if (shouldPlay) {
            musicService.play()
        } else {
            musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
        }
    }

    fun start() {
        try {
            mPlayer.setVolume(0.0f, 0.0f)
            if (isPrepared) {
                mPlayer.start()
                if (musicService.isFading)
                    fadeIn()
                else
                    mPlayer.setVolume(1.0f, 1.0f)
            } else {
                mPlayer.prepare()
                mPlayer.start()
                if (musicService.isFading)
                    fadeIn()
                else
                    mPlayer.setVolume(1.0f, 1.0f)
            }
            musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Error pausing MultiPlayer: " + e.localizedMessage)
        }
    }

    fun stop() {
        try {
            if (isPlaying())
                pausedTime = getPosition().toInt()
            mPlayer.stop()
            musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, false)
            isPrepared = false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping MultiPlayer: " + e.localizedMessage)
        }
    }

    /**
     * You CANNOT use this player anymore after calling release()
     */
    fun release() {
        stop()
        mPlayer.release()
    }

    fun pause() {
        try {
            if (isPlaying()) {
                pausedTime = getPosition().toInt()
                if (musicService.isFading) {
                    fadeOut({
                        mPlayer.pause()
                        musicService.notifyManger
                                .notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
                    })
                } else {
                    mPlayer.pause()
                    musicService.notifyManger
                            .notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
                }
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error pausing MultiPlayer: " + e.localizedMessage)
        }

    }

    fun getDuration(): Long {
        return try {
            mPlayer.duration.toLong()
        } catch (ignored: IllegalStateException) {
            0
        }

    }

    fun getPosition(): Long {
        return try {
            if (isPlaying())
                mPlayer.currentPosition.toLong()
            else
                pausedTime.toLong()
        } catch (ignored: IllegalStateException) {
            0
        }

    }

    fun seekTo(whereto: Long): Long {
        try {
            mPlayer.seekTo(whereto.toInt())
            if (!isPlaying())
                pausedTime = whereto.toInt()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error seeking MultiPlayer: " + e.localizedMessage)
        }

        return whereto
    }

    fun isPlaying(): Boolean {
        return try {
            mPlayer.isPlaying
        } catch (ignored: IllegalStateException) {
            false
        }
    }

    fun getAudioSessionId(): Int {
        var sessionId = 0
        try {
            sessionId = mPlayer.audioSessionId
        } catch (ignored: IllegalStateException) {
            //Nothing to do
        }

        return sessionId

    }

    // Fade In / Out

    private fun fadeIn() {
        val FADE_DURATION = 500 //The duration of the fade
        //The amount of time between volume changes. The smaller this is, the smoother the fade
        val FADE_INTERVAL = 50
        val MAX_VOLUME = 1 //The volume will increase from 0 to 1
        currentVolume = 0f
        val numberOfSteps = FADE_DURATION / FADE_INTERVAL //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        val deltaVolume = MAX_VOLUME / numberOfSteps.toFloat()

        //Create a new Timer and Timer task to run the fading outside the main UI thread
        val timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                //Cancel and Purge the Timer if the desired volume has been reached
                if (currentVolume >= 1f) {
                    timer.cancel()
                    timer.purge()
                    mPlayer.setVolume(1f, 1f)
                } else {
                    mPlayer.setVolume(currentVolume, currentVolume)
                    currentVolume += deltaVolume
                }
            }
        }

        timer.schedule(timerTask, FADE_INTERVAL.toLong(), FADE_INTERVAL.toLong())
    }

    private fun fadeOut(callback: (() -> Unit)? = null) {
        val FADE_DURATION = 250 //The duration of the fade
        //The amount of time between volume changes. The smaller this is, the smoother the fade
        val FADE_INTERVAL = 50
        val MAX_VOLUME = 1 //The volume will increase from 0 to 1
        val numberOfSteps = FADE_DURATION / FADE_INTERVAL //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        val deltaVolume = MAX_VOLUME / numberOfSteps.toFloat()

        //Create a new Timer and Timer task to run the fading outside the main UI thread
        val timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                //Cancel and Purge the Timer if the desired volume has been reached
                if (currentVolume <= 0f) {
                    timer.cancel()
                    timer.purge()
                    mPlayer.setVolume(0f, 0f)
                    callback?.let { it() }
                } else {
                    mPlayer.setVolume(currentVolume, currentVolume)
                    currentVolume -= deltaVolume
                }
            }
        }

        timer.schedule(timerTask, FADE_INTERVAL.toLong(), FADE_INTERVAL.toLong())
    }

    private fun updatePlayCount() {
        Log.d(TAG, "updatePlayCount")
        song?.let {
            Observable.just(it)
                    .observeOn(Schedulers.io())
                    .take(1)
                    .subscribe {
                        Log.d(TAG, "updatePlayCount: Subscribed")
                        musicService.dataRepository.increasePlayCount(it)
                    }
        }
    }
}