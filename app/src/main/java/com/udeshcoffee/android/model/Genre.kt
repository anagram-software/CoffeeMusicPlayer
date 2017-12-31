package com.udeshcoffee.android.model

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore

/**
 * Created by Udathari on 9/8/2017.
 */
class Genre(var id: Long, var name: String) {

    var no: Int? = null
    var arts: List<Long>? = null

    constructor(cursor: Cursor): this(
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID))
            ,cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)))

    fun setArtsAndSongCount(contentResolver: ContentResolver){
        val cursor = contentResolver.query(
                MediaStore.Audio.Genres.Members.getContentUri("external", this.id),
                arrayOf(MediaStore.Audio.Genres.Members.ALBUM_ID),
                null,
                null,
                null)
        val arts = ArrayList<Long>()
        if (cursor != null) {
            this.no = cursor.count
            cursor.moveToFirst()
            while (!cursor.isAfterLast && arts.size < 5) {
                val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Genres.Members.ALBUM_ID))
                if (!arts.contains(id))
                    arts.add(id)
                cursor.moveToNext()
            }
            this.arts = arts
            cursor.close()
        }
    }
}