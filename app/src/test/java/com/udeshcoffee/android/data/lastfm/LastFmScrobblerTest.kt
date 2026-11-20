package com.udeshcoffee.android.data.lastfm

import com.udeshcoffee.android.song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastFmScrobblerTest {

    @Test
    fun `songs without a known artist or title aren't scrobbled`() {
        assertNull(LastFmScrobbler.pendingScrobble(song(1, artist = "<unknown>"), 0))
        assertNull(LastFmScrobbler.pendingScrobble(song(1, title = " "), 0))
    }

    @Test
    fun `unknown album is left out`() {
        val p = LastFmScrobbler.pendingScrobble(song(1, album = "<unknown>", duration = 185_400), 1234)!!
        assertNull(p.album)
        assertEquals(185L, p.durationSec)
        assertEquals(1234L, p.timestamp)
    }

    @Test
    fun `batch parameters are indexed`() {
        val batch = listOf(
                PendingScrobble("A", "One", "Album", 200, null, 100),
                PendingScrobble("B", "Two", null, null, 3, 200))
        assertEquals(mapOf(
                "artist[0]" to "A", "track[0]" to "One", "timestamp[0]" to "100", "album[0]" to "Album", "duration[0]" to "200",
                "artist[1]" to "B", "track[1]" to "Two", "timestamp[1]" to "200", "trackNumber[1]" to "3"),
                LastFmScrobbler.scrobbleParams(batch))
    }

    @Test
    fun `now playing parameters`() {
        val params = LastFmScrobbler.trackParams(song(1, title = "T", artist = "A", album = "Al", duration = 120_000))!!
        assertEquals("T", params["track"]); assertEquals("A", params["artist"]); assertEquals("Al", params["album"]); assertEquals("120", params["duration"])
    }
}
