package com.udeshcoffee.android.ui.common.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.EditText
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import org.koin.android.ext.android.inject

/**
* Created by udesh on 3/21/18.
*/

class NewPlaylistDialog : DialogFragment() {

    val mediaRepository: MediaRepository by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity!!)
        // Get the layout inflater
        val inflater = activity!!.layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_single_edittext, null)
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Add New Playlist")
                .setView(dialogView)
                // Add action buttons
                .setPositiveButton("Add") { _, _ ->
                    val playlist = dialogView.findViewById<View>(R.id.new_name) as EditText
                    val name = playlist.text.toString()
                    if (name != "") {
                        mediaRepository.addPlaylist(name)
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> this@NewPlaylistDialog.dialog.cancel() }
        return builder.create()
    }

    companion object {
        fun create(): NewPlaylistDialog {
            return NewPlaylistDialog()
        }
    }
}
