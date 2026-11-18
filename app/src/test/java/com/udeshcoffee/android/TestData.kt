package com.udeshcoffee.android

import com.udeshcoffee.android.model.Song

/** A song with sensible defaults; [track] uses MediaStore's encoding (disc * 1000 + track). */
fun song(
        id: Long,
        title: String = "Song $id",
        albumId: Long = 1,
        album: String = "Album $albumId",
        artistId: Long = 1,
        artist: String = "Artist $artistId",
        duration: Long = 180_000,
        year: Int = 2000,
        dateAdded: Int = 0,
        track: Int = 0
) = Song(id, title, albumId, album, artistId, artist, duration, year, dateAdded, 0, track)
