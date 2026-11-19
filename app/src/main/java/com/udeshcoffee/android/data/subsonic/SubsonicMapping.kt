package com.udeshcoffee.android.data.subsonic

import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import java.time.Instant

private const val UNKNOWN = "<unknown>"

private fun epochSeconds(iso: String?): Long =
        iso?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() } ?: 0

fun SubSong.toSong(): Song = Song(
        id = RemoteIds.song(id),
        title = title,
        albumId = albumId?.let { RemoteIds.album(it) } ?: 0,
        albumName = album ?: UNKNOWN,
        artistId = artistId?.let { RemoteIds.artist(it) } ?: 0,
        artistName = artist ?: UNKNOWN,
        duration = duration * 1000L,
        year = year,
        dateAdded = epochSeconds(created).toInt(),
        dateModified = 0,
        // Same encoding as MediaStore (disc * 1000 + track); Song splits it again.
        track = discNumber.coerceAtLeast(0) * 1000 + track.coerceIn(0, 999),
        remoteId = id,
        coverArt = coverArt,
        albumRemoteId = albumId,
        artistRemoteId = artistId)

fun SubAlbum.toAlbum(): Album =
        Album(RemoteIds.album(id), name, artist ?: UNKNOWN, year, 0, remoteId = id, coverArt = coverArt)
                .also { it.songCount = songCount }

fun SubArtist.toArtist(): Artist =
        Artist(RemoteIds.artist(id), name, remoteId = id, coverArt = coverArt)
                .also { it.songCount = albumCount }

fun SubGenre.toGenre(): Genre =
        Genre(RemoteIds.genre(value), value).also { it.remote = true; it.no = songCount }

fun SubPlaylist.toPlaylist(): Playlist =
        Playlist(Playlist.USER, RemoteIds.playlist(id), name, remoteId = id, readonly = readonly, coverArt = coverArt)
                .also { it.songCount = songCount }

/** Playable songs only (search and directories can include folders). */
fun List<SubSong>.toSongs(): List<Song> = filter { !it.isDir && it.type != "video" }.map { it.toSong() }
