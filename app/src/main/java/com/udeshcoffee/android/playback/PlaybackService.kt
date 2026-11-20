package com.udeshcoffee.android.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.data.download.DownloadRepository
import com.udeshcoffee.android.data.metabrainz.ListenBrainzScrobbler
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.ui.MainActivity
import com.udeshcoffee.android.utils.PreferenceUtil
import com.udeshcoffee.android.widget.BigAppWidget
import com.udeshcoffee.android.widget.MediumAppWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Background playback: an ExoPlayer exposed through a Media3 MediaLibrarySession.
 *
 * Media3 handles the notification, foreground state, media buttons, audio focus, headphone
 * unplugging and playback resumption. This class adds the app's own behaviour: queue
 * persistence, play counts, ListenBrainz listens, widget updates, the fade and on-call settings, and a browse tree for Android Auto and other media browsers.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private val dataRepository: DataRepository by inject()
    private val mediaRepository: MediaRepository by inject()
    private val subsonic: SubsonicRepository by inject()
    private val queueStore: QueueStore by inject()
    private val sourceStore: MusicSourceStore by inject()
    private val downloads: DownloadRepository by inject()
    private val listenBrainz: ListenBrainzScrobbler by inject()

    /** Listening time of the current play, for ListenBrainz listens. */
    private val scrobbles = ScrobbleTracker()
    private var nowPlayingSent = false

    private lateinit var player: ExoPlayer
    private lateinit var prefs: SharedPreferences
    private lateinit var browser: LibraryBrowser
    private var session: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The item that was playing before the latest transition, for play counts. */
    private var lastItem: MediaItem? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        browser = LibraryBrowser(mediaRepository, subsonic, sourceStore)

        player = ExoPlayer.Builder(this)
                .setMediaSourceFactory(playbackMediaSourceFactory(this, downloads.cache))
                .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(), /* handleAudioFocus = */ true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()
        player.repeatMode = RepeatModes.fromPreference(prefs.getInt(PreferenceUtil.REPEAT_MODE, RepeatModes.PREF_ALL))
        player.addListener(playerListener)
        // Send plays queued while offline.
        listenBrainz.flush()

        val sessionActivity = PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val sessionPlayer = SessionPlayer(player,
                initialShuffle = prefs.getBoolean(PreferenceUtil.SHUFFLE, false),
                isFadeEnabled = { prefs.getBoolean(PreferenceUtil.PREF_FADE, true) },
                onShuffleChanged = { prefs.edit { putBoolean(PreferenceUtil.SHUFFLE, it) } })
        session = MediaLibrarySession.Builder(this, sessionPlayer, LibraryCallback())
                .setSessionActivity(sessionActivity)
                .setBitmapLoader(ArtworkBitmapLoader(this))
                .build()

        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build().apply {
            setSmallIcon(R.drawable.ic_notification)
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing in the background; otherwise there's no reason to stay alive.
        if (!isPlaybackOngoing) {
            saveQueue()
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onDestroy() {
        scrobbleIfDue()
        saveQueue()
        updateWidgets(PlaybackEvents.PLAYBACK_STATE_CHANGED)
        session?.run {
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
                lastItem?.let { countPlay(it) }
            lastItem = mediaItem
            // ListenBrainz: the previous play counts if it was listened to long enough (skips included).
            scrobbleIfDue()
            scrobbles.start(mediaItem?.toSong(), player.isPlaying, System.currentTimeMillis(), SystemClock.elapsedRealtime())
            nowPlayingSent = false
            if (player.isPlaying) sendNowPlaying()
            // Server songs: tell the server what's playing (scrobbling "now playing").
            mediaItem?.toSong()?.remoteId?.let { id -> scope.launch { subsonic.scrobble(id, submission = false) } }
            saveQueue()
            updateWidgets(PlaybackEvents.METADATA_CHANGED)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            scrobbles.setPlaying(isPlaying, System.currentTimeMillis(), SystemClock.elapsedRealtime())
            if (isPlaying) sendNowPlaying() else scrobbleIfDue()
            if (!isPlaying) saveQueue()
            updateWidgets(PlaybackEvents.PLAYBACK_STATE_CHANGED)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // End of the queue with repeat off: count the last song and rewind to the start, paused.
                player.currentMediaItem?.let { countPlay(it) }
                // Playing it again from the start is a new play (no transition when the queue is one song).
                scrobbleIfDue()
                scrobbles.start(player.currentMediaItem?.toSong(), false, System.currentTimeMillis(), SystemClock.elapsedRealtime())
                nowPlayingSent = false
                player.playWhenReady = false
                if (player.mediaItemCount > 0) player.seekTo(0, 0)
            }
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) saveQueue()
        }

        override fun onPlaybackSuppressionReasonChanged(reason: Int) {
            // "On call" setting: stay paused after a transient focus loss instead of resuming.
            if (reason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS &&
                    prefs.getBoolean(PreferenceUtil.PREF_CALL, false))
                player.pause()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            prefs.edit { putInt(PreferenceUtil.REPEAT_MODE, RepeatModes.toPreference(repeatMode)) }
        }
    }

    /** Submits the current play to ListenBrainz once it has been listened to long enough ([ScrobbleTracker]). */
    private fun scrobbleIfDue() {
        val play = scrobbles.due(SystemClock.elapsedRealtime()) ?: return
        listenBrainz.scrobble(play.song, play.startedAtSec)
    }

    private fun sendNowPlaying() {
        if (nowPlayingSent) return
        val song = player.currentMediaItem?.toSong() ?: return
        nowPlayingSent = true
        listenBrainz.nowPlaying(song)
    }

    private fun countPlay(item: MediaItem) {
        val song = item.toSong()
        // Server songs are counted by the server (and passed on to ListenBrainz / Last.fm there).
        song.remoteId?.let { id -> scope.launch { subsonic.scrobble(id, submission = true) }; return }
        scope.launch {
            try {
                dataRepository.increasePlayCount(song)
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't update play count", e)
            }
        }
    }

    private fun saveQueue() {
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        val songs = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toSong() }
        queueStore.save(songs, player.currentMediaItemIndex, player.currentPosition)
    }

    private fun updateWidgets(what: String) {
        val song = player.currentMediaItem?.toSong()
        val playing = ::player.isInitialized && player.isPlaying
        MediumAppWidget.getInstance().notifyChange(applicationContext, what, song, playing)
        BigAppWidget.getInstance().notifyChange(applicationContext, what, song, playing)
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onAddMediaItems(mediaSession: MediaSession, controller: MediaSession.ControllerInfo,
                                     mediaItems: MutableList<MediaItem>): ListenableFuture<MutableList<MediaItem>> {
            // Voice search ("play X" from Assistant / Android Auto) arrives as a single item with a query.
            mediaItems.singleOrNull()?.requestMetadata?.searchQuery?.let { query ->
                return scope.future { browser.search(query).toMutableList() }
            }
            // Our own UI sends complete items; external browsers may send only a media ID.
            if (mediaItems.all { it.mediaMetadata.title != null })
                return Futures.immediateFuture(mediaItems.map { it.withPlaybackUri() }.toMutableList())
            return scope.future {
                mediaItems.map { item -> browser.song(item.mediaId) ?: item.withPlaybackUri() }.toMutableList()
            }
        }

        override fun onPlaybackResumption(mediaSession: MediaSession, controller: MediaSession.ControllerInfo,
                                          isForPlayback: Boolean): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
                scope.future {
                    val saved = queueStore.load() ?: throw UnsupportedOperationException("No saved queue")
                    MediaSession.MediaItemsWithStartPosition(saved.songs.map { it.toMediaItem() }, saved.index, saved.positionMs)
                }

        override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
                                      params: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> =
                Futures.immediateFuture(LibraryResult.ofItem(this@PlaybackService.browser.root, params))

        override fun onGetChildren(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String,
                                   page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
                scope.future {
                    val all = this@PlaybackService.browser.children(parentId)
                    val from = (page * pageSize).coerceAtMost(all.size)
                    val to = (from + pageSize).coerceAtMost(all.size)
                    LibraryResult.ofItemList(ImmutableList.copyOf(all.subList(from, to)), params)
                }

        override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo,
                               mediaId: String): ListenableFuture<LibraryResult<MediaItem>> =
                scope.future {
                    this@PlaybackService.browser.song(mediaId)?.let { LibraryResult.ofItem(it, null) }
                            ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                }
    }

    companion object {
        private const val TAG = "PlaybackService"
    }
}

/** Maps the stored repeat preference (1 = one, 2 = all, 3 = none) to Player modes. */
object RepeatModes {
    const val PREF_ONE = 1
    const val PREF_ALL = 2
    const val PREF_NONE = 3

    fun fromPreference(value: Int): Int = when (value) {
        PREF_ONE -> Player.REPEAT_MODE_ONE
        PREF_NONE -> Player.REPEAT_MODE_OFF
        else -> Player.REPEAT_MODE_ALL
    }

    fun toPreference(mode: Int): Int = when (mode) {
        Player.REPEAT_MODE_ONE -> PREF_ONE
        Player.REPEAT_MODE_OFF -> PREF_NONE
        else -> PREF_ALL
    }
}
