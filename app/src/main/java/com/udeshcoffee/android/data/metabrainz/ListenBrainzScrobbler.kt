package com.udeshcoffee.android.data.metabrainz

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.udeshcoffee.android.BuildConfig
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.PreferenceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** A listen waiting to be sent to ListenBrainz. */
@Serializable
data class PendingListen(
        val artist: String,
        val track: String,
        val release: String? = null,
        val durationMs: Long? = null,
        val trackNumber: Int? = null,
        /** When the play started, in Unix seconds. */
        val listenedAt: Long
)

/**
 * Submits listens to the connected ListenBrainz account ([ListenBrainzAccount]), for device and
 * server songs alike. "Playing now" is best effort; listens are queued on disk first, so plays
 * made offline (or while ListenBrainz is down) are sent later (one as `single`, several as
 * `import`, up to [BATCH_SIZE]) when the next play is submitted or playback starts. [enabled] is
 * off by default for a music server, which usually submits listens itself.
 */
class ListenBrainzScrobbler(context: Context, private val account: ListenBrainzAccount, private val settings: SharedPreferences,
                            private val sourceStore: MusicSourceStore) {

    /** Same file as the account, so disconnecting (which clears it) drops the queue too. */
    private val prefs = context.getSharedPreferences("listenbrainz", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val listSerializer = ListSerializer(PendingListen.serializer())

    private val _accepted = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    /** How many listens ListenBrainz accepted, each time some are sent (so the UI can update its counts). */
    val accepted: SharedFlow<Int> = _accepted.asSharedFlow()

    /** "Submit listens" setting, in the default prefs so it outlives disconnecting. Off by default for a music server. */
    var enabled: Boolean
        get() = settings.getBoolean(PreferenceUtil.PREF_LISTENBRAINZ_SCROBBLE, !sourceStore.isServer)
        set(value) = settings.edit { putBoolean(PreferenceUtil.PREF_LISTENBRAINZ_SCROBBLE, value) }

    private val active: Boolean get() = enabled && account.isConnected

    fun nowPlaying(song: Song) {
        if (!active) return
        val listen = pendingListen(song, 0) ?: return
        val token = account.token ?: return
        scope.launch {
            try {
                ListenBrainzApi.post("submit-listens", submission("playing_now", listOf(listen)), token)
            } catch (e: MetaBrainzException) {
                Log.w(TAG, "Playing now failed: ${e.message}")
                if (e.isAuthError) account.disconnect()
            }
        }
    }

    /** Queues a play (synchronously, so it survives the process dying) and tries to send the queue. */
    fun scrobble(song: Song, startedAtSec: Long) {
        if (!active) return
        val entry = pendingListen(song, startedAtSec) ?: return
        synchronized(this) { save((load() + entry).takeLast(MAX_PENDING)) }
        flush()
    }

    /** Sends queued listens, if any. */
    fun flush() {
        if (!active || load().isEmpty()) return
        scope.launch { mutex.withLock { sendPending() } }
    }

    private suspend fun sendPending() {
        while (true) {
            val token = account.token ?: return
            val batch = synchronized(this) { load() }.take(BATCH_SIZE)
            if (batch.isEmpty()) return
            try {
                ListenBrainzApi.post("submit-listens", submission(if (batch.size == 1) "single" else "import", batch), token)
                _accepted.tryEmit(batch.size)
            } catch (e: MetaBrainzException) {
                when {
                    e.isAuthError -> { account.disconnect(); return }
                    // Offline, ListenBrainz down or rate limited: keep them for later.
                    e.isRetryable -> { Log.w(TAG, "Listens deferred: ${e.message}"); return }
                    // Rejected (invalid listen): drop the batch rather than retry forever.
                    else -> Log.w(TAG, "Listens rejected: ${e.message}")
                }
            }
            synchronized(this) { save(load().drop(batch.size)) }
        }
    }

    private fun load(): List<PendingListen> =
            prefs.getString(KEY_PENDING, null)?.let { runCatching { MetaBrainz.json.decodeFromString(listSerializer, it) }.getOrNull() }
                    ?: emptyList()

    private fun save(list: List<PendingListen>) = prefs.edit(commit = true) {
        if (list.isEmpty()) remove(KEY_PENDING) else putString(KEY_PENDING, MetaBrainz.json.encodeToString(listSerializer, list))
    }

    companion object {
        private const val TAG = "ListenBrainzScrobbler"
        private const val KEY_PENDING = "pending_listens"
        /** ListenBrainz allows 1000 listens per request; smaller batches keep requests small. */
        private const val BATCH_SIZE = 100
        private const val MAX_PENDING = 5000

        /** MediaStore's placeholder for missing tags. */
        private fun String.known(): String? = trim().takeIf { it.isNotEmpty() && it != "<unknown>" }

        /** A listen to submit, or null when the song lacks the artist or title ListenBrainz requires. */
        fun pendingListen(song: Song, startedAtSec: Long): PendingListen? {
            val artist = song.artistName.known() ?: return null
            val track = song.title.known() ?: return null
            return PendingListen(artist, track, song.albumName.known(), song.duration.takeIf { it > 0 },
                    song.track.takeIf { it > 0 }, startedAtSec)
        }

        /** A submit-listens body. `playing_now` listens carry no timestamp. */
        fun submission(listenType: String, listens: List<PendingListen>): JsonObject = buildJsonObject {
            put("listen_type", listenType)
            put("payload", buildJsonArray {
                listens.forEach { l ->
                    add(buildJsonObject {
                        if (listenType != "playing_now") put("listened_at", l.listenedAt)
                        putJsonObject("track_metadata") {
                            put("artist_name", l.artist)
                            put("track_name", l.track)
                            l.release?.let { put("release_name", it) }
                            putJsonObject("additional_info") {
                                l.durationMs?.let { put("duration_ms", it) }
                                l.trackNumber?.let { put("tracknumber", it) }
                                put("media_player", MetaBrainz.CLIENT_NAME)
                                put("submission_client", MetaBrainz.CLIENT_NAME)
                                put("submission_client_version", BuildConfig.VERSION_NAME)
                            }
                        }
                    })
                }
            })
        }
    }
}
