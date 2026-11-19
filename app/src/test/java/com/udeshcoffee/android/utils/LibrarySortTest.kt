package com.udeshcoffee.android.utils

import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.ui.compose.components.SortState
import com.udeshcoffee.android.ui.compose.components.artistNaturalAscending
import com.udeshcoffee.android.ui.compose.components.playlistNaturalAscending
import com.udeshcoffee.android.ui.compose.components.sortedArtists
import com.udeshcoffee.android.ui.compose.components.sortedPlaylists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySortTest {

    private fun playlist(id: Long, title: String, songs: Int, created: Long) =
            Playlist(Playlist.USER, id, title, remoteId = "p$id").also { it.songCount = songs; it.created = created }

    private val downloads = Playlist(Playlist.DOWNLOADS)
    private val a = playlist(1, "Chill", songs = 5, created = 30)
    private val b = playlist(2, "Anthems", songs = 40, created = 10)
    private val c = playlist(3, "Bangers", songs = 12, created = 20)
    private val all = listOf(downloads, a, b, c)
    private val played = mapOf(PlaylistPlays.key(b) to 300L, PlaylistPlays.key(c) to 100L)

    private fun sort(order: Int) =
            all.sortedPlaylists(SortState(order, playlistNaturalAscending(order))) { played[PlaylistPlays.key(it)] }.map { it.title }

    @Test
    fun `playlists in their natural direction, smart playlists on top`() {
        val smart = downloads.title
        // Never played counts as oldest.
        assertEquals(listOf(smart, "Anthems", "Bangers", "Chill"), sort(SortManager.PlaylistSort.RECENTLY_PLAYED))
        assertEquals(listOf(smart, "Anthems", "Bangers", "Chill"), sort(SortManager.PlaylistSort.NAME))
        assertEquals(listOf(smart, "Anthems", "Bangers", "Chill"), sort(SortManager.PlaylistSort.SONG_COUNT))
        assertEquals(listOf(smart, "Chill", "Bangers", "Anthems"), sort(SortManager.PlaylistSort.RECENTLY_ADDED))
    }

    @Test
    fun `ascending reverses the user's playlists only`() {
        val sorted = all.sortedPlaylists(SortState(SortManager.PlaylistSort.SONG_COUNT, ascending = true)) { null }
        assertEquals(listOf(downloads, a, c, b), sorted)
    }

    @Test
    fun `artists alphabetical, or most songs or albums first`() {
        val artists = listOf(Artist(1, "Coldplay").also { it.songCount = 3 }, Artist(2, "ABBA").also { it.songCount = 9 },
                Artist(3, "Bon Iver").also { it.songCount = 1 })
        assertTrue(artistNaturalAscending(SortManager.ArtistSort.NAME))
        assertFalse(artistNaturalAscending(SortManager.ArtistSort.COUNT))
        assertEquals(listOf("ABBA", "Bon Iver", "Coldplay"), artists.sortedArtists(SortState(SortManager.ArtistSort.NAME, true)).map { it.name })
        assertEquals(listOf("ABBA", "Coldplay", "Bon Iver"), artists.sortedArtists(SortState(SortManager.ArtistSort.COUNT, false)).map { it.name })
    }
}
