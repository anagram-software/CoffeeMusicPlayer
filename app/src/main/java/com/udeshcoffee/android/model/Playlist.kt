package com.udeshcoffee.android.model

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Created by Udathari on 9/28/2017.
 */
@SuppressLint("ParcelCreator")
@Parcelize
class Playlist(val type: Int, var id: Long = 0,var title: String = ""): Parcelable {

    @IgnoredOnParcel
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

    companion object {

        const val MOST_PLAYED = 0
        const val RECENTLY_ADDED = 1
        const val RECENTLY_PLAYED = 2
        const val USER = 3
    }
}