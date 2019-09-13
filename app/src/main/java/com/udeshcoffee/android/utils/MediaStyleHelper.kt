package com.udeshcoffee.android.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.udeshcoffee.android.R
import com.udeshcoffee.android.service.MusicService

/**
 * Helper APIs for constructing MediaStyle notifications
 */
object MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of [MediaMetadataCompat.getDescription] to extract the appropriate information.
     * @param context Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    fun from(
            context: Context, mediaSession: MediaSessionCompat, channelId: String? = null): NotificationCompat.Builder {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        var bitmap = description.iconBitmap
        if (bitmap == null)
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.default_art)

        val stopIntent = Intent(context, MusicService::class.java)
        stopIntent.action = MusicService.ACTION_STOP
        val stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, 0)

        val builder = if (channelId == null) {
            @Suppress("DEPRECATION") NotificationCompat.Builder(context)
        } else {
            NotificationCompat.Builder(context, channelId)
        }
        builder.setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setSubText(description.description)
                .setLargeIcon(bitmap)
                //                .setContentIntent(controller.getSessionActivity())
                .setDeleteIntent(stopPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return builder
    }
}
