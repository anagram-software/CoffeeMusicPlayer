package com.udeshcoffee.android.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import com.udeshcoffee.android.data.subsonic.toSong

class QueueStoreTest {

    @Test
    fun `encodes ids as reverse hex terminated by semicolons`() {
        // 26 = 0x1a -> least significant digit first.
        assertEquals("a1;0;f;", QueueStore.encode(listOf(26, 0, 15)))
    }

    @Test
    fun `skips negative ids`() {
        assertEquals("1;", QueueStore.encode(listOf(-5, 1)))
    }

    @Test
    fun `decodes what it encodes`() {
        val ids = listOf(0L, 1, 26, 4096, 123_456_789, Long.MAX_VALUE)
        assertEquals(ids, QueueStore.decode(QueueStore.encode(ids)))
    }

    @Test
    fun `decodes queues saved by older releases`() {
        assertEquals(listOf(26L, 255L), QueueStore.decode("a1;ff;"))
    }

    @Test
    fun `ignores an unterminated trailing id`() {
        assertEquals(listOf(26L), QueueStore.decode("a1;3"))
        assertEquals(emptyList<Long>(), QueueStore.decode(""))
    }

    @Test
    fun `mixed queues keep device ids and server song details`() {
        val local = com.udeshcoffee.android.song(26)
        val remote = com.udeshcoffee.android.data.subsonic.SubSong(
                id = "s1", title = "Butch", album = "Imagine Us", albumId = "al1", artist = "Back On Earth",
                artistId = "ar1", coverArt = "mf-s1", duration = 192, track = 3, discNumber = 2, year = 2015)
        val entries = QueueStore.decodeEntries(QueueStore.encodeEntries(listOf(local, remote.toSong())))
        assertEquals(26L, entries[0].localId)
        val saved = entries[1].remote!!
        assertEquals("s1", saved.id)
        assertEquals("Butch", saved.title)
        assertEquals(192_000L, saved.durationMs)
        assertEquals(3, saved.track)
        assertEquals(2, saved.disc)
        assertEquals("mf-s1", saved.coverArt)
    }

    @Test
    fun `corrupt saved queues decode as empty`() {
        assertEquals(emptyList<QueueStore.Entry>(), QueueStore.decodeEntries("not json"))
    }
}
