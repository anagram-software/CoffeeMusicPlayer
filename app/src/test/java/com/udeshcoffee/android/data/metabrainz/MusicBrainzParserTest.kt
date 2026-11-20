package com.udeshcoffee.android.data.metabrainz

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicBrainzParserTest {

    @Test
    fun `query quotes and escapes the title and artist`() {
        assertEquals("recording:\"Say \\\"Hi\\\"\" AND artist:\"Muse\"", MusicBrainzParser.query("Say \"Hi\"", " Muse "))
        assertEquals("recording:\"Airbag\"", MusicBrainzParser.query("Airbag", " "))
    }

    @Test
    fun `best match prefers the original official album`() {
        val response = Json.parseToJsonElement("""{"recordings":[{
            "id":"rec","score":100,"title":"Karma Police","first-release-date":"1997-05-21",
            "artist-credit":[{"name":"Radiohead","joinphrase":" & "},{"name":"Friends"}],
            "tags":[{"count":1,"name":"rock"},{"count":5,"name":"alternative rock"}],
            "releases":[
              {"id":"comp","title":"Best Of","status":"Official","date":"2008","release-group":{"id":"rg-comp","primary-type":"Album","secondary-types":["Compilation"]}},
              {"id":"ok","title":"OK Computer","status":"Official","date":"1997-06-16","release-group":{"id":"rg-ok","primary-type":"Album"}},
              {"id":"single","title":"Karma Police","status":"Official","date":"1997-08-25","release-group":{"id":"rg-s","primary-type":"Single"}}]
            }]}""").jsonObject
        val m = MusicBrainzParser.bestMatch(response)!!
        assertEquals("Karma Police", m.title)
        assertEquals("Radiohead & Friends", m.artist)
        assertEquals("OK Computer", m.album)
        assertEquals(1997, m.year)
        assertEquals("Alternative rock", m.genre)
        assertEquals("https://coverartarchive.org/release-group/rg-ok/front-500", m.artworkUrl)
    }

    @Test
    fun `no recordings, no match`() {
        assertNull(MusicBrainzParser.bestMatch(Json.parseToJsonElement("""{"recordings":[]}""").jsonObject))
    }

    @Test
    fun `listenbrainz lookup plus the release gives year, genre and cover`() {
        val lookup = Json.parseToJsonElement("""{"artist_credit_name":"Radiohead","artist_mbids":["a"],"recording_mbid":"rec",
            "recording_name":"Karma Police","release_mbid":"rel","release_name":"OK Computer"}""").jsonObject
        val release = Json.parseToJsonElement("""{"title":"OK Computer","date":"2009-03-24",
            "release-group":{"id":"rg","first-release-date":"1997-05-21","genres":[{"name":"rock","count":3},{"name":"art rock","count":9}]}}""").jsonObject
        assertEquals("rel", MusicBrainzParser.lookupRelease(lookup))
        val m = MusicBrainzParser.fromLookup(lookup, release)!!
        assertEquals(TrackMetadata("Karma Police", "Radiohead", "OK Computer", "Art rock", 1997,
                "https://coverartarchive.org/release-group/rg/front-500"), m)
        // Without the release: the release's own cover, no year or genre.
        assertEquals("https://coverartarchive.org/release/rel/front-500", MusicBrainzParser.fromLookup(lookup, null)!!.artworkUrl)
    }

    @Test
    fun `an empty lookup is no match`() {
        assertNull(MusicBrainzParser.lookupRelease(Json.parseToJsonElement("{}").jsonObject))
    }
}
