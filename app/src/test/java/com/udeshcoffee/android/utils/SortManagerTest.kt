package com.udeshcoffee.android.utils

import com.udeshcoffee.android.song
import com.udeshcoffee.android.utils.SortManager.SongSort
import org.junit.Assert.assertEquals
import org.junit.Test

class SortManagerTest {

    @Test
    fun `track order sorts by disc then track`() {
        // MediaStore encodes disc 2 track 1 as 2001.
        val songs = arrayListOf(song(1, track = 2001), song(2, track = 2), song(3, track = 1), song(4, track = 1003))
        SortManager.sortSongs(songs, SongSort.TRACK_NUMBER)
        assertEquals(listOf(3L, 2L, 4L, 1L), songs.map { it.id })
    }

    @Test
    fun `name order is alphabetical`() {
        val songs = arrayListOf(song(1, title = "Charlie"), song(2, title = "alpha"), song(3, title = "Bravo"))
        SortManager.sortSongs(songs, SongSort.NAME)
        assertEquals(listOf("alpha", "Bravo", "Charlie"), songs.map { it.title })
    }

    @Test
    fun `date order puts the newest first`() {
        val songs = arrayListOf(song(1, dateAdded = 5), song(2, dateAdded = 9), song(3, dateAdded = 1))
        SortManager.sortSongs(songs, SongSort.DATE)
        assertEquals(listOf(2L, 1L, 3L), songs.map { it.id })
    }

    @Test
    fun `album order groups by album and keeps track order inside`() {
        val songs = arrayListOf(
                song(1, album = "B", track = 2), song(2, album = "A", track = 2),
                song(3, album = "B", track = 1), song(4, album = "A", track = 1))
        SortManager.sortSongs(songs, SongSort.ALBUM_NAME)
        assertEquals(listOf(4L, 2L, 3L, 1L), songs.map { it.id })
    }
}
