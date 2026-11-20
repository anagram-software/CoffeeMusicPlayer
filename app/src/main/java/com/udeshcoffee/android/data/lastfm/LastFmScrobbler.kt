package com.udeshcoffee.android.data.lastfm

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.PreferenceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A play waiting to be sent to Last.fm. */
@Serializable
data class PendingScrobble(
        val artist: String,
        val track: String,
        val album: String? = null,
        val durationSec: Long? = null,
        val trackNumber: Int? = null,
        /** When the play started, in Unix seconds. */
        val timestamp: Long
)

/**
 * Scrobbles to the connected Last.fm account ([LastFmAccount]), for device and server songs alike.
 * "Now playing" is best effort; scrobbles are queued on disk first, so plays made offline (or
 * while Last.fm is down) are sent later, in batches of up to 50, when the next play is
 * scrobbled or playback starts. Turned off with [enabled], e.g. when the music server already
 * scrobbles to Last.fm.
 */
class LastFmScrobbler(context: Context, private val account: LastFmAccount, private val settings: SharedPreferences) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(PendingScrobble.serializer())

    private val _accepted = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    /** How many scrobbles Last.fm accepted, each time a batch is sent (so the UI can update its counts). */
    val accepted: SharedFlow<Int> = _accepted.asSharedFlow()

    /** "Scrobble to Last.fm" setting, in the default prefs so it outlives disconnecting. */
    var enabled: Boolean
        get() = settings.getBoolean(PreferenceUtil.PREF_LASTFM_SCROBBLE, PreferenceUtil.DEFAULT_LASTFM_SCROBBLE)
        set(value) = settings.edit { putBoolean(PreferenceUtil.PREF_LASTFM_SCROBBLE, value) }

    private val active: Boolean get() = enabled && account.isConnected

    fun nowPlaying(song: Song) {
        if (!active) return
        val params = trackParams(song) ?: return
        val sessionKey = account.sessionKey ?: return
        scope.launch {
            try {
                account.api.call("track.updateNowPlaying", params, sessionKey, post = true)
            } catch (e: LastFmException) {
                Log.w(TAG, "Now playing failed: ${e.message}")
                if (e.code == ERROR_INVALID_SESSION) account.disconnect()
            }
        }
    }

    /** Queues a play (synchronously, so it survives the process dying) and tries to send the queue. */
    fun scrobble(song: Song, startedAtSec: Long) {
        if (!active) return
        val entry = pendingScrobble(song, startedAtSec) ?: return
        synchronized(this) { save((load() + entry).takeLast(MAX_PENDING)) }
        flush()
    }

    /** Sends queued plays, if any. */
    fun flush() {
        if (!active || load().isEmpty()) return
        scope.launch { mutex.withLock { sendPending() } }
    }

    private suspend fun sendPending() {
        while (true) {
            val sessionKey = account.sessionKey ?: return
            val batch = synchronized(this) { load() }.take(BATCH_SIZE)
            if (batch.isEmpty()) return
            try {
                val response = account.api.call("track.scrobble", scrobbleParams(batch), sessionKey, post = true)
                acceptedCount(response, batch.size).takeIf { it > 0 }?.let { _accepted.tryEmit(it) }
            } catch (e: LastFmException) {
                when {
                    e.code == ERROR_INVALID_SESSION -> { account.disconnect(); return }
                    // Offline, Last.fm down or rate limited: keep them for later.
                    e.code == 0 || e.code in RETRYABLE -> { Log.w(TAG, "Scrobble deferred: ${e.message}"); return }
                    // Rejected (bad parameters): drop the batch rather than retry forever.
                    else -> Log.w(TAG, "Scrobbles rejected: ${e.message}")
                }
            }
            synchronized(this) { save(load().drop(batch.size)) }
        }
    }

    private fun load(): List<PendingScrobble> =
            prefs.getString(KEY_PENDING, null)?.let { runCatching { json.decodeFromString(listSerializer, it) }.getOrNull() } ?: emptyList()

    private fun save(list: List<PendingScrobble>) =
            prefs.edit(commit = true) { if (list.isEmpty()) remove(KEY_PENDING) else putString(KEY_PENDING, json.encodeToString(listSerializer, list)) }

    companion object {
        private const val TAG = "LastFmScrobbler"
        /** Same file as the account, so disconnecting (which clears it) drops the queue too. */
        private const val PREFS = "lastfm"
        private const val KEY_PENDING = "pending_scrobbles"
        private const val BATCH_SIZE = 50
        private const val MAX_PENDING = 1000
        private const val ERROR_INVALID_SESSION = 9
        /** 11 service offline, 16 temporarily unavailable, 29 rate limit exceeded. */
        private val RETRYABLE = setOf(11, 16, 29)

        /** MediaStore's placeholder for missing tags. */
        private fun String.known(): String? = trim().takeIf { it.isNotEmpty() && it != "<unknown>" }

        /** A play to scrobble, or null when the song lacks the artist or title Last.fm requires. */
        fun pendingScrobble(song: Song, startedAtSec: Long): PendingScrobble? {
            val artist = song.artistName.known() ?: return null
            val track = song.title.known() ?: return null
            return PendingScrobble(artist, track, song.albumName.known(), (song.duration / 1000).takeIf { it > 0 },
                    song.track.takeIf { it > 0 }, startedAtSec)
        }

        /** track.updateNowPlaying parameters; null when the song can't be scrobbled. */
        fun trackParams(song: Song): Map<String, String>? {
            val p = pendingScrobble(song, 0) ?: return null
            return buildMap {
                put("artist", p.artist)
                put("track", p.track)
                p.album?.let { put("album", it) }
                p.durationSec?.let { put("duration", it.toString()) }
                p.trackNumber?.let { put("trackNumber", it.toString()) }
            }
        }

        /** The number of scrobbles a track.scrobble response accepted (`scrobbles.@attr.accepted`), else [sent]. */
        fun acceptedCount(response: JsonObject, sent: Int): Int =
                (((response["scrobbles"] as? JsonObject)?.get("@attr") as? JsonObject)?.get("accepted") as? JsonPrimitive)
                        ?.contentOrNull?.toIntOrNull() ?: sent

        /** track.scrobble parameters for a batch: artist[0], track[0], timestamp[0], … */
        fun scrobbleParams(batch: List<PendingScrobble>): Map<String, String> = buildMap {
            batch.forEachIndexed { i, s ->
                put("artist[$i]", s.artist)
                put("track[$i]", s.track)
                put("timestamp[$i]", s.timestamp.toString())
                s.album?.let { put("album[$i]", it) }
                s.durationSec?.let { put("duration[$i]", it.toString()) }
                s.trackNumber?.let { put("trackNumber[$i]", it.toString()) }
            }
        }
    }
}
