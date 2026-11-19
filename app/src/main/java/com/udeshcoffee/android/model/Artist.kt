package com.udeshcoffee.android.model

import android.annotation.SuppressLint
import android.os.Parcelable
import com.udeshcoffee.android.utils.compareString
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
* Created by Udathari on 8/22/2017.
*/
@SuppressLint("ParcelCreator")
@Parcelize
data class Artist(val id:Long, val name:String,
                  /** Subsonic artist ID for server artists; null for device artists. */
                  val remoteId: String? = null,
                  val coverArt: String? = null): Parcelable {

    val isRemote: Boolean get() = remoteId != null

    @IgnoredOnParcel
    @Transient var songCount: Int = 0
    //var albumCount: Int = 0

    operator fun compareTo(artist: Artist): Int {
        return compareString(this.name, artist.name)
    }
}