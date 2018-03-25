package com.udeshcoffee.android.model

import android.os.Parcelable
import com.udeshcoffee.android.utils.compareString
import kotlinx.android.parcel.Parcelize

/**
* Created by Udathari on 8/22/2017.
*/
@Parcelize
data class Album(val id:Long, val title:String,
            val artist:String, val year:Int, val dateModified: Long): Parcelable {

    @Transient var songCount: Int = 0

    operator fun compareTo(album: Album): Int {
        return compareString(this.title, album.title)
    }

}