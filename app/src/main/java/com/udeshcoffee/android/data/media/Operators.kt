package com.udeshcoffee.android.data.media

import com.udeshcoffee.android.data.model.AlbumStat
import com.udeshcoffee.android.data.model.ArtistStat
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song

/** Albums of [songs] in order of first appearance, each with its song count. */
fun songsToAlbums(songs: List<Song>): List<Album> {
    val albums = LinkedHashMap<Long, Album>()
    for (song in songs) {
        albums.getOrPut(song.albumId) { Album(song.albumId, song.albumName, song.artistName, song.year, song.dateModified) }
                .songCount++
    }
    return albums.values.toList()
}

/** Artists of [songs] in order of first appearance, each with its song count. */
fun songsToArtists(songs: List<Song>): List<Artist> {
    val artists = LinkedHashMap<Long, Artist>()
    for (song in songs) {
        artists.getOrPut(song.artistId) { Artist(song.artistId, song.artistName) }.songCount++
    }
    return artists.values.toList()
}

/** [albums] ordered by [stats] (e.g. most played first); albums without a stat are left out. */
fun statsToAlbums(stats: List<AlbumStat>, albums: List<Album>): List<Album> {
    val byId = albums.groupBy { it.id }
    return stats.flatMap { byId[it.id].orEmpty() }
}

/** [artists] ordered by [stats]; artists without a stat are left out. */
fun statsToArtists(stats: List<ArtistStat>, artists: List<Artist>): List<Artist> {
    val byId = artists.groupBy { it.id }
    return stats.flatMap { byId[it.id].orEmpty() }
}
