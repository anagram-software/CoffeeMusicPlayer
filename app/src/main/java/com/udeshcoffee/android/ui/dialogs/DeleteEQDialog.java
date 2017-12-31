package com.udeshcoffee.android.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.udeshcoffee.android.ui.main.equalizer.EqualizerFragment;

/**
 * Created by Udathari on 2/25/2017.
 */

public class DeleteEQDialog extends DialogFragment {

    public static String ARGUMENT_PRESET = "ARGUMENT_PRESET";
    public static String ARGUMENT_NAME = "ARGUMENT_NAME";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int preset = this.getArguments().getInt(ARGUMENT_PRESET);
        final String name = this.getArguments().getString(ARGUMENT_NAME);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete")
                .setMessage("Delete " + name + " preset?")
                // Add action buttons
                .setPositiveButton("Delete", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (getTargetFragment() != null) {
                                    ((EqualizerFragment) getTargetFragment()).onDeleteEQDialogResult(preset);
                                }
                            }
                        }
                )
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                });
        return builder.create();

    }

}
