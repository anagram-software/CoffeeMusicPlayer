package com.udeshcoffee.android.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.udeshcoffee.android.model.Playlist

/**
 * Created by Udathari on 10/17/2017.
 */
class DeletePlaylistDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlist = this.arguments!!.getParcelable<Playlist>(ARGUMENT_PLAYLIST)
        val goBack = this.arguments!!.getBoolean(ARGUMENT_SHOULD_GO_BACK, false)
        val builder = AlertDialog.Builder(context!!)
        assert(playlist != null)
        builder.setTitle("Delete")
                .setMessage("Delete " + playlist!!.title + " ?")
                // Add action buttons
                .setPositiveButton("Delete") { _, _ ->
                    val resolver = context!!.contentResolver
                    val where = MediaStore.Audio.Playlists._ID + "=?"
                    val whereVal: Array<String> = arrayOf("${playlist.id}")
                    resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal)
                    Toast.makeText(context, " Deleted", Toast.LENGTH_SHORT).show()
                    if (goBack)
                        activity!!.onBackPressed()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

    companion object {
        val ARGUMENT_PLAYLIST = "ARGUMENT_PLAYLIST"
        val ARGUMENT_SHOULD_GO_BACK = "ARGUMENT_SHOULD_GO_BACK"
    }
}