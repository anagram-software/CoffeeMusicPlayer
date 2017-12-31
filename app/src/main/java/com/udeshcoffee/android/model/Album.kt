package com.udeshcoffee.android.model

import android.content.ContentResolver
import android.os.Parcelable
import com.annimon.stream.Stream
import com.udeshcoffee.android.utils.CompareUtils
import com.udeshcoffee.android.data.media.songsToAlbums
import io.reactivex.Observable
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Created by Udathari on 8/22/2017.
 */
@Parcelize
data class Album(val id:Long, val title:String,
            val artist:String, val year:Int, val dateModified: Long): Parcelable {

    @Transient var songCount: Int = 0

    operator fun compareTo(album: Album): Int {
        return CompareUtils.compare(this.title, album.title)
    }

}