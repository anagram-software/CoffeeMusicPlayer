package com.udeshcoffee.android.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.model.EQPreset
import com.udeshcoffee.android.extensions.getQueue
import com.udeshcoffee.android.extensions.getSharedData
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.extensions.saveQueue
import com.udeshcoffee.android.utils.ObservableList
import com.udeshcoffee.android.utils.PreferenceUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Udesh Kumarasinghe on 8/22/2017.
 */
class MusicService : Service() {

    private val TAG = "MusicService"

    private val binder = MusicBinder(this)

    val mediaRepository: MediaRepository by inject()
    val dataRepository: DataRepository by inject()

    lateinit var player : Player
    private lateinit var audioManager: AudioManager
    lateinit var mediaSession : MediaSessionCompat
    lateinit var notifyManger : NotifyManager
    lateinit var sharedPreferences : SharedPreferences
    private lateinit var sharedPreferencesListener : SharedPreferences.OnSharedPreferenceChangeListener
    private lateinit var localBroadcastManager: LocalBroadcastManager

    var audioFXHelper: AudioFXHelper? = null

    private var isAttached = false

    var currentPosition : Long
        set(value) {player.seekTo(value)}
        get() = player.getPosition()

    var duration : Long = 0
    var playPosition: Int = 0

    private var wasPlaying = false
    private var isShuffled = false
        set(value) {
            field = value
            checkAndShuffle()
        }

    var list = ObservableList<Song>()
    private var unshuffledList = ArrayList<Song>()
    private var repeatMode: Int = RepeatMode.ALL

    // Pref
    var isFading = true
    var isPausingAfterCall = true

    // Forcus Things
    private var playbackDelayed = false
    private var playbackNowAuthorized = false
    private var resumeOnFocusGain = false
    private val focusHandler = Handler()

    // Listeners
    var becomingNoisy: Disposable? = null

    object InternalIntents {
        val PLAYBACK_STATE_CHANGED = "com.udeshcoffee.android.internalintents.PLAYBACK_STATE_CHANGED"
        val METADATA_CHANGED = "com.udeshcoffee.android.internalintents.METADATA_CHANGED"
        val SERVICE_CONNECTED = "com.udeshcoffee.android.internalintents.SERVICE_CONNECTED"
    }

    object WidgetIntents {
        val PREVIOUS = "com.udeshcoffee.android.widgetintents.previous"
        val PLAY_PAUSE = "com.udeshcoffee.android.widgetintents.play_pause"
        val NEXT = "com.udeshcoffee.android.widgetintents.next"
    }

    companion object {
        val ACTION_STOP = "com.udeshcoffee.android.intents.action_stop"
    }

    object RepeatMode {
        val ONE = 1
        val ALL = 2
        val NONE = 3
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        player = Player(this)
        notifyManger = NotifyManager(this)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, s ->
            when(s) {
                PreferenceUtil.REPEAT_MODE -> {
                    repeatMode = sharedPreferences.getInt(s, RepeatMode.ALL)
                }
                PreferenceUtil.SHUFFLE -> {
                    isShuffled = sharedPreferences.getBoolean(s, false)
                }
                PreferenceUtil.PREF_FADE -> {
                    isFading = sharedPreferences.getBoolean(s, true)
                }
                PreferenceUtil.PREF_CALL -> {
                    isPausingAfterCall = sharedPreferences.getBoolean(s, false)
                }
            }
        }
        repeatMode = sharedPreferences.getInt(PreferenceUtil.REPEAT_MODE, RepeatMode.ALL)
        isShuffled = sharedPreferences.getBoolean(PreferenceUtil.SHUFFLE, false)
        isFading = sharedPreferences.getBoolean(PreferenceUtil.PREF_FADE, true)
        isPausingAfterCall = sharedPreferences.getBoolean(PreferenceUtil.PREF_CALL, false)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)

        startMediaSession()

        try {
            Log.d(TAG, "creating audioFXHelper")
            dataRepository.getEQPresets() as ArrayList<EQPreset>
            audioFXHelper = AudioFXHelper(this, getAudioSessionId(), dataRepository)
        }catch (e: Exception) {
            Log.d(TAG, "execption audioFXHelper $e")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand intent:${intent?.extras}")
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
            val action = intent.action

            when (action) {
                WidgetIntents.PLAY_PAUSE -> {
                    if (!isPlaying()) {
                        if (list.isEmpty())
                            loadLastList({ pos, current ->
                                initSong(pos, true, current)
                            })
                        else
                            play()
                    } else
                        pause()
                }
                WidgetIntents.NEXT  -> {
                    if (list.isEmpty())
                        loadLastList({pos, _ ->
                            playPosition = pos
                            gotoNext()
                        })
                    else
                        gotoNext()
                }
                WidgetIntents.PREVIOUS-> {
                    if (list.isEmpty())
                        loadLastList({pos, _ ->
                            playPosition = pos
                            gotoBack()
                        })
                    else
                        gotoBack()
                }
                ACTION_STOP -> {
                    stop()
                }
            }
        } else
            Log.e(TAG, "fucked")

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        saveUnshuffledQueue()
        player.release()
        focusHandler.removeCallbacks(delayedStopRunnable)
        notifyManger.notifyWidgets(InternalIntents.PLAYBACK_STATE_CHANGED)

        audioFXHelper?.release()

        mediaSession.isActive = false
        mediaSession.release()

        removeAudioFocus()

        disposePlayTimeEvents()

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)

    }

    override fun onBind(p0: Intent?): IBinder {
        Log.d(TAG, "onBind")
        if (list.isNotEmpty())
            notifyManger.notifyChange(InternalIntents.METADATA_CHANGED, true)
        isAttached = true
        return binder
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind")
        notifyManger.notifyChange(InternalIntents.METADATA_CHANGED, true)
        isAttached = true
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        isAttached = false

        // Take a snapshot of the current playlist
        saveUnshuffledQueue()
        if (isPlaying()) {
            // something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop the service now.
            return true
        }
        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between tracks.
//        if (list.isNotEmpty() && isPlaying()) {
//            return true
//        }
        // No active playlist, OK to stop the service right now
        stopSelf()

        return true
    }

    // Notify Actions
    private fun startMediaSession() {
        mediaSession = MediaSessionCompat(this, "CoffeeMusicPlayer", null, null)
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {

            override fun onPause() {
                pause()
            }

            override fun onPlay() {
                play()
            }

            override fun onSeekTo(pos: Long) {
                currentPosition = pos
            }

            override fun onSkipToNext() {
                gotoNext()
            }

            override fun onSkipToPrevious() {
                gotoBack()
            }

            override fun onStop() {
                stop()
            }
        })
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)

        val mbrIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)
        mediaSession.setMediaButtonReceiver(mbrIntent)
    }

    private fun getAudioSessionId(): Int {
        synchronized(this) {
            return player.getAudioSessionId()
        }
    }

    // Event Listeners
    private fun observePlayTimeEvents(){
        becomingNoisy = RxBroadcast.fromBroadcast(this, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
                .subscribe{
                    pause()
                }
    }

    private fun disposePlayTimeEvents(){
        becomingNoisy?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    // Media Control

    fun setSongList(list: ArrayList<Song>) {
        this.list.clearAndAddAll(list)
    }

    fun loadLastListAndInit() {
        if (list.isEmpty())
            loadLastList { pos, current -> initSong(pos, false, current) }
    }

    fun loadLastList(callback: (pos: Int, current: Int)->Unit) {
        getQueue()?.subscribe { songs ->
            val tempSongPos = getSharedData(PreferenceUtil.LAST_PLAYED_POS)
            if (songs.isNotEmpty() && tempSongPos < songs.size && tempSongPos != -1) {
                setSongList(songs as ArrayList<Song>)
                callback(tempSongPos, getSharedData(PreferenceUtil.LAST_CURRENT_POS))
                checkAndShuffle()
                notifyManger.notifyChange(InternalIntents.METADATA_CHANGED, true)
            }
        }
    }

    fun clearSongList() {
        saveUnshuffledQueue()
        this.list.clear()
        playPosition = -1
        wasPlaying = false
        notifyManger.notifyChange(MusicService.InternalIntents.METADATA_CHANGED, false)
        player.stop()
    }

    // Playback
    fun checkAndShuffle(){
        if (isShuffled) {
            if (list.list.isNotEmpty()) {
                val currentSong = list.list[playPosition]
                unshuffledList.clear()
                unshuffledList.addAll(list.list)
                list.list.removeAt(playPosition)
                Collections.shuffle(list.list)
                val tempList = ArrayList<Song>()
                tempList.add(currentSong)
                tempList.addAll(list.list)
                list.clearAndAddAll(tempList)
                playPosition = 0
            }
        } else {
            if (unshuffledList.isNotEmpty()) {
                val currentSong = list.getOrNull(playPosition)
                Log.d(TAG, "current ${currentSong?.id}, ${currentSong?.title}")
                val position = unshuffledList.indexOf(currentSong)
                Log.d(TAG, "in loop $position")
                list.clearAndAddAll(unshuffledList)
                playPosition = position
                Log.d(TAG, "play position $playPosition")
                unshuffledList.clear()
            }
        }
    }

    fun initSong(position: Int, shouldPlay: Boolean) {
        initSong(position, shouldPlay, 0)
        Log.d(TAG, "initSub")
    }

    fun initSong(position: Int, shouldPlay: Boolean, startPos: Int) {
        if (position >= list.size || position < 0)
            return
        playPosition = position
        player.initSong(list.list[playPosition], shouldPlay, startPos)
        Log.d(TAG, "init")
    }

    fun initFile(uri:Uri) {
        mediaRepository.getSong(uri)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    if (it.isNotEmpty()) {
                        setSongList(it as ArrayList<Song>)
                        initSong(0, true)
                    }
                }
    }

    fun  gotoNextOnFinish() {
        if (repeatMode == RepeatMode.ONE){
            player.seekTo(0)
            play()
            focusHandler.removeCallbacks(delayedStopRunnable)
        } else {
            gotoNext()
        }
    }

    fun  gotoNext() {
        playPosition++
        if (playPosition >= list.size) {
            playPosition = 0
            if (repeatMode == RepeatMode.ALL) {
                initSong(playPosition, wasPlaying)
            } else {
                initSong(playPosition, false)
            }
        } else {
            initSong(playPosition, wasPlaying)
        }
        focusHandler.removeCallbacks(delayedStopRunnable)
    }

    fun gotoBack() {
        Log.d(TAG, "gotoBack")
        playPosition--
        if (playPosition < 0){
            playPosition = list.size - 1
        }
        initSong(playPosition, wasPlaying)
        focusHandler.removeCallbacks(delayedStopRunnable)
    }

    fun play() {
        Log.d(TAG, "play")
        if (playbackNowAuthorized) {
            wasPlaying = true
            player.start()
            focusHandler.removeCallbacks(delayedStopRunnable)
            observePlayTimeEvents()
        } else {
            gainAudioFocusAndPlay()
        }
    }

    fun pause() {
        Log.d(TAG, "pause")
        wasPlaying = false
        player.pause()
        disposePlayTimeEvents()
    }

    fun stop() {
        Log.d(TAG, "stop")
        wasPlaying = false
        player.stop()
        Log.d(TAG, "player stop")
        disposePlayTimeEvents()
        focusHandler.removeCallbacks(delayedStopRunnable)
        mediaSession.isActive = false
        removeAudioFocus()
        if (!isAttached) {
            Log.d(TAG, "isNotAttached")
            stopSelf()
        }
        Log.d(TAG, "end stop")
    }

    // Playback Info
    fun isPlaying(): Boolean = player.isPlaying()

    fun currentSong(): Song? = list.getOrNull(playPosition)

    fun getProgressObservarable(period: Long): Observable<Long>{
        return Observable.interval(period, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { return@map currentPosition }
    }

    fun getQueueObservable() : Observable<List<Song>> {
        return list.observable
    }

    // Some Queue saving stuff
    private fun saveUnshuffledQueue() {
        if (isShuffled) {
            val currentSong = list.getOrNull(playPosition)
            val position = unshuffledList.indexOf(currentSong)
            saveQueue(unshuffledList, position, currentPosition)
        } else
            saveQueue(list.list, playPosition, currentPosition)
    }

    // Focus Shit

    private val focusLock = Any()
    var focusRequest: AudioFocusRequest? = null

    private fun gainAudioFocusAndPlay() {
        Log.d(TAG, "gainAudioFocusAndPlay")
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            val mPlaybackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mPlaybackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusListener, focusHandler)
                    .build()
            val res = audioManager.requestAudioFocus (focusRequest)
            synchronized(focusLock) {
                when (res) {
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> playbackNowAuthorized = false
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        playbackNowAuthorized = true
                        mediaSession.isActive = true
                        play()
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                        playbackDelayed = true
                        playbackNowAuthorized = false
                    }
                }
            }
        } else {
            val result = audioManager.requestAudioFocus(audioFocusListener,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN)

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                playbackNowAuthorized = true
                mediaSession.isActive = true
                play()
            }
        }
    }

    private fun removeAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(focusRequest) }
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener {
        when (it) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "gain")
                if (playbackDelayed || resumeOnFocusGain) {

                    Log.d(TAG, "gain playbackDelayed:$playbackDelayed resumeOnFocusGain:$resumeOnFocusGain")
                    mediaSession.isActive = true
                    synchronized(focusLock) {
                        playbackDelayed = false
                        resumeOnFocusGain = false
                    }
                    play()
                }

                playbackNowAuthorized = true
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "loss")
                synchronized(focusLock) {
                    resumeOnFocusGain = false
                    playbackDelayed = false
                }
                pause()
                playbackNowAuthorized = false
                focusHandler.postDelayed(delayedStopRunnable, TimeUnit.SECONDS.toMillis(30))
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "loss trnsient")
                synchronized(focusLock) {
                    resumeOnFocusGain = wasPlaying
                    playbackDelayed = false
                }
                pause()
                playbackNowAuthorized = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "duck")
                // ... pausing or ducking depends on your app
            }
        }
    }

    private val delayedStopRunnable = Runnable { stop() }

}