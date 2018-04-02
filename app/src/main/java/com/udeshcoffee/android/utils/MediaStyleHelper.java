package com.udeshcoffee.android.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.udeshcoffee.android.R;
import com.udeshcoffee.android.service.MusicService;

/**
 * Helper APIs for constructing MediaStyle notifications
 */
public class MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     * @param context Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    public static NotificationCompat.Builder from(
            Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        Bitmap bitmap = description.getIconBitmap();
        if (bitmap == null)
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_art);

        Intent stopIntent = new Intent(context, MusicService.class);
        stopIntent.setAction(MusicService.Companion.getACTION_STOP());
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context);
        builder
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(bitmap)
//                .setContentIntent(controller.getSessionActivity())
                .setDeleteIntent(stopPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }
}
