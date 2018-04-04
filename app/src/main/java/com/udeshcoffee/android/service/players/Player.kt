package com.udeshcoffee.android.service.players

import android.content.ContentUris
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import java.util.*

/**
 * Created by Udesh Kumarasinghe on 8/22/2017.
 */

class Player(musicService: MusicService): BasePlayer(musicService), MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener {

    companion object {
        private const val TAG = "Player1997"
    }

    private var mPlayer = MediaPlayer()
    private var isPrepared = false
    private var shouldPlay = true
    private var pausedTime: Int = 0
    private var currentVolume = 1.0f

    init {
        mPlayer.setWakeMode(musicService, PowerManager.PARTIAL_WAKE_LOCK)
        @Suppress("DEPRECATION")
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mPlayer.setOnCompletionListener(this)
        mPlayer.setOnErrorListener(this)
        mPlayer.setOnPreparedListener(this)
        mPlayer.setOnSeekCompleteListener(this)
    }

    override fun initSong(song: Song, shouldPlay:Boolean, startPos: Int){
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

    override fun onSeekComplete(mp: MediaPlayer?) {
        if (!isPlaying())
            musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
    }

    override fun start() {
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

    override fun stop() {
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

    override fun release() {
        stop()
        mPlayer.release()
    }

    override fun pause() {
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

    override fun getPosition(): Long {
        return try {
            if (isPlaying())
                mPlayer.currentPosition.toLong()
            else
                pausedTime.toLong()
        } catch (ignored: IllegalStateException) {
            0
        }

    }

    override fun seekTo(whereto: Long): Long {
        try {
            mPlayer.seekTo(whereto.toInt())
            if (!isPlaying())
                pausedTime = whereto.toInt()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error seeking MultiPlayer: " + e.localizedMessage)
        }

        return whereto
    }

    override fun isPlaying(): Boolean {
        return try {
            mPlayer.isPlaying
        } catch (ignored: IllegalStateException) {
            false
        }
    }

    override fun getAudioSessionId(): Int {
        var sessionId = 0
        try {
            sessionId = mPlayer.audioSessionId
        } catch (ignored: IllegalStateException) {
            //Nothing to do
        }

        return sessionId

    }

    // Fade In / Out
    override fun fadeIn() {
        currentVolume = 0f

        val timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                if (currentVolume >= 1f) {
                    timer.cancel()
                    timer.purge()
                    mPlayer.setVolume(1f, 1f)
                } else {
                    mPlayer.setVolume(currentVolume, currentVolume)
                    currentVolume += FADE_IN_DELTA
                }
            }
        }

        timer.schedule(timerTask, FADE_INTERVAL.toLong(), FADE_INTERVAL.toLong())
    }

    override fun fadeOut(callback: (() -> Unit)?) {
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
                    currentVolume -= FADE_OUT_DELTA
                }
            }
        }

        timer.schedule(timerTask, FADE_INTERVAL.toLong(), FADE_INTERVAL.toLong())
    }
}