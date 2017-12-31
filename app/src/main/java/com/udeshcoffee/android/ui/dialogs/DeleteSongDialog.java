package com.udeshcoffee.android.ui.dialogs;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.udeshcoffee.android.model.Song;
import com.udeshcoffee.android.utils.DopeUtil;

import java.io.File;

/**
 * Created by Udathari on 2/25/2017.
 */

public class DeleteSongDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Song song = this.getArguments().getParcelable("song");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        assert song != null;
        builder.setTitle("Delete")
                .setMessage("Permanently delete " + song.getTitle() + " from storage?")
                // Add action buttons
                .setPositiveButton("Delete", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ContentResolver deleteResolver = getActivity().getContentResolver();
                                Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
                                String path = DopeUtil.getRealPathFromURI(getContext().getContentResolver(), trackUri);
                                if (path != null) {
                                    File file = new File(path);
                                    boolean deleted = file.delete();
                                    if (deleted) {
                                        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                                        deleteResolver.delete(uri, "_ID ='" + song.getId() + "'", null);
                                        Toast.makeText(getContext(), "Song deleted", Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(getContext(), "Coudn't delete the song", Toast.LENGTH_SHORT).show();
                                    }
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
