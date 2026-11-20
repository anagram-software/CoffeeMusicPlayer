package com.udeshcoffee.android.data.subsonic

import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalTest {

    private var nextId = 1L

    private fun album(title: String, artist: String = "Muse", songs: Int = 10, year: Int = 2000, remoteId: String = "al-${nextId}") =
            Album(nextId++, title, artist, year, 0, remoteId = remoteId).also { it.songCount = songs }

    private fun ext(title: String, artist: String = "Muse", songs: Int = 10, year: Int = 2000) =
            album(title, artist, songs, year, remoteId = "ext-qobuz-album-${nextId}")

    @Test
    fun `recognises Octo-Fiesta ids and providers`() {
        assertTrue(isExternalId("ext-qobuz-song-123"))
        assertFalse(isExternalId("a1b2c3"))
        assertFalse(isExternalId(null))
        assertEquals("Qobuz", externalProvider("ext-qobuz-album-1"))
        assertEquals("Spotify", externalProvider("ext-spotify-album-1"))
        assertNull(externalProvider("abc"))
    }

    @Test
    fun `spots covers and karaoke`() {
        assertTrue(looksLikeCover("Hysteria (Karaoke Version)"))
        assertTrue(looksLikeCover("Uprising", "A Tribute to Muse"))
        assertFalse(looksLikeCover("Absolution", "Muse"))
    }

    @Test
    fun `merges editions keeping the biggest`() {
        val plain = album("Origin of Symmetry", songs = 11)
        val deluxe = album("Origin of Symmetry (Deluxe Edition)", songs = 15)
        val other = album("Showbiz")
        assertEquals(listOf(deluxe, other), mergeEditions(listOf(plain, deluxe, other)))
    }

    @Test
    fun `library releases are grouped by kind, provider releases kept apart`() {
        val lp = album("Absolution", songs = 14)
        val ep = album("Dead Inside", songs = 2)
        val comp = album("Now 2004", artist = "Various Artists")
        val extLp = ext("Drones", year = 2015)
        val extNewer = ext("Simulation Theory", year = 2018)
        val extSingle = ext("Won't Stand Down", songs = 1, year = 2022)
        val karaoke = ext("Muse Karaoke Hits")

        val r = groupReleases(listOf(extSingle, lp, karaoke, ep, extLp, comp, extNewer), "Muse")

        assertEquals(listOf(lp), r.albums)
        assertEquals(listOf(ep), r.singles)
        assertEquals(listOf(comp), r.appearsOn)
        assertEquals(listOf(extNewer, extLp, extSingle), r.provider)
        assertEquals(1, r.hiddenCovers)
        assertEquals(3, r.libraryCount)
        assertEquals("Qobuz", r.providerName)
    }

    @Test
    fun `only appearances are shown as albums`() {
        val comp = album("Now 2004", artist = "Various Artists")
        val r = groupReleases(listOf(comp), "Muse")
        assertEquals(listOf(comp), r.albums)
        assertTrue(r.appearsOn.isEmpty())
    }

    @Test
    fun `album tracks split into library and provider`() {
        val local = song(1).copy(remoteId = "s1")
        val missing = song(2).copy(remoteId = "ext-qobuz-song-2")
        val tracks = splitAlbumTracks(listOf(local, missing))
        assertEquals(listOf(local), tracks.library)
        assertEquals(listOf(missing), tracks.provider)
        assertEquals(listOf(local), tracks.playable)
        // A provider album plays its own tracks.
        assertEquals(listOf(missing), splitAlbumTracks(listOf(missing)).playable)
    }

    @Test
    fun `add to library stars the provider album`() {
        val missing = song(2).copy(remoteId = "ext-qobuz-song-2", albumRemoteId = "ext-qobuz-album-9")
        assertEquals("ext-qobuz-album-1", externalAlbumId("ext-qobuz-album-1", listOf(missing)))
        assertEquals("ext-qobuz-album-9", externalAlbumId("al-1", listOf(missing)))
        // Missing tracks without a provider album: no album to star.
        assertNull(externalAlbumId("al-1", listOf(missing.copy(albumRemoteId = "al-1"))))
    }

    @Test
    fun `search results group into the library then each provider`() {
        val libSong = song(1).copy(remoteId = "s1")
        val qobuzSong = song(2).copy(remoteId = "ext-qobuz-song-2")
        val deezerSong = song(3).copy(remoteId = "ext-deezer-song-3")
        val libArtist = Artist(1, "Muse", "ar-1")
        val qobuzArtist = Artist(2, "Muse", "ext-qobuz-artist-2")
        val qobuzAlbum = ext("Drones")
        val groups = groupBySource(listOf(qobuzArtist, libArtist), listOf(qobuzAlbum), listOf(qobuzSong, libSong, deezerSong))
        assertEquals(listOf(null, "Qobuz", "Deezer"), groups.map { it.provider })
        assertEquals(SourceResults(null, listOf(libArtist), emptyList(), listOf(libSong)), groups[0])
        assertEquals(SourceResults("Qobuz", listOf(qobuzArtist), listOf(qobuzAlbum), listOf(qobuzSong)), groups[1])
        assertEquals(listOf(deezerSong), groups[2].songs)
    }

    @Test
    fun `the library group is there even when only providers have results`() {
        val groups = groupBySource(emptyList(), emptyList(), listOf(song(2).copy(remoteId = "ext-qobuz-song-2")))
        assertTrue(groups[0].provider == null && groups[0].isEmpty)
        assertEquals(1, groupBySource(emptyList(), emptyList(), listOf(song(1).copy(remoteId = "s1"))).size)
    }

    @Test
    fun `provider playlists (pl- ids) group under the provider, with their owner as artist`() {
        val libAlbum = album("Drones")
        val qobuzPlaylist = Album(500, "Chill Mix", "\uD83C\uDFB5 Qobuz qobuz", 0, 0, remoteId = "pl-qobuz-789").also { it.songCount = 40 }
        val curated = Album(501, "Rock Gems", "\uD83C\uDFB5 Qobuz Jane Doe", 0, 0, remoteId = "pl-qobuz-790")
        val groups = groupBySource(emptyList(), listOf(libAlbum, qobuzPlaylist, curated), emptyList())
        assertEquals(listOf(null, "Qobuz"), groups.map { it.provider })
        assertEquals(listOf(libAlbum), groups[0].albums)
        assertTrue(groups[1].albums.isEmpty())
        assertEquals(listOf("Qobuz", "Jane Doe"), groups[1].playlists.map { it.artist })
        assertEquals(40, groups[1].playlists[0].songCount)
        assertTrue(isExternalId("pl-qobuz-789") && isExternalPlaylistId("pl-qobuz-789") && !isExternalPlaylistId("ext-qobuz-album-1"))
        // Not something "Add to library" can star.
        assertNull(externalAlbumId("pl-qobuz-789", emptyList()))
    }
}
