package com.udeshcoffee.android.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.udeshcoffee.android.R;
import com.udeshcoffee.android.ui.main.equalizer.EqualizerFragment;

/**
 * Created by Udathari on 2/25/2017.
 */

public class SaveEQDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.dialog_single_edittext, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Preset Name")
                .setView(dialogView)
                // Add action buttons
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText playlist = dialogView.findViewById(R.id.new_name);
                        String name = playlist.getText().toString();
                        if (!name.equals("")){
                            if (getTargetFragment() != null) {
                                ((EqualizerFragment) getTargetFragment()).onSaveEQDialogResult(name);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SaveEQDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();

    }
}
