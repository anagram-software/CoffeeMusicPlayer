package com.udeshcoffee.android.model

import android.annotation.SuppressLint
import android.os.Parcelable
import com.udeshcoffee.android.utils.CompareUtils
import kotlinx.android.parcel.Parcelize

/**
 * Created by Udathari on 8/22/2017.
 */
@SuppressLint("ParcelCreator")
@Parcelize
data class Artist(val id:Long, val name:String): Parcelable {

    @Transient var songCount: Int = 0
    //var albumCount: Int = 0

    operator fun compareTo(artist: Artist): Int {
        return CompareUtils.compare(this.name, artist.name)
    }
}