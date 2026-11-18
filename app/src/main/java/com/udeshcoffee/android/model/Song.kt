package com.udeshcoffee.android.model

import android.database.Cursor
import android.os.Parcelable
import android.provider.MediaStore
import com.udeshcoffee.android.utils.compareString
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
* Created by Udathari on 8/22/2017.
*/
@Parcelize
data class Song(val id:Long, val title:String,
           val albumId:Long, val albumName:String,
           val artistId:Long, val artistName:String,
           val duration:Long, val year:Int,
           val dateAdded:Int, val dateModified: Long,
           var track:Int,
           /** Subsonic song ID for server songs (then [id] is derived from it, see RemoteIds); null for device songs. */
           val remoteId: String? = null,
           /** Subsonic cover art ID. */
           val coverArt: String? = null,
           val albumRemoteId: String? = null,
           val artistRemoteId: String? = null): Parcelable {

    /** True for songs streamed from the music server. */
    val isRemote: Boolean get() = remoteId != null

    @IgnoredOnParcel var discNumber : Int = 0
    @IgnoredOnParcel var playlistSongId: Long = 0
    @IgnoredOnParcel var playcount: Int = 0
    @IgnoredOnParcel var lastplayed: Int = 0

    init {
        if (track >= 1000){
            discNumber = track / 1000
            track %= 1000
        }
    }

    constructor(cursor: Cursor) : this(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: "",
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)) ?: "<unknown>",
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "<unknown>",
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)),
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)),
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)),
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)))

    fun getAlbum() : Album = Album(albumId, albumName, artistName, year, dateModified, albumRemoteId, coverArt)

    fun getArtist() : Artist = Artist(artistId, artistName, artistRemoteId)

    operator fun compareTo(song: Song): Int {
        return compareString(this.title, song.title)
    }
}