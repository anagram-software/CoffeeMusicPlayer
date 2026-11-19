package com.udeshcoffee.android.data.subsonic

import com.udeshcoffee.android.model.Song
import kotlinx.serialization.Serializable

/**
 * The details of a music server song, enough to show and play it without asking the server:
 * stored with the saved queue (`QueueStore`) and with each download (`DownloadRepository`).
 */
@Serializable
data class ServerSong(
        val id: String, val title: String, val album: String, val albumId: String? = null,
        val artist: String, val artistId: String? = null, val coverArt: String? = null,
        val durationMs: Long = 0, val track: Int = 0, val disc: Int = 0, val year: Int = 0
)

/** Snapshot of a server song (null for device songs). */
fun Song.toServerSong(): ServerSong? = remoteId?.let { id ->
    ServerSong(id, title, albumName, albumRemoteId, artistName, artistRemoteId, coverArt, duration, track, discNumber, year)
}

fun ServerSong.toSong(): Song = Song(RemoteIds.song(id), title, albumId?.let { RemoteIds.album(it) } ?: 0, album,
        artistId?.let { RemoteIds.artist(it) } ?: 0, artist, durationMs, year, 0, 0, disc * 1000 + track,
        remoteId = id, coverArt = coverArt, albumRemoteId = albumId, artistRemoteId = artistId)
