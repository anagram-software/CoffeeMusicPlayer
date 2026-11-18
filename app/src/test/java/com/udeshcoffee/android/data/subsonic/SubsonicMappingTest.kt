package com.udeshcoffee.android.data.subsonic

import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicMappingTest {

    // Trimmed from a real Navidrome getAlbum response (extra fields must be ignored).
    private val albumJson = """
        {"subsonic-response":{"status":"ok","album":{"id":"al1","name":"Imagine Us","artist":"Back On Earth","artistId":"ar1",
         "coverArt":"al-al1_abc","songCount":2,"year":2015,"genres":[],"isCompilation":false,
         "song":[
          {"id":"s1","title":"Butch","album":"Imagine Us","albumId":"al1","artist":"Back On Earth","artistId":"ar1",
           "coverArt":"mf-s1_abc","duration":192,"track":3,"discNumber":2,"year":2015,"suffix":"mp3","bitRate":192,
           "created":"2025-03-26T22:26:49.348640872Z","replayGain":{"trackGain":-1.5},"isDir":false},
          {"id":"d1","title":"A folder","isDir":true}
         ]}}}
    """.trimIndent()

    private fun album(): SubAlbum {
        val body = SubsonicClient.parse(albumJson)
        return SubsonicClient.json.decodeFromJsonElement(SubAlbum.serializer(), body["album"]!!.jsonObject)
    }

    @Test
    fun `maps server songs to app songs`() {
        val songs = album().song.toSongs()
        assertEquals("folders are skipped", 1, songs.size)
        val song = songs.single()
        assertEquals("s1", song.remoteId)
        assertEquals(RemoteIds.song("s1"), song.id)
        assertEquals("Butch", song.title)
        assertEquals(192_000L, song.duration)
        assertEquals(3, song.track)
        assertEquals(2, song.discNumber)
        assertEquals("mf-s1_abc", song.coverArt)
        assertEquals("al1", song.albumRemoteId)
        assertEquals(RemoteIds.album("al1"), song.albumId)
        assertEquals(1743028009, song.dateAdded)
        assertTrue(song.isRemote)
    }

    @Test
    fun `maps albums`() {
        val album = album().toAlbum()
        assertEquals("al1", album.remoteId)
        assertEquals("Imagine Us", album.title)
        assertEquals(2, album.songCount)
        assertEquals(2015, album.year)
    }

    @Test
    fun `remote ids are negative, stable and distinct per kind`() {
        val id = RemoteIds.song("0uTqxDgHNaihj2pIciP0h8")
        assertTrue(id < 0)
        assertEquals(id, RemoteIds.song("0uTqxDgHNaihj2pIciP0h8"))
        assertNotEquals(id, RemoteIds.album("0uTqxDgHNaihj2pIciP0h8"))
        assertNotEquals(id, RemoteIds.song("0uTqxDgHNaihj2pIciP0h9"))
    }
}
