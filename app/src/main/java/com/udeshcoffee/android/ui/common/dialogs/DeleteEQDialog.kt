package com.udeshcoffee.android.ui.common.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.udeshcoffee.android.ui.main.equalizer.EqualizerFragment

/**
* Created by udesh on 3/21/18.
*/

class DeleteEQDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val preset = this.arguments!!.getInt(ARGUMENT_PRESET)
        val name = this.arguments!!.getString(ARGUMENT_NAME)
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle("Delete")
                .setMessage("Delete $name preset?")
                // Add action buttons
                .setPositiveButton("Delete"
                ) { _, _ ->
                    if (targetFragment != null) {
                        (targetFragment as EqualizerFragment).onDeleteEQDialogResult(preset)
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> dismiss() }
        return builder.create()

    }

    companion object {
        var ARGUMENT_PRESET = "ARGUMENT_PRESET"
        var ARGUMENT_NAME = "ARGUMENT_NAME"
        fun create(preset: Int, name: String): DeleteEQDialog {
            val mDialog = DeleteEQDialog()
            val bundle = Bundle()
            bundle.putInt(DeleteEQDialog.ARGUMENT_PRESET, preset)
            bundle.putString(DeleteEQDialog.ARGUMENT_NAME, name)
            mDialog.arguments = bundle
            return mDialog
        }
    }

}
