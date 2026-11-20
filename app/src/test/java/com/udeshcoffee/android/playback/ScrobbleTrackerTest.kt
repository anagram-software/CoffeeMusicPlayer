package com.udeshcoffee.android.playback

import com.udeshcoffee.android.song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScrobbleTrackerTest {

    @Test
    fun `threshold is half the track, capped at four minutes, and short tracks never count`() {
        assertEquals(90_000L, ScrobbleTracker.threshold(180_000))
        assertEquals(240_000L, ScrobbleTracker.threshold(600_000))
        assertNull(ScrobbleTracker.threshold(30_000))
        assertNull(ScrobbleTracker.threshold(0))
    }

    @Test
    fun `counts only time spent playing`() {
        val t = ScrobbleTracker()
        t.start(song(1, duration = 200_000), playing = true, wallMs = 1_000_000, clockMs = 0)
        assertNull(t.due(60_000))
        t.setPlaying(false, wallMs = 1_060_000, clockMs = 60_000)
        // Paused for a long time: still not enough listening.
        assertNull(t.due(500_000))
        t.setPlaying(true, wallMs = 1_500_000, clockMs = 500_000)
        val play = t.due(540_000)
        assertNotNull(play)
        // Timestamp is when it first started playing.
        assertEquals(1_000L, play!!.startedAtSec)
    }

    @Test
    fun `due only once per play, again after a restart`() {
        val t = ScrobbleTracker()
        val s = song(1, duration = 60_000)
        t.start(s, playing = true, wallMs = 0, clockMs = 0)
        assertNotNull(t.due(30_000))
        assertNull(t.due(60_000))
        t.start(s, playing = true, wallMs = 60_000, clockMs = 60_000)
        assertNotNull(t.due(90_000))
    }

    @Test
    fun `start time is set when playback begins, not when queued paused`() {
        val t = ScrobbleTracker()
        t.start(song(1, duration = 60_000), playing = false, wallMs = 0, clockMs = 0)
        t.setPlaying(true, wallMs = 120_000, clockMs = 120_000)
        assertEquals(120L, t.due(150_000)?.startedAtSec)
    }
}
