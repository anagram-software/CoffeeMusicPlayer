package com.udeshcoffee.android.extensions

import android.support.v4.app.Fragment
import android.widget.Toast
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist

/**
* Created by udesh on 2/16/18.
*/

fun <T> Fragment.showPlayingToast(item: T) {
    when (item) {
        is Album -> Toast.makeText(context, "Playing ${item.title}", Toast.LENGTH_SHORT).show()
        is Artist -> Toast.makeText(context, "Playing ${item.name}'s songs", Toast.LENGTH_SHORT).show()
    }
}
