package com.udeshcoffee.android.data.subsonic

import kotlinx.serialization.Serializable

/*
 * The parts of Subsonic / OpenSubsonic responses the app uses. Unknown fields are ignored and
 * everything optional has a default, since servers differ in what they send.
 */

@Serializable
data class SubSong(
        val id: String,
        val title: String = "",
        val album: String? = null,
        val albumId: String? = null,
        val artist: String? = null,
        val artistId: String? = null,
        val coverArt: String? = null,
        /** Seconds. */
        val duration: Int = 0,
        val track: Int = 0,
        val discNumber: Int = 0,
        val year: Int = 0,
        val created: String? = null,
        val starred: String? = null,
        val suffix: String? = null,
        val bitRate: Int = 0,
        val isDir: Boolean = false,
        val type: String? = null
)

@Serializable
data class SubAlbum(
        val id: String,
        val name: String = "",
        val artist: String? = null,
        val artistId: String? = null,
        val coverArt: String? = null,
        val songCount: Int = 0,
        val year: Int = 0,
        val genre: String? = null,
        val created: String? = null,
        val starred: String? = null,
        val song: List<SubSong> = emptyList()
)

@Serializable
data class SubArtist(
        val id: String,
        val name: String = "",
        val coverArt: String? = null,
        val albumCount: Int = 0,
        val artistImageUrl: String? = null,
        val starred: String? = null,
        val album: List<SubAlbum> = emptyList()
)

@Serializable data class SubIndex(val name: String = "", val artist: List<SubArtist> = emptyList())
@Serializable data class SubArtists(val index: List<SubIndex> = emptyList())

@Serializable data class SubGenre(val value: String = "", val songCount: Int = 0, val albumCount: Int = 0)
@Serializable data class SubGenres(val genre: List<SubGenre> = emptyList())

@Serializable
data class SubPlaylist(
        val id: String,
        val name: String = "",
        val songCount: Int = 0,
        val owner: String? = null,
        val coverArt: String? = null,
        /** Navidrome: smart playlists and other users' playlists can't be edited. */
        val readonly: Boolean = false,
        val entry: List<SubSong> = emptyList()
)

@Serializable data class SubPlaylists(val playlist: List<SubPlaylist> = emptyList())
@Serializable data class SubAlbumList(val album: List<SubAlbum> = emptyList())
@Serializable data class SubSongs(val song: List<SubSong> = emptyList())

@Serializable
data class SubSearchResult(
        val song: List<SubSong> = emptyList(),
        val album: List<SubAlbum> = emptyList(),
        val artist: List<SubArtist> = emptyList()
)

@Serializable
data class SubArtistInfo(
        val biography: String? = null,
        val mediumImageUrl: String? = null,
        val largeImageUrl: String? = null,
        val similarArtist: List<SubArtist> = emptyList()
)

/** Classic `getLyrics`. */
@Serializable data class SubLyrics(val value: String? = null)

/** OpenSubsonic `getLyricsBySongId` (songLyrics extension). `start` is in milliseconds when synced. */
@Serializable data class SubLyricLine(val start: Long? = null, val value: String = "")
@Serializable data class SubStructuredLyrics(val synced: Boolean = false, val line: List<SubLyricLine> = emptyList())
@Serializable data class SubLyricsList(val structuredLyrics: List<SubStructuredLyrics> = emptyList())

@Serializable data class SubExtension(val name: String = "")
@Serializable data class SubUser(val username: String = "", val adminRole: Boolean = false)
