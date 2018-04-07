package com.udeshcoffee.android.ui.common.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.udeshcoffee.android.R
import com.udeshcoffee.android.ui.main.editor.EditorFragment
import com.udeshcoffee.android.ui.player.lyrics.LyricsFragment

/**
 * Created by Udathari on 10/23/2017.
 */
class SearchSongDialog: DialogFragment() {

    private var id: Long = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        id = arguments!!.getLong(ARGUMENT_ID)
        val title = arguments!!.getString(ARGUMENT_TITLE)
        val artist = arguments!!.getString(ARGUMENT_ARTIST)

        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val builder = AlertDialog.Builder(context!!)
        // Get the layout inflater
        val inflater = activity!!.layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_search_song, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.search_title)
        titleEditText.setText(title)
        val artistEditText = dialogView.findViewById<EditText>(R.id.search_artist)
        artistEditText.setText(artist)
        artistEditText.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(titleEditText.text.toString(), artistEditText.text.toString())
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Search Song")
                .setView(dialogView)
                // Add action buttons
                .setPositiveButton("Search", { _, _ ->
                    performSearch(titleEditText.text.toString(), artistEditText.text.toString()) })
                .setNegativeButton("Cancel", { _, _ -> dismiss() })
        titleEditText!!.requestFocus()
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        return builder.create()
    }

    private fun performSearch(title: String, artist: String) {
        val fragment = targetFragment
        when (fragment) {
            is LyricsFragment -> fragment.onSearchRequest(id, title, artist)
            is EditorFragment -> fragment.onSearchRequest(id, title, artist)
        }
        dismiss()
    }

    companion object {
        val ARGUMENT_ID = "ARGUMENT_ID"
        val ARGUMENT_TITLE = "ARGUMENT_TITLE"
        val ARGUMENT_ARTIST = "ARGUMENT_ARTIST"

        fun create(id: Long, title: String, artist: String): SearchSongDialog {
            val mDialog = SearchSongDialog()
            val bundle = Bundle()
            bundle.putLong(SearchSongDialog.ARGUMENT_ID, id)
            bundle.putString(SearchSongDialog.ARGUMENT_TITLE, title)
            bundle.putString(SearchSongDialog.ARGUMENT_ARTIST, artist)
            mDialog.arguments = bundle
            return mDialog
        }
    }
}