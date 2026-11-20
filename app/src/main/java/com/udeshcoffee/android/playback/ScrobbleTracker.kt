package com.udeshcoffee.android.playback

import com.udeshcoffee.android.model.Song

/**
 * Tracks how long the current song has actually played, to apply the scrobbling rule (ListenBrainz and Last.fm alike): a
 * track longer than 30 seconds counts once it has played for half its length or 4 minutes,
 * whichever comes first. Pauses and seeks don't count as listening; repeats count again (call
 * [start] for each play). Times are passed in so this is testable: [wallMs] is
 * System.currentTimeMillis (for the scrobble's timestamp), [clockMs] a monotonic clock.
 */
class ScrobbleTracker {

    /** One play to scrobble: the song and when it started playing (Unix seconds). */
    data class Play(val song: Song, val startedAtSec: Long)

    private var song: Song? = null
    private var startedAtSec = 0L
    private var playedMs = 0L
    private var playingSince: Long? = null
    private var scrobbled = false
    private var hasPlayed = false

    /** A new play of [song] (null when the queue empties). */
    fun start(song: Song?, playing: Boolean, wallMs: Long, clockMs: Long) {
        this.song = song
        playedMs = 0
        playingSince = null
        scrobbled = false
        hasPlayed = false
        if (playing) setPlaying(true, wallMs, clockMs)
    }

    /** The scrobble's timestamp is when the song first started playing, not when it was queued up paused. */
    fun setPlaying(playing: Boolean, wallMs: Long, clockMs: Long) {
        val since = playingSince
        if (playing && since == null && song != null) {
            playingSince = clockMs
            if (!hasPlayed) { hasPlayed = true; startedAtSec = wallMs / 1000 }
        } else if (!playing && since != null) {
            playedMs += clockMs - since
            playingSince = null
        }
    }

    fun listenedMs(clockMs: Long): Long = playedMs + (playingSince?.let { clockMs - it } ?: 0)

    /** The current play, once it has been listened to long enough; returned at most once per play. */
    fun due(clockMs: Long): Play? {
        val s = song ?: return null
        if (scrobbled) return null
        val needed = threshold(s.duration) ?: return null
        if (listenedMs(clockMs) < needed) return null
        scrobbled = true
        return Play(s, startedAtSec)
    }

    companion object {
        const val MIN_DURATION_MS = 30_000L
        const val MAX_THRESHOLD_MS = 240_000L

        /** Listening time needed to scrobble a track of [durationMs]; null if it's too short to scrobble (or unknown). */
        fun threshold(durationMs: Long): Long? =
                if (durationMs <= MIN_DURATION_MS) null else minOf(durationMs / 2, MAX_THRESHOLD_MS)
    }
}
