package com.udeshcoffee.android.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playSong
import com.udeshcoffee.android.queueSong

/**
 * Created by Udathari on 10/17/2017.
 */
class CollectionLongDialog: DialogFragment() {

    lateinit var dataRepository: DataRepository
    lateinit var mediaRepository: MediaRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = this.arguments!!.getString(ARGUMENT_TITLE)
        val songs = this.arguments!!.getParcelableArrayList<Song>(ARGUMENT_SONGS)
        val editablePlaylist = this.arguments!!.getParcelable<Playlist>(ARGUMENT_PLAYLIST)

        if (songs == null) {
            Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
            this.dismissAllowingStateLoss()
        }
        assert(songs != null)

        val collectionLongItems = arrayOf(getString(R.string.action_play),
                getString(R.string.action_play_next),
                getString(R.string.action_queue),
                getString(R.string.action_add_to_playlist),
                getString(R.string.action_add_to_favorites))

        val playlistLongItems = arrayOf(getString(R.string.action_play),
                getString(R.string.action_play_next),
                getString(R.string.action_queue),
                getString(R.string.action_add_to_playlist),
                getString(R.string.action_add_to_favorites),
                getString(R.string.action_rename),
                getString(R.string.action_delete))

        val builder = AlertDialog.Builder(context!!)
        builder.setTitle(title)
                .setItems(if (editablePlaylist == null) collectionLongItems else playlistLongItems) { _, which ->
                    when (which) {
                        0 -> if (songs.size > 0) {
                            playSong(0, songs, true)
                        } else {
                            Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
                        }
                        1 -> if (songs.size > 0) {
                            queueSong(songs, true)
                        } else {
                            Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
                        }
                        2 -> if (songs.size > 0) {
                            queueSong(songs, false)
                        } else {
                            Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
                        }
                        3 -> if (songs!!.size > 0) {
                            val addToPlaylistDialog = AddToPlaylistDialog()
                            val bundle = Bundle()
                            bundle.putParcelableArrayList(AddToPlaylistDialog.ARGUMENT_SONGS, songs)
                            if (editablePlaylist != null)
                                bundle.putLong(AddToPlaylistDialog.ARGUMENT_THIS_PLAYLIST_ID, editablePlaylist.id)
                            addToPlaylistDialog.arguments = bundle
                            addToPlaylistDialog.show(fragmentManager, "AddToPlaylistDialog")
                        } else {
                            Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
                        }
                        4 -> if (songs!!.size > 0) {
                            dataRepository.addToFavorites(songs)
                            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No songs available", Toast.LENGTH_SHORT).show()
                        }
                        5 -> if (editablePlaylist != null) {
                            val mDialog = RenamePlaylistDialog()
                            val bundle1 = Bundle()
                            bundle1.putString(RenamePlaylistDialog.ARGUMENT_TITLE, editablePlaylist.title)
                            bundle1.putLong(RenamePlaylistDialog.ARGUMENT_ID, editablePlaylist.id)
                            mDialog.arguments = bundle1
                            mDialog.show(fragmentManager, "RenameDialog")
                        }
                        6 -> if (editablePlaylist != null) {
                            val mDialog = DeletePlaylistDialog()
                            val bundle1 = Bundle()
                            bundle1.putParcelable(DeletePlaylistDialog.ARGUMENT_PLAYLIST, editablePlaylist)
                            mDialog.arguments = bundle1
                            mDialog.show(fragmentManager, "DeletePlaylistDialog")
                        }
                    }
                }
        return builder.create()
    }

    companion object {
        val ARGUMENT_TITLE = "ARGUMENT_TITLE"
        val ARGUMENT_SONGS = "ARGUMENT_SONGS"
        val ARGUMENT_PLAYLIST = "ARGUMENT_PLAYLIST"
    }
}