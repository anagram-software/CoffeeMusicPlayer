package com.udeshcoffee.android.data.lastfm

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastFmStatsParserTest {

    private fun obj(s: String): JsonObject = Json.parseToJsonElement(s).jsonObject

    private val info = obj("""
        {"user":{"name":"rj","realname":"","url":"https://www.last.fm/user/rj","playcount":"150316",
         "artist_count":"9012","track_count":"40000","album_count":"15000",
         "image":[{"size":"small","#text":"https://img/s.png"},{"size":"extralarge","#text":"https://img/xl.png"},{"size":"mega","#text":""}],
         "registered":{"unixtime":"1037793040","#text":1037793040}}}
    """)

    @Test
    fun `parses user info with string numbers`() {
        val s = LastFmStatsParser.stats(info, obj("""{"topalbums":{"album":[]}}"""), obj("""{"topartists":{"artist":[]}}"""),
                obj("""{"toptracks":{"track":[]}}"""))
        assertEquals("rj", s.name)
        assertNull(s.realName)
        assertEquals(150316L, s.scrobbles)
        assertEquals(9012L, s.artists)
        assertEquals(40000L, s.tracks)
        assertEquals(1037793040L, s.registeredSec)
        assertEquals("https://img/xl.png", s.imageUrl)
        assertNull(s.topAlbum)
        assertNull(s.topTrack)
        assertEquals(emptyList<TopItem>(), s.topArtists)
    }

    @Test
    fun `parses the top album and track, including a single item given as an object`() {
        val albums = obj("""{"topalbums":{"album":[{"name":"LP","playcount":"42","artist":{"name":"A"},
            "image":[{"size":"large","#text":"https://img/lp.png"}]}]}}""")
        val tracks = obj("""{"toptracks":{"track":{"name":"Song","playcount":"3","artist":{"name":"A"},
            "image":[{"size":"large","#text":"https://img/2a96cbd8b46e442fc41c2b86b821562f.png"}]}}}""")
        val artists = obj("""{"topartists":{"artist":[{"name":"A","playcount":"42"},{"name":"B","playcount":"7"},{"name":"C","playcount":"1"}]}}""")
        val s = LastFmStatsParser.stats(info, albums, artists, tracks, limit = 2)
        assertEquals(TopItem("LP", "A", 42, "https://img/lp.png"), s.topAlbum)
        assertEquals(TopItem("Song", "A", 3, null), s.topTrack)
        assertEquals(listOf(TopItem("Song", "A", 3, null)), s.topTracks)
        assertEquals(listOf(TopItem("A", null, 42), TopItem("B", null, 7)), s.topArtists)
    }

    @Test
    fun `track artwork comes from its album, ignoring the placeholder`() {
        assertEquals("https://img/b.png", LastFmStatsParser.trackArtwork(obj(
                """{"track":{"album":{"image":[{"#text":"https://img/a.png"},{"#text":"https://img/b.png"}]}}}""")))
        assertNull(LastFmStatsParser.trackArtwork(obj(
                """{"track":{"album":{"image":[{"#text":"https://x/2a96cbd8b46e442fc41c2b86b821562f.png"}]}}}""")))
        assertNull(LastFmStatsParser.trackArtwork(obj("""{"track":{"name":"x"}}""")))
    }

    @Test
    fun `all-time top artist`() {
        val response = obj("""{"topartists":{"artist":{"name":"A","playcount":"1500","image":[{"#text":"https://x/2a96cbd8b46e442fc41c2b86b821562f.png"}]}}}""")
        assertEquals(listOf(TopItem("A", null, 1500)), LastFmStatsParser.topArtists(response, 1))
    }

    @Test
    fun `top album image skips the placeholder`() {
        assertEquals("https://img/lp.png", LastFmStatsParser.topAlbumImage(obj(
                """{"topalbums":{"album":[{"name":"LP","image":[{"#text":"https://img/lp.png"}]}]}}""")))
        assertNull(LastFmStatsParser.topAlbumImage(obj(
                """{"topalbums":{"album":{"name":"LP","image":[{"#text":"https://x/2a96cbd8b46e442fc41c2b86b821562f.png"}]}}}""")))
    }
}
