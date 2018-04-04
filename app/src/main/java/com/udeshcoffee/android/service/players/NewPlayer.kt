package com.udeshcoffee.android.service.players

import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import java.util.*


/**
* Created by Udesh Kumarasinghe on 8/22/2017.
 *
 * This is a new Player implementation using Google's ExoPlayer currently backed down because
 * ExoPlayer doesn't support seeking for FLAC file which doesn't have seekable metadata.
*/

class NewPlayer(musicService: MusicService): BasePlayer(musicService), NewPlayerEventListener {

    companion object {
        private const val TAG = "NewPlayer1997"
    }

    private var mPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DefaultDataSourceFactory
    private lateinit var extractorsFactory: DefaultExtractorsFactory

    private var isPrepared = false
    private var shouldPlay = true
    private var pausedTime: Int = 0
    private var currentVolume = 1.0f

    init {
        val bandwidthMeter = DefaultBandwidthMeter()
        val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)

        mPlayer = ExoPlayerFactory.newSimpleInstance(musicService, trackSelector)
        mPlayer.addListener(this)
    }

    private fun initSongAgain() {
        this.song?.let { initSong(it, true, pausedTime) }
    }

    override fun initSong(song: Song, shouldPlay:Boolean, startPos: Int){
        Log.d(TAG, "preparing" + song.title)
        this.song = song
        this.pausedTime = startPos
        this.shouldPlay = shouldPlay
        mPlayer.playWhenReady = false
        mPlayer.stop()

        val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        try {
            dataSourceFactory = DefaultDataSourceFactory(musicService, Util.getUserAgent(musicService, "mediaPlayerSample"))
            extractorsFactory = DefaultExtractorsFactory()
            val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(trackUri)
            isPrepared = false
            mPlayer.prepare(mediaSource)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting data source", e)
            Toast.makeText(musicService.applicationContext, "Error", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.d(TAG, "onPlayerStateChanged $playWhenReady $playbackState")
        when (playbackState) {
            ExoPlayer.STATE_READY -> {
                if (!isPrepared) {
                    isPrepared = true
                    musicService.notifyManger.notifyChange(MusicService.InternalIntents.METADATA_CHANGED, true)
                    musicService.duration = mPlayer.duration
                    mPlayer.seekTo(pausedTime.toLong())
                    if (shouldPlay) {
                        musicService.play()
                    } else {
                        musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
                    }
                }
            }
            ExoPlayer.STATE_ENDED -> {
                if (playWhenReady) {
                    updatePlayCount()
                    musicService.gotoNextOnFinish()
                }
            }
        }
    }

    override fun onSeekProcessed() {
        if (!isPlaying())
            musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
    }

    override fun start() {
        try {
            if (isPrepared) {
                mPlayer.volume = 0.0f
                mPlayer.playWhenReady = true
                if (musicService.isFading)
                    fadeIn()
                else
                    mPlayer.volume = 1.0f

                musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
            } else {
                initSongAgain()
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Error pausing MultiPlayer: " + e.localizedMessage)
        }
    }

    override fun stop() {
        try {
            isPrepared = false
            if (isPlaying())
                pausedTime = getPosition().toInt()
            mPlayer.playWhenReady = false
            mPlayer.stop()
            musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, false)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping MultiPlayer: " + e.localizedMessage)
        }
    }

    /**
     * You CANNOT use this player anymore after calling release()
     */
    override fun release() {
        stop()
        mPlayer.removeListener(this)
        mPlayer.release()
    }

    override fun pause() {
        try {
            if (isPlaying()) {
                pausedTime = getPosition().toInt()
                if (musicService.isFading) {
                    fadeOut({
                        mPlayer.playWhenReady = false
                        musicService.notifyManger
                                .notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
                    })
                } else {
                    mPlayer.playWhenReady = false
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
                mPlayer.currentPosition
            else
                pausedTime.toLong()
        } catch (ignored: IllegalStateException) {
            0
        }

    }

    override fun seekTo(whereto: Long): Long {
        try {
            mPlayer.seekTo(whereto)
            if (!isPlaying())
                pausedTime = whereto.toInt()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error seeking MultiPlayer: " + e.localizedMessage)
        }

        return whereto
    }

    override fun isPlaying(): Boolean {
        return try {
            mPlayer.playWhenReady
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
                    mPlayer.volume = 1f
                } else {
                    mPlayer.volume = currentVolume
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
                    mPlayer.volume = 0f
                    callback?.let { it() }
                } else {
                    mPlayer.volume = currentVolume
                    currentVolume -= FADE_OUT_DELTA
                }
            }
        }

        timer.schedule(timerTask, FADE_INTERVAL.toLong(), FADE_INTERVAL.toLong())
    }
}