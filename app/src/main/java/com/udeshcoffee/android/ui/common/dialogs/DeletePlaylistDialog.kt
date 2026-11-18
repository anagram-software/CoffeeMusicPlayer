package com.udeshcoffee.android.ui.common.dialogs

import kotlinx.coroutines.launch
import com.udeshcoffee.android.utils.ProcessLifecycleScope
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.udeshcoffee.android.data.MediaRepository
import org.koin.android.ext.android.inject

/**
* Created by Udathari on 10/17/2017.
*/
class DeletePlaylistDialog: androidx.fragment.app.DialogFragment() {

    private val mediaRepository: MediaRepository by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlistId = this.arguments!!.getLong(ARGUMENT_ID)
        val playlistTitle = this.arguments!!.getString(ARGUMENT_TITLE)
        val goBack = this.arguments!!.getBoolean(ARGUMENT_SHOULD_GO_BACK, false)
        val builder = AlertDialog.Builder(context!!)

        builder.setTitle("Delete")
                .setMessage("Delete $playlistTitle ?")
                // Add action buttons
                .setPositiveButton("Delete") { _, _ ->
                    ProcessLifecycleScope.launch { mediaRepository.deletePlaylist(playlistId) }
                    Toast.makeText(context, " Deleted", Toast.LENGTH_SHORT).show()
                    if (goBack)
                        requireActivity().onBackPressedDispatcher.onBackPressed()
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