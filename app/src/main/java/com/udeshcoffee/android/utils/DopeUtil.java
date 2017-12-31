package com.udeshcoffee.android.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.udeshcoffee.android.model.Song;

import java.util.ArrayList;

/**
 * Created by Udathari on 8/26/2017.
 */

public class DopeUtil {

    public static ArrayList<Long> reserseHexa(String q){
        ArrayList<Long> ids = new ArrayList<>();

        int len = q.length();
        if (len > 1) {
            int n = 0;
            int shift = 0;
            for (int i = 0; i < len; i++) {
                char c = q.charAt(i);
                if (c == ';') {
                    ids.add((long) n);
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += ((c - '0') << shift);
                    } else if (c >= 'a' && c <= 'f') {
                        n += ((10 + c - 'a') << shift);
                    }
                    shift += 4;
                }
            }
        }

        return ids;
    }

    public static String hexa(ArrayList<Song> songs){
        final StringBuilder q = new StringBuilder();

        // The current playlist is saved as a list of "reverse hexadecimal"
        // numbers, which we can generate faster than normal decimal or
        // hexadecimal numbers, which in turn allows us to save the playlist
        // more often without worrying too much about performance.
        char hexDigits[] = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        int len = songs.size();
        for (int i = 0; i < len; i++) {
            long n = songs.get(i).getId();
            if (n >= 0) {
                if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        final int digit = (int) (n & 0xf);
                        n >>>= 4;
                        q.append(hexDigits[digit]);
                    }
                    q.append(";");
                }
            }
        }
        return q.toString();
    }

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

    public static int getStatusHeight(Context c){
        int result = 0;
        int resourceId = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = c.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
