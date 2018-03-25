package com.udeshcoffee.android.service

import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.udeshcoffee.android.model.Song
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*


/**
* Created by Udesh Kumarasinghe on 8/22/2017.
*/

class Player(private val musicService: MusicService): Player.EventListener {

    companion object {
        private const val TAG = "Player1997"
    }

    private var mPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DefaultDataSourceFactory
    private lateinit var extractorsFactory: ExtractorsFactory

    private var isPrepared = false
    private var shouldPlay = true
    private var pausedTime: Int = 0
    private var song: Song? = null
    private var currentVolume = 1.0f

    init {
        val bandwidthMeter = DefaultBandwidthMeter()
        val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)

        mPlayer = ExoPlayerFactory.newSimpleInstance(musicService, trackSelector)
        mPlayer.addListener(this)
    }

    fun initSong(song: Song, shouldPlay:Boolean, startPos: Int){
        Log.d(TAG, "preparing" + song.title)
        this.song = song
        this.pausedTime = startPos
        this.shouldPlay = shouldPlay
        mPlayer.stop()

        val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        try {
            dataSourceFactory = DefaultDataSourceFactory(musicService, Util.getUserAgent(musicService, "mediaPlayerSample"))
            extractorsFactory = DefaultExtractorsFactory()
            val mediaSource = ExtractorMediaSource(trackUri, dataSourceFactory, extractorsFactory, null, null)
            isPrepared = false
            mPlayer.prepare(mediaSource)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting data source", e)
            Toast.makeText(musicService.applicationContext, "Error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}

    override fun onSeekProcessed() {
        Log.d(TAG, "onSeekProcessed")
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}

    override fun onPlayerError(error: ExoPlaybackException?) {}

    override fun onLoadingChanged(isLoading: Boolean) {
        Log.d(TAG, "onLoadingChanged $isLoading")
    }

    override fun onPositionDiscontinuity(reason: Int) {
        Log.d(TAG, "onPositionDiscontinuity")
    }

    override fun onRepeatModeChanged(repeatMode: Int) {}

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {}

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
                updatePlayCount()
                musicService.gotoNextOnFinish()
            }
        }
    }

    fun start() {
        try {
            mPlayer.playWhenReady = true
            mPlayer.volume = 0.0f
            if (musicService.isFading)
                fadeIn()
            else
                mPlayer.volume = 1.0f

            musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, true)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Error pausing MultiPlayer: " + e.localizedMessage)
        }
    }

    fun stop() {
        try {
            isPrepared = false
            if (isPlaying())
                pausedTime = getPosition().toInt()
            mPlayer.stop()
            musicService.notifyManger.notifyChange(MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, false)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping MultiPlayer: " + e.localizedMessage)
        }
    }

    /**
     * You CANNOT use this player anymore after calling release()
     */
    fun release() {
        stop()
        mPlayer.removeListener(this)
        mPlayer.release()
    }

    fun pause() {
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

    fun getPosition(): Long {
        return try {
            if (isPlaying())
                mPlayer.currentPosition
            else
                pausedTime.toLong()
        } catch (ignored: IllegalStateException) {
            0
        }

    }

    fun seekTo(whereto: Long): Long {
        try {
            mPlayer.seekTo(whereto)
            if (!isPlaying())
                pausedTime = whereto.toInt()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error seeking MultiPlayer: " + e.localizedMessage)
        }

        return whereto
    }

    fun isPlaying(): Boolean {
        return try {
            mPlayer.playWhenReady
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
        val fadeDuration = 500 //The duration of the fade
        //The amount of time between volume changes. The smaller this is, the smoother the fade
        val fadeInterval = 50
        val maxVolume = 1 //The volume will increase from 0 to 1
        currentVolume = 0f
        val numberOfSteps = fadeDuration / fadeInterval //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        val deltaVolume = maxVolume / numberOfSteps.toFloat()

        //Create a new Timer and Timer task to run the fading outside the main UI thread
        val timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                //Cancel and Purge the Timer if the desired volume has been reached
                if (currentVolume >= 1f) {
                    timer.cancel()
                    timer.purge()
                    mPlayer.volume = 1f
                } else {
                    mPlayer.volume = currentVolume
                    currentVolume += deltaVolume
                }
            }
        }

        timer.schedule(timerTask, fadeInterval.toLong(), fadeInterval.toLong())
    }

    private fun fadeOut(callback: (() -> Unit)? = null) {
        val fadeDuration = 250 //The duration of the fade
        //The amount of time between volume changes. The smaller this is, the smoother the fade
        val fadeInterval = 50
        val maxVolume = 1 //The volume will increase from 0 to 1
        val numberOfSteps = fadeDuration / fadeInterval //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        val deltaVolume = maxVolume / numberOfSteps.toFloat()

        //Create a new Timer and Timer task to run the fading outside the main UI thread
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
                    currentVolume -= deltaVolume
                }
            }
        }

        timer.schedule(timerTask, fadeInterval.toLong(), fadeInterval.toLong())
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