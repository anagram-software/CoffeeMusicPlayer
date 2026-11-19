package com.udeshcoffee.android.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.udeshcoffee.android.model.Song;

import java.util.ArrayList;

/**
 * Created by Udathari on 8/26/2017.
 */

public class DopeUtil {

    public static String countToSongCount(int count) {
        if (count == 1)
           return "1 Song";
        else
            return count + " Songs";
    }

    public static String getRealPathFromURI(ContentResolver contentResolver, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Audio.Media.DATA };
            cursor = contentResolver.query(contentUri,  proj, null, null, null);
            int column_index;
            if (cursor != null) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
