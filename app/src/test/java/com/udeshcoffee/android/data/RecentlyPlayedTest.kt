package com.udeshcoffee.android.data

import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.song
import com.udeshcoffee.android.utils.PlaylistPlays
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentlyPlayedTest {

    private fun album(id: Long) = Album(id, "Album $id", "Artist", 2000, 0)
    private fun playlist(id: Long) = Playlist(Playlist.USER, id, "Playlist $id")
    private fun played(id: Long, at: Long) = RecentItem.OfAlbum(album(id), at)

    @Test
    fun `device albums take their latest song's play time, in millis`() {
        val songs = listOf(
                song(1, albumId = 10).apply { lastplayed = 300 },
                song(2, albumId = 20).apply { lastplayed = 200 },
                song(3, albumId = 10).apply { lastplayed = 100 })
        val albums = recentAlbums(songs)
        assertEquals(listOf(10L, 20L), albums.map { it.album.id })
        assertEquals(listOf(300_000L, 200_000L), albums.map { it.playedAt })
    }

    @Test
    fun `played playlists are mixed in by time, unplayed ones left out`() {
        val p1 = playlist(1)
        val p2 = playlist(2)
        val plays = mapOf(PlaylistPlays.key(p1) to 250L)
        val merged = mergeRecent(listOf(played(10, 300), played(20, 200)), listOf(p1, p2), plays, count = 10)
        assertEquals(listOf("album:10", "playlist:${PlaylistPlays.key(p1)}", "album:20"), merged.map { it.key })
    }

    @Test
    fun `albums without a play time keep their order after timed items`() {
        val p1 = playlist(1)
        val merged = mergeRecent(listOf(played(10, 0), played(20, 0)), listOf(p1), mapOf(PlaylistPlays.key(p1) to 5L), count = 10)
        assertEquals(listOf("playlist:${PlaylistPlays.key(p1)}", "album:10", "album:20"), merged.map { it.key })
    }

    @Test
    fun `keeps at most count items`() {
        val merged = mergeRecent((1L..5L).map { played(it, 10 - it) }, emptyList(), emptyMap(), count = 3)
        assertEquals(listOf(1L, 2L, 3L), merged.map { (it as RecentItem.OfAlbum).album.id })
    }
}
