package com.udeshcoffee.android.data.metabrainz

import com.udeshcoffee.android.song
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenBrainzScrobblerTest {

    @Test
    fun `songs without a known artist or title aren't submitted`() {
        assertNull(ListenBrainzScrobbler.pendingListen(song(1, artist = "<unknown>"), 0))
        assertNull(ListenBrainzScrobbler.pendingListen(song(1, title = " "), 0))
    }

    @Test
    fun `unknown album is left out`() {
        val l = ListenBrainzScrobbler.pendingListen(song(1, album = "<unknown>", duration = 185_400, track = 3), 1234)!!
        assertNull(l.release)
        assertEquals(185_400L, l.durationMs)
        assertEquals(3, l.trackNumber)
        assertEquals(1234L, l.listenedAt)
    }

    @Test
    fun `single and import listens carry a timestamp and metadata`() {
        val body = ListenBrainzScrobbler.submission("import", listOf(
                PendingListen("A", "One", "Album", 200_000, 1, 100), PendingListen("B", "Two", listenedAt = 200)))
        assertEquals("import", body["listen_type"]!!.jsonPrimitive.content)
        val payload = body["payload"] as JsonArray
        assertEquals(2, payload.size)
        val first = payload[0] as JsonObject
        assertEquals(100L, first["listened_at"]!!.jsonPrimitive.long)
        val meta = first["track_metadata"] as JsonObject
        assertEquals("A", meta["artist_name"]!!.jsonPrimitive.content)
        assertEquals("One", meta["track_name"]!!.jsonPrimitive.content)
        assertEquals("Album", meta["release_name"]!!.jsonPrimitive.content)
        val info = meta["additional_info"] as JsonObject
        assertEquals(200_000L, info["duration_ms"]!!.jsonPrimitive.long)
        assertEquals(1L, info["tracknumber"]!!.jsonPrimitive.long)
        assertFalse(((payload[1] as JsonObject)["track_metadata"] as JsonObject).containsKey("release_name"))
    }

    @Test
    fun `playing now has no timestamp`() {
        val listen = (ListenBrainzScrobbler.submission("playing_now", listOf(PendingListen("A", "One", listenedAt = 0)))["payload"] as JsonArray)[0] as JsonObject
        assertFalse(listen.containsKey("listened_at"))
        assertTrue(listen.containsKey("track_metadata"))
    }
}
