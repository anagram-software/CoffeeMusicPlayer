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
class Playlist(val type: Int, var id: Long = 0, var title: String = "",
               /** Subsonic playlist ID for server playlists; null for device playlists. */
               val remoteId: String? = null,
               /** Server playlists that can't be edited (smart playlists, other users'). */
               val readonly: Boolean = false,
               /** Subsonic cover art ID of a server playlist (e.g. Navidrome's generated mosaic); null for device playlists. */
               val coverArt: String? = null): Parcelable {

    val isRemote: Boolean get() = remoteId != null

    @IgnoredOnParcel
    @Transient
    var songCount: Int? = null

    /** When the playlist was created, for sorting: epoch seconds on a server, creation order (the Room ID) on the device. */
    @IgnoredOnParcel
    @Transient
    var created: Long = 0

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
            DOWNLOADS -> {
                id = 2000
                title = "Downloads"
            }
        }
    }

    companion object {

        const val MOST_PLAYED = 0
        const val RECENTLY_ADDED = 1
        const val RECENTLY_PLAYED = 2
        const val USER = 3
        /** Music server songs downloaded for offline listening (server source only). */
        const val DOWNLOADS = 4
    }
}