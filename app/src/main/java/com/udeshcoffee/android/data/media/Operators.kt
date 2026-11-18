package com.udeshcoffee.android.data.media

import android.util.LongSparseArray
import com.udeshcoffee.android.data.model.AlbumStat
import com.udeshcoffee.android.data.model.ArtistStat
import com.udeshcoffee.android.model.*
import com.udeshcoffee.android.extensions.toList

/**
* Created by Udathari on 9/5/2017.
*/
        private val TAG = "Operators"

fun songsToAlbums(songs: List<Song>): List<Album> {

    val albumMap = LongSparseArray<Album>()

    for (song in songs) {

        //Create an album representing the album this song belongs to
        val album = Album(song.albumId, song.albumName, song.artistName, song.year, song.dateModified)

        //Now check if there's already an equivalent album in our albumMap
        val oldAlbum = albumMap[album.id]

        if (oldAlbum == null) {
            //Couldn't find an existing entry for this album. Add a new one.
            album.songCount++
            albumMap.put(album.id, album)
        } else {
            oldAlbum.songCount++
        }
    }

    return albumMap.toList()
}

fun songsToArtists(songs: List<Song>): List<Artist> {

    val artistMap = LongSparseArray<Artist>()

    for (song in songs) {

        //Create an album representing the album this song belongs to
        val artist = Artist(song.artistId, song.artistName)

        //Now check if there's already an equivalent album in our albumMap
        val oldArtist = artistMap[artist.id]

        if (oldArtist == null) {
            artist.songCount++
            artistMap.put(artist.id, artist)
        }else {
            oldArtist.songCount++
        }
    }

    return artistMap.toList()
}

fun statsToAlbums(stats: List<AlbumStat>, albums: List<Album>): List<Album> {
    val tempAlbums = ArrayList<Album>()
    stats.forEach { stat ->
        albums.filter {  stat.id == it.id }
                .forEach { tempAlbums.add(it) }
    }
    return tempAlbums
}

fun statsToArtists(stats: List<ArtistStat>, artists: List<Artist>): List<Artist> {
    val tempArtists = ArrayList<Artist>()
    stats.forEach { stat ->
        artists.filter { stat.id == it.id }
                .forEach { tempArtists.add(it) }
    }
    return tempArtists
}
