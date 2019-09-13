package com.udeshcoffee.android.ui.common.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import com.udeshcoffee.android.R

/**
* Created by udesh on 3/21/18.
*/

class WhatsNewDialog : androidx.fragment.app.DialogFragment() {

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
