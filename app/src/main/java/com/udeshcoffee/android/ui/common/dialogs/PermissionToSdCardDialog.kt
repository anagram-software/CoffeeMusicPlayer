package com.udeshcoffee.android.ui.common.dialogs

import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.udeshcoffee.android.R
import com.udeshcoffee.android.ui.main.editor.EditorFragment

/**
 * Created by Udathari on 12/3/2017.
 */

class PermissionToSdCardDialog: androidx.fragment.app.DialogFragment() {

    companion object {
        const val REQUEST_CODE = 17776
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d("Permission", "onCreateDialog")
        val builder = AlertDialog.Builder(context)

//        val view = layoutInflater.inflate(R.layout.dialog_permission_to_sdcard, null)

//        builder.setView(view)
        builder.setTitle(R.string.grant_permission)
                .setNegativeButton(R.string.action_cancel){_,_ ->
                    (targetFragment as EditorFragment).activity?.onBackPressed()
                }
                .setPositiveButton(R.string.action_give_permission) { _,_ ->
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    targetFragment!!.startActivityForResult(intent, REQUEST_CODE)
                    dialog?.dismiss()
                }
        return builder.create()
    }
}