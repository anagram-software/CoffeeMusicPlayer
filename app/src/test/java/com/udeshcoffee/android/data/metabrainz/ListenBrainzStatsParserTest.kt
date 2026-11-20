package com.udeshcoffee.android.data.metabrainz

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListenBrainzStatsParserTest {

    private fun obj(s: String): JsonObject = Json.parseToJsonElement(s).jsonObject
    private val user = ListenBrainzUser("rob")

    private val allArtists = obj("""{"payload":{"artists":[{"artist_name":"Muse","artist_mbid":"x","listen_count":1500}],"total_artist_count":900}}""")
    private val allAlbums = obj("""{"payload":{"release_groups":[
        {"release_group_name":"OK Computer","artist_name":"Radiohead","listen_count":300,"caa_id":42,"caa_release_mbid":"r1"},
        {"release_group_name":"Absolution","artist_name":"Muse","listen_count":200,"caa_id":7,"caa_release_mbid":"r2"}],
        "total_release_group_count":500}}""")
    private val allTracks = obj("""{"payload":{"recordings":[],"total_recording_count":4000}}""")

    @Test
    fun `parses totals and top items`() {
        val weekTracks = obj("""{"payload":{"recordings":[
            {"track_name":"Hysteria","artist_name":"Muse","listen_count":12,"release_mbid":"rel"},
            {"track_name":"Airbag","artist_name":"Radiohead","listen_count":9,"caa_id":5,"caa_release_mbid":"r9"}]}}""")
        val s = ListenBrainzStatsParser.stats(user, obj("""{"payload":{"count":12345}}"""), allArtists, allAlbums, allTracks,
                allArtists, allAlbums, weekTracks)
        assertEquals(12345L, s.listens)
        assertEquals(900L, s.artists)
        assertEquals(500L, s.albums)
        assertEquals(4000L, s.tracks)
        assertEquals("https://listenbrainz.org/user/rob/", s.profileUrl)
        assertEquals(TopItem("Muse", null, 1500), s.topArtistAllTime)
        assertEquals(TopItem("OK Computer", "Radiohead", 300, "https://coverartarchive.org/release/r1/42-500.jpg"), s.topAlbum)
        // Without caa fields, the release's front cover.
        assertEquals("https://coverartarchive.org/release/rel/front-500", s.topTrack?.imageUrl)
        assertEquals("https://coverartarchive.org/release/r9/5-500.jpg", s.topTracks[1].imageUrl)
    }

    @Test
    fun `stats not calculated yet (204) are empty`() {
        val s = ListenBrainzStatsParser.stats(user, null, null, null, null, null, null, null)
        assertEquals(0L, s.listens)
        assertNull(s.topAlbum)
        assertNull(s.topArtistAllTime)
        assertEquals(emptyList<TopItem>(), s.topTracks)
    }

    @Test
    fun `top artist cover comes from their most listened album`() {
        assertEquals("https://coverartarchive.org/release/r2/7-500.jpg", ListenBrainzStatsParser.coverByArtist(allAlbums, "muse"))
        assertNull(ListenBrainzStatsParser.coverByArtist(allAlbums, "Björk"))
    }
}
