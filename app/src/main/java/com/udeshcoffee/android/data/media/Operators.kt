package com.udeshcoffee.android.data.media

import android.content.ContentResolver
import android.provider.MediaStore
import android.util.LongSparseArray
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.squareup.sqlbrite2.BriteContentResolver
import com.udeshcoffee.android.data.model.AlbumStat
import com.udeshcoffee.android.data.model.ArtistStat
import com.udeshcoffee.android.data.model.SongStat
import com.udeshcoffee.android.model.*
import com.udeshcoffee.android.extensions.toList
import io.reactivex.Observable

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

fun toCompletePlaylists(contentResolver: ContentResolver, playlists: List<Playlist>): List<Playlist> {

    val tempList = ArrayList<Playlist>()

    tempList.add(Playlist(Playlist.MOST_PLAYED))
    tempList.add(Playlist(Playlist.RECENTLY_PLAYED))
    tempList.add(Playlist(Playlist.RECENTLY_ADDED))

    for (playlist in playlists) {
        val cursor = contentResolver.query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.id),
                arrayOf(MediaStore.Audio.Playlists.Members._ID),
                null,
                null,
                null)
        if (cursor != null) {
            playlist.songCount = cursor.count
            cursor.close()
        }
    }

    tempList.addAll(playlists)

    return tempList
}

fun statsToSongs(stats: List<SongStat>, briteContentResolver: BriteContentResolver, songQuery: Query): Observable<List<Song>> {
    val query = songQuery.copy(
            selection = MediaStore.Audio.Media.IS_MUSIC + "=1 AND " + MediaStore.Audio.Media._ID + " IN (" +
                    Stream.of(stats)
                            .map({ it.id.toString() })
                            .collect(Collectors.joining(",")) + ")"
    )
    return briteContentResolver
            .createQuery(query.uri, query.projection, query.selection, query.args, query.sort, false)
            .mapToList{ Song(it) }
            .map { songs ->
                Stream.of(songs)
                        .forEach { song ->
                            Stream.of(stats)
                                    .forEach {
                                        if (it.id == song.id) {
                                            song.playcount = it.playCount
                                            song.lastplayed = (it.playTime).toInt()
                                        }
                                    }
                        }
                songs
            }
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
