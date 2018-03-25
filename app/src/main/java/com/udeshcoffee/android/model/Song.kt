package com.udeshcoffee.android.model

import android.database.Cursor
import android.os.Parcelable
import android.provider.MediaStore
import com.udeshcoffee.android.utils.compareString
import kotlinx.android.parcel.Parcelize

/**
* Created by Udathari on 8/22/2017.
*/
@Parcelize
class Song(val id:Long, val title:String,
           val albumId:Long, val albumName:String,
           val artistId:Long, val artistName:String,
           val duration:Long, val year:Int,
           val dateAdded:Int, val dateModified: Long,
           var track:Int): Parcelable {

    @Transient var discNumber : Int = 0
    @Transient var playlistSongId: Long = 0
    @Transient var playcount: Int = 0
    @Transient var lastplayed: Int = 0

    init {
        if (track >= 1000){
            discNumber = track / 1000
            track %= 1000
        }
    }

    constructor(cursor: Cursor) : this(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)),
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)),
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)),
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)))

    fun getAlbum() : Album = Album(albumId, albumName, artistName, year, dateModified)

    fun getArtist() : Artist = Artist(artistId, artistName)

    operator fun compareTo(song: Song): Int {
        return compareString(this.title, song.title)
    }
}