package com.udeshcoffee.android.data.media

import com.udeshcoffee.android.data.model.AlbumStat
import com.udeshcoffee.android.song
import org.junit.Assert.assertEquals
import org.junit.Test

class OperatorsTest {

    private val songs = listOf(
            song(1, albumId = 10, artistId = 100),
            song(2, albumId = 20, artistId = 200),
            song(3, albumId = 10, artistId = 100),
            song(4, albumId = 30, artistId = 100))

    @Test
    fun `groups songs into albums in order of first appearance with counts`() {
        val albums = songsToAlbums(songs)
        assertEquals(listOf(10L, 20L, 30L), albums.map { it.id })
        assertEquals(listOf(2, 1, 1), albums.map { it.songCount })
        assertEquals("Album 10", albums.first().title)
    }

    @Test
    fun `groups songs into artists with counts`() {
        val artists = songsToArtists(songs)
        assertEquals(listOf(100L, 200L), artists.map { it.id })
        assertEquals(listOf(3, 1), artists.map { it.songCount })
    }

    @Test
    fun `orders albums by stats and leaves out albums without stats`() {
        val albums = songsToAlbums(songs)
        val stats = listOf(AlbumStat(30, 9, 0), AlbumStat(10, 3, 0), AlbumStat(99, 1, 0))
        assertEquals(listOf(30L, 10L), statsToAlbums(stats, albums).map { it.id })
    }

    @Test
    fun `empty input gives empty output`() {
        assertEquals(emptyList<Any>(), songsToAlbums(emptyList()))
        assertEquals(emptyList<Any>(), songsToArtists(emptyList()))
    }
}
