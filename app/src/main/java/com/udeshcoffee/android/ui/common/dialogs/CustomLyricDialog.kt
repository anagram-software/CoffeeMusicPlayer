package com.udeshcoffee.android.ui.common.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.udeshcoffee.android.R
import com.udeshcoffee.android.ui.player.lyrics.LyricsFragment

/**
 * Created by Udathari on 10/23/2017.
 */
class CustomLyricDialog : DialogFragment() {

    private var id: Long = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        id = arguments!!.getLong(ARGUMENT_ID)

        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val builder = AlertDialog.Builder(context!!)
        // Get the layout inflater
        val inflater = activity!!.layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_multi_edittext, null)
        val lyricEditText = dialogView.findViewById<EditText>(R.id.multi_et)
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle(getString(R.string.msg_add_lyrics))
                .setView(dialogView)
                // Add action buttons
                .setPositiveButton("Add", { _, _ ->
                    addLyrics(lyricEditText.text.toString()) })
                .setNegativeButton("Cancel", { _, _ -> dismiss() })
        lyricEditText!!.requestFocus()
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        return builder.create()
    }

    private fun addLyrics(lyric: String) {
        val fragment = targetFragment
        if (fragment is LyricsFragment) {
            fragment.onAddLyrics(id, lyric)
        }
        dismiss()
    }

    companion object {
        val ARGUMENT_ID = "ARGUMENT_ID"

        fun create(id: Long): CustomLyricDialog {
            val mDialog = CustomLyricDialog()
            val bundle = Bundle()
            bundle.putLong(CustomLyricDialog.ARGUMENT_ID, id)
            mDialog.arguments = bundle
            return mDialog
        }
    }
}