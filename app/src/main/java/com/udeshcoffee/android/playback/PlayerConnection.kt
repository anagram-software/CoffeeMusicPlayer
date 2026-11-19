package com.udeshcoffee.android.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * The UI's handle on playback: a MediaController connected to [PlaybackService].
 * Replaces the old bound MusicService and its local broadcasts. All methods must be called on
 * the main thread; most are no-ops until [isConnected].
 */
class PlayerConnection(
        private val context: Context,
        private val mediaRepository: MediaRepository,
        private val queueStore: QueueStore
) {

    private val eventFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private val queueFlow = MutableStateFlow<List<Song>>(emptyList())
    private val stateFlow = MutableStateFlow(PlayerUiState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var future: ListenableFuture<MediaController>? = null
    private var users = 0

    var controller: MediaController? = null
        private set

    val isConnected: Boolean
        get() = controller != null

    /** Everything the UI shows about playback, except the position (see [progress]). */
    val state: StateFlow<PlayerUiState> = stateFlow.asStateFlow()

    /** [PlaybackEvents] names: metadata, playback state, and connection changes. Emitted on the main thread. */
    val events: SharedFlow<String> = eventFlow.asSharedFlow()

    // ---- Connection ----

    /** Connects (reference counted). Pair every call with [disconnect]. */
    fun connect() {
        users++
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            if (future !== f) return@addListener
            try {
                val c = f.get()
                controller = c
                c.addListener(listener)
                refreshQueue()
                refreshState()
                eventFlow.tryEmit(PlaybackEvents.SERVICE_CONNECTED)
                eventFlow.tryEmit(PlaybackEvents.METADATA_CHANGED)
                eventFlow.tryEmit(PlaybackEvents.PLAYBACK_STATE_CHANGED)
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't connect to playback service", e)
                future = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun disconnect() {
        users = (users - 1).coerceAtLeast(0)
        if (users > 0) return
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
        stateFlow.value = PlayerUiState()
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) refreshQueue()
            refreshState()
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_MEDIA_METADATA_CHANGED,
                            Player.EVENT_TIMELINE_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED))
                eventFlow.tryEmit(PlaybackEvents.METADATA_CHANGED)
            if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED,
                            Player.EVENT_PLAYBACK_STATE_CHANGED))
                eventFlow.tryEmit(PlaybackEvents.PLAYBACK_STATE_CHANGED)
        }
    }

    private fun refreshState() {
        val c = controller ?: return
        stateFlow.value = PlayerUiState(
                connected = true,
                song = c.currentMediaItem?.toSong(),
                isPlaying = c.isPlaying,
                duration = c.duration.takeIf { it != C.TIME_UNSET } ?: (c.currentMediaItem?.toSong()?.duration ?: 0),
                queue = queueFlow.value,
                index = c.currentMediaItemIndex,
                repeatMode = RepeatModes.toPreference(c.repeatMode),
                shuffle = c.shuffleModeEnabled)
    }

    private fun refreshQueue() {
        val c = controller ?: return
        queueFlow.value = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).toSong() }
    }

    // ---- State ----

    fun currentSong(): Song? = controller?.currentMediaItem?.toSong()

    fun isPlaying(): Boolean = controller?.isPlaying ?: false

    /** Index of the current song in [queue]. */
    val playPosition: Int
        get() = controller?.currentMediaItemIndex ?: 0

    /** Playback position in ms; setting it seeks. */
    var currentPosition: Long
        get() = controller?.currentPosition ?: 0
        set(value) { controller?.seekTo(value) }

    val duration: Long
        get() = controller?.duration?.takeIf { it != C.TIME_UNSET } ?: currentSong()?.duration ?: 0

    val queue: List<Song>
        get() = queueFlow.value

    val queueChanges: StateFlow<List<Song>> = queueFlow.asStateFlow()

    /** Emits the playback position every [intervalMs] while collected (on the main thread). */
    fun progress(intervalMs: Long = 100): Flow<Long> = flow {
        while (true) {
            emit(currentPosition)
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.Main.immediate)

    /** One of [RepeatModes] PREF_* values (the format the queue screen uses). */
    var repeatMode: Int
        get() = RepeatModes.toPreference(controller?.repeatMode ?: Player.REPEAT_MODE_ALL)
        set(value) { controller?.repeatMode = RepeatModes.fromPreference(value) }

    var shuffleEnabled: Boolean
        get() = controller?.shuffleModeEnabled ?: false
        set(value) { controller?.shuffleModeEnabled = value }

    // ---- Commands ----

    fun setQueue(songs: List<Song>, startIndex: Int, play: Boolean, startPositionMs: Long = 0) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.size - 1), startPositionMs)
        c.prepare()
        c.playWhenReady = play
    }

    /** Adds songs after the current one ([playNext]) or at the end. Starts playback if the queue was empty. */
    fun addToQueue(songs: List<Song>, playNext: Boolean) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        if (c.mediaItemCount == 0) {
            setQueue(songs, 0, true)
            return
        }
        val items = songs.map { it.toMediaItem() }
        if (playNext) c.addMediaItems(c.currentMediaItemIndex + 1, items) else c.addMediaItems(items)
    }

    fun playAt(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        c.seekTo(index, 0)
        c.prepare()
        c.play()
    }

    fun play() {
        val c = controller ?: return
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun pause() { controller?.pause() }

    fun next() { controller?.seekToNext() }

    fun previous() { controller?.seekToPrevious() }

    fun moveInQueue(from: Int, to: Int) { controller?.moveMediaItem(from, to) }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun clearQueue() {
        val c = controller ?: return
        c.stop()
        c.clearMediaItems()
    }

    /** Plays a file opened from another app. */
    fun playUri(uri: Uri) {
        scope.launch {
            try {
                val songs = mediaRepository.loadSong(uri)
                if (songs.isNotEmpty()) setQueue(songs, 0, true)
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't open $uri", e)
            }
        }
    }

    /** Plays songs matching a search query (Assistant's "play X on Coffee Player"); the session resolves it. */
    fun playFromSearch(query: String) {
        val c = controller ?: return
        c.setMediaItem(androidx.media3.common.MediaItem.Builder()
                .setRequestMetadata(androidx.media3.common.MediaItem.RequestMetadata.Builder().setSearchQuery(query).build())
                .build())
        c.prepare()
        c.play()
    }

    /** Shows the last session's queue (paused) when the app opens with nothing queued. */
    fun restoreQueueIfEmpty() {
        val c = controller ?: return
        if (c.mediaItemCount > 0) return
        scope.launch {
            val saved = try {
                queueStore.load()
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't restore queue", e)
                null
            } ?: return@launch
            val now = controller ?: return@launch
            if (now.mediaItemCount == 0) setQueue(saved.songs, saved.index, false, saved.positionMs)
        }
    }

    companion object {
        private const val TAG = "PlayerConnection"
    }
}

/** Snapshot of playback for the UI. [repeatMode] uses [RepeatModes] PREF_* values. */
data class PlayerUiState(
        val connected: Boolean = false,
        val song: Song? = null,
        val isPlaying: Boolean = false,
        val duration: Long = 0,
        val queue: List<Song> = emptyList(),
        val index: Int = 0,
        val repeatMode: Int = RepeatModes.PREF_ALL,
        val shuffle: Boolean = false
)
