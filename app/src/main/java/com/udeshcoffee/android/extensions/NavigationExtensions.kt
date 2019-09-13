package com.udeshcoffee.android.extensions

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.common.dialogs.AddToPlaylistDialog
import com.udeshcoffee.android.ui.common.dialogs.CollectionLongDialog
import com.udeshcoffee.android.ui.common.dialogs.SongLongDialog


/**
 * Created by Udathari on 1/14/2018.
 */

fun Fragment.openSongLongDialog(song: Song) {
    val mDialog = SongLongDialog()
    val bundle = Bundle()
    bundle.putParcelable(SongLongDialog.ARGUMENT_SONG, song)
    mDialog.arguments = bundle
    fragmentManager?.let { mDialog.show(it, "SongLongDialog") }
}

fun Fragment.openCollectionLongDialog(title: String, songs: List<Song>) {
    fragmentManager?.let { CollectionLongDialog.create(title, songs as ArrayList<Song>).show(it, "CollectionLongDialog") }
}

fun Fragment.openAddToPlaylistDialog(songs: ArrayList<Song>, thisPlaylistId: Long? = null) {
    if (songs.isNotEmpty()) {
        fragmentManager?.let { AddToPlaylistDialog.create(songs, thisPlaylistId).show(it, "AddToPlaylistDialog") }
    } else {
        Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
    }
}

