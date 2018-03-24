package com.udeshcoffee.android.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.EditText
import com.udeshcoffee.android.R
import com.udeshcoffee.android.ui.main.equalizer.EqualizerFragment

/**
* Created by udesh on 3/21/18.
*/

class SaveEQDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity!!)

        val dialogView = layoutInflater.inflate(R.layout.dialog_single_edittext, null)
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Preset Name")
                .setView(dialogView)
                // Add action buttons
                .setPositiveButton("Save") { _, _ ->
                    val playlist = dialogView.findViewById<EditText>(R.id.new_name)
                    val name = playlist.text.toString()
                    if (name != "") {
                        if (targetFragment != null) {
                            (targetFragment as EqualizerFragment).onSaveEQDialogResult(name)
                        }
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> this@SaveEQDialog.dialog.cancel() }
        return builder.create()
    }

    companion object {
        fun create(): SaveEQDialog {
            return SaveEQDialog()
        }
    }
}
