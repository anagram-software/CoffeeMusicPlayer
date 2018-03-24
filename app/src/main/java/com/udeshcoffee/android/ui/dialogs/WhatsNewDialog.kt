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

class WhatsNewDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity!!)
        // Get the layout inflater
        val inflater = activity!!.layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_whats_new, null)
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("What's new!")
                .setView(dialogView)
                // Add action buttons
                .setPositiveButton("Cool!!") { _, _ ->
                    dismiss()
                }
        return builder.create()
    }

    companion object {
        fun create(): WhatsNewDialog {
            return WhatsNewDialog()
        }
    }
}
