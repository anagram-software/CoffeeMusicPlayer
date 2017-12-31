package com.udeshcoffee.android.model

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.Parcelable
import android.provider.MediaStore
import kotlinx.android.parcel.Parcelize

/**
 * Created by Udathari on 9/28/2017.
 */
@SuppressLint("ParcelCreator")
@Parcelize
class Playlist(val type: Int, var id: Long = 0,var title: String = ""): Parcelable {

    @Transient
    var songCount: Int? = null

    init {
        when (type) {
            MOST_PLAYED -> {
                id = 1994
                title = "Most Played"
            }
            RECENTLY_ADDED -> {
                id = 1996
                title = "Recently Added"
            }
            RECENTLY_PLAYED -> {
                id = 1998
                title = "Recently Played"
            }
        }
    }

    constructor(cursor: Cursor) : this(USER,
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)))

    companion object {

        val MOST_PLAYED = 0
        val RECENTLY_ADDED = 1
        val RECENTLY_PLAYED = 2
        val USER = 3

        val SONGMAPPER = { cursor: Cursor ->
            val song = Song(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.TITLE)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ARTIST_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ARTIST)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.DURATION)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.YEAR)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.DATE_ADDED)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.DATE_MODIFIED)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.TRACK)))
            song.playlistSongId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members._ID))
            song
        }
    }
}