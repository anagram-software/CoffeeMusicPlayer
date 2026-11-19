package com.udeshcoffee.android.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import com.udeshcoffee.android.data.subsonic.toSong

class QueueStoreTest {

    @Test
    fun `queues keep device ids and server song details`() {
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
    fun `entries restore only for their music source`() {
        val device = QueueStore.Entry(localId = 26)
        val server = QueueStore.Entry(remote = QueueStore.RemoteSong(id = "s1", title = "Butch", album = "Imagine Us", artist = "Back On Earth"))
        assertEquals(true, device.belongsTo(com.udeshcoffee.android.data.source.MusicSource.DEVICE))
        assertEquals(false, device.belongsTo(com.udeshcoffee.android.data.source.MusicSource.SERVER))
        assertEquals(true, server.belongsTo(com.udeshcoffee.android.data.source.MusicSource.SERVER))
        assertEquals(false, server.belongsTo(com.udeshcoffee.android.data.source.MusicSource.DEVICE))
    }

    @Test
    fun `corrupt saved queues decode as empty`() {
        assertEquals(emptyList<QueueStore.Entry>(), QueueStore.decodeEntries("not json"))
    }
}
