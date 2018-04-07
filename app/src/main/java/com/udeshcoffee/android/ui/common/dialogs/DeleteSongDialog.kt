package com.udeshcoffee.android.ui.common.dialogs

import android.app.Dialog
import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.DopeUtil
import java.io.File

/**
* Created by udesh on 3/21/18.
*/


class DeleteSongDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song = this.arguments!!.getParcelable<Song>(ARGUMENT_SONG)
        val builder = AlertDialog.Builder(activity!!)
        assert(song != null)
        builder.setTitle("Delete")
                .setMessage("Permanently delete " + song!!.title + " from storage?")
                // Add action buttons
                .setPositiveButton("Delete"
                ) { _, _ ->
                    val deleteResolver = activity!!.contentResolver
                    val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
                    val path = DopeUtil.getRealPathFromURI(context!!.contentResolver, trackUri)
                    if (path != null) {
                        val file = File(path)
                        val deleted = file.delete()
                        if (deleted) {
                            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            deleteResolver.delete(uri, "_ID ='" + song.id + "'", null)
                            Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Coudn't delete the song", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> dismiss() }
        return builder.create()
    }

    companion object {
        var ARGUMENT_SONG = "song"
        fun create(song: Song): DeleteSongDialog {
            val mDialog = DeleteSongDialog()
            val bundle = Bundle()
            bundle.putParcelable(ARGUMENT_SONG, song)
            mDialog.arguments = bundle
            return mDialog
        }
    }
}
