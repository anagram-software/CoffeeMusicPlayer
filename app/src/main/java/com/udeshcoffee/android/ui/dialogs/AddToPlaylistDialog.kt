package com.udeshcoffee.android.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import org.koin.android.ext.android.inject
import java.util.*

/**
* Created by Udathari on 10/19/2017.
*/
class AddToPlaylistDialog: DialogFragment() {

    val mediaRepository: MediaRepository by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val songs = this.arguments!!.getParcelableArrayList<Song>(ARGUMENT_SONGS)
        if (songs == null) {
            Toast.makeText(context, "No songs here", Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        }

        val thisPlaylistId = this.arguments!!.getLong(ARGUMENT_THIS_PLAYLIST_ID, -2155)

        val playlists = loadPlaylists(thisPlaylistId)
        val size = playlists.size
        val titles = arrayOfNulls<String>(size + 1)
        (0 until size).forEach { titles[it] = playlists[it].title }
        titles[size] = "Add New"

        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("Add to playlist")
                .setItems(titles) { _, i ->
                    if (i == size) {
                        NewPlaylistDialog.create().show(fragmentManager, "NewPlaylistDialog")
                        dismiss()
                    } else {
                        addToPlaylist(playlists[i].id, songs)
                    }
                }

        return builder.create()
    }

    private fun loadPlaylists(playlistid: Long): List<Playlist> =
            mediaRepository.getPlaylistsExcept(playlistid)

    private fun addToPlaylist(playlistId: Long, songs: ArrayList<Song>) {
        mediaRepository.addToPlaylist(playlistId, songs)
        Toast.makeText(context, "Added " + songs.size + " to playlist", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ARGUMENT_SONGS = "ARGUMENT_SONGS"
        const val ARGUMENT_THIS_PLAYLIST_ID = "ARGUMENT_THIS_PLAYLIST_ID"

        fun create(songs: ArrayList<Song>, thisPlaylistId: Long? = null): AddToPlaylistDialog {
            val addToPlaylistDialog = AddToPlaylistDialog()
            val bundle = Bundle()
            bundle.putParcelableArrayList(AddToPlaylistDialog.ARGUMENT_SONGS, songs)
            thisPlaylistId?.let { bundle.putLong(AddToPlaylistDialog.ARGUMENT_THIS_PLAYLIST_ID, it) }
            addToPlaylistDialog.arguments = bundle
            return addToPlaylistDialog
        }
    }
}