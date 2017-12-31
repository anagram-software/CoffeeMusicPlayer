package com.udeshcoffee.android.ui.dialogs

import android.app.Dialog
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.EditText
import com.udeshcoffee.android.R

/**
 * Created by Udathari on 10/17/2017.
 */
class RenamePlaylistDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = this.arguments!!.getString(ARGUMENT_TITLE)
        val playlistId = this.arguments!!.getLong(ARGUMENT_ID)

        val builder = AlertDialog.Builder(context!!)
        // Get the layout inflater
        val inflater = activity!!.layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_single_edittext, null)
        val playlist = dialogView.findViewById<EditText>(R.id.new_name)
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        playlist.setText(title)
        builder.setTitle("Rename $title")
                .setView(dialogView)
                // Add action buttons
                .setPositiveButton("Rename", { _, _ ->
                    val name = playlist.text.toString()
                    if (name != "") {
                        val playlistResolver = context!!.contentResolver
                        val mInserts = ContentValues()
                        mInserts.put(MediaStore.Audio.Playlists.NAME, name)
                        val where = MediaStore.Audio.Playlists._ID + "=?"
                        val whereVal = arrayOf("$playlistId")
                        playlistResolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mInserts, where, whereVal)
                    }
                })
                .setNegativeButton("Cancel", { _, _ -> this@RenamePlaylistDialog.dialog.cancel() })
        return builder.create()
    }

    companion object {
        val ARGUMENT_TITLE = "ARGUMENT_TITLE"
        val ARGUMENT_ID = "ARGUMENT_ID"
    }
}