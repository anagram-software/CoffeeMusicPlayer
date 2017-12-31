package com.udeshcoffee.android.ui.dialogs;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.udeshcoffee.android.R;

/**
 * Created by Udathari on 2/25/2017.
 */

public class NewPlaylistDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.dialog_single_edittext, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Add New Playlist")
                .setView(dialogView)
                // Add action buttons
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText playlist = (EditText) dialogView.findViewById(R.id.new_name);
                        String name = playlist.getText().toString();
                        if (!name.equals("")){
                            ContentResolver playlistResolver = getActivity().getContentResolver();
                            ContentValues mInserts = new ContentValues();
                            mInserts.put(MediaStore.Audio.Playlists.NAME, name);
                            mInserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
                            mInserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());
                            Uri mUri = playlistResolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mInserts);
                            String[] PROJECTION_PLAYLIST = new String[] {
                                    MediaStore.Audio.Playlists._ID,
                                    MediaStore.Audio.Playlists.NAME,
                                    MediaStore.Audio.Playlists.DATA
                            };
                            if (mUri != null) {
                                Cursor c = playlistResolver.query(mUri, PROJECTION_PLAYLIST, null, null, null);
                                if (c != null) {
                                    // Save the newly created ID so it can be selected.  Names are allowed to be duplicated,
                                    // but IDs can never be.
                                    c.close();
                                }
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NewPlaylistDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();

    }
}
