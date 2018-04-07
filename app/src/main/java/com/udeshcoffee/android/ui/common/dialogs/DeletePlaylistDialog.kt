package com.udeshcoffee.android.ui.common.dialogs

import android.app.Dialog
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.Toast

/**
* Created by Udathari on 10/17/2017.
*/
class DeletePlaylistDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlistId = this.arguments!!.getLong(ARGUMENT_ID)
        val playlistTitle = this.arguments!!.getString(ARGUMENT_TITLE)
        val goBack = this.arguments!!.getBoolean(ARGUMENT_SHOULD_GO_BACK, false)
        val builder = AlertDialog.Builder(context!!)

        builder.setTitle("Delete")
                .setMessage("Delete $playlistTitle ?")
                // Add action buttons
                .setPositiveButton("Delete") { _, _ ->
                    val resolver = context!!.contentResolver
                    val where = MediaStore.Audio.Playlists._ID + "=?"
                    val whereVal: Array<String> = arrayOf("$playlistId")
                    resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal)
                    Toast.makeText(context, " Deleted", Toast.LENGTH_SHORT).show()
                    if (goBack)
                        activity!!.onBackPressed()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        return builder.create()
    }

    companion object {
        val ARGUMENT_ID = "ARGUMENT_ID"
        val ARGUMENT_TITLE = "ARGUMENT_TITLE"
        val ARGUMENT_SHOULD_GO_BACK = "ARGUMENT_SHOULD_GO_BACK"

        fun create(playlistId: Long, playlistTitle: String, shouldGoBack: Boolean): DeletePlaylistDialog {
            val mDialog = DeletePlaylistDialog()
            val bundle1 = Bundle()
            bundle1.putLong(DeletePlaylistDialog.ARGUMENT_ID, playlistId)
            bundle1.putString(DeletePlaylistDialog.ARGUMENT_TITLE, playlistTitle)
            bundle1.putBoolean(DeletePlaylistDialog.ARGUMENT_SHOULD_GO_BACK, shouldGoBack)
            mDialog.arguments = bundle1
            return mDialog
        }
    }
}