package com.udeshcoffee.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.udeshcoffee.android.R
import com.udeshcoffee.android.service.MusicService.InternalIntents
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.utils.ArtworkURI
import com.udeshcoffee.android.utils.MediaStyleHelper
import com.udeshcoffee.android.widget.BigAppWidget
import com.udeshcoffee.android.widget.MediumAppWidget

/**
* Created by Udesh Kumarasinghe on 8/22/2017.
*/
class NotifyManager(val service: MusicService) {

    private var mContext = service.applicationContext
    private var channel: NotificationChannel? = null
    private val metadataBuilder = MediaMetadataCompat.Builder()
    private val playbackBuilder = PlaybackStateCompat.Builder()

    init {
        channel = createNotificationChannel()
    }

    fun notifyChange(what: String, createNotification: Boolean) {
        Log.d(Companion.TAG, "notifyChange what: $what, createNotification: $createNotification")
        when (what) {
            InternalIntents.METADATA_CHANGED -> {
                updateMetaData()
                if (createNotification)
                    createNotification(service.isPlaying())
            }
            InternalIntents.PLAYBACK_STATE_CHANGED -> {
                updatePlaybackState()
                if (createNotification)
                    createNotification(service.isPlaying())
                else
                    removeNotification()
            }
        }
        notifyWidgets(what)
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(Intent().setAction(what))
    }

    fun notifyWidgets(what: String) {
        MediumAppWidget.getInstance().notifyChange(service.applicationContext, what, service.currentSong(), service.isPlaying())
        BigAppWidget.getInstance().notifyChange(service.applicationContext, what, service.currentSong(), service.isPlaying())
    }

    private fun updateMetaData() {
        Log.d(Companion.TAG, "updateMetaData")
        val currentSong = service.currentSong() ?: return
        val uri = ContentUris.withAppendedId(ArtworkURI, currentSong.albumId)

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, uri.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri.toString())

        service.mediaSession.setMetadata(metadataBuilder.build())

        Glide.with(mContext).asBitmap()
                .load(ContentUris.withAppendedId(ArtworkURI, currentSong.albumId))
                .into(object : SimpleTarget<Bitmap>(320, 320) {
                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, resource)
                        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
                        service.mediaSession.setMetadata(metadataBuilder.build())
                        createNotification(service.isPlaying())
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null)
                        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                        service.mediaSession.setMetadata(metadataBuilder.build())
                        createNotification(service.isPlaying())
                    }
                })
    }

    private fun updatePlaybackState() {
        Log.d(Companion.TAG, "updatelaybackState")
        service.mediaSession.let {
            val isPlaying = service.isPlaying()
            if (isPlaying) {
                playbackBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP)
                playbackBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            } else {
                playbackBuilder.setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP)
                playbackBuilder.setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            }
            service.mediaSession.setPlaybackState(playbackBuilder.build())
        }
    }


    private fun createNotification(isPlay: Boolean) {
        Log.d(Companion.TAG, "createNotification isPlay:$isPlay")

        val builder = MediaStyleHelper.from(service, service.mediaSession)

        builder.setSmallIcon(R.drawable.ic_notification)
        channel?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(it.id)
        }}
        builder.color = ContextCompat.getColor(service, R.color.colorPrimaryDark)
        builder.setColorized(true)
        builder.setShowWhen(false)

        builder.addAction(NotificationCompat.Action(R.drawable.ic_back,
                "Back",
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))

        // Actions
        if (!isPlay)
            builder.addAction(NotificationCompat.Action(R.drawable.ic_play,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PLAY)))
        else
            builder.addAction(NotificationCompat.Action(R.drawable.ic_pause,
                    "Pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PAUSE)))

        builder.addAction(NotificationCompat.Action(R.drawable.ic_next,
                "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))

        val stopIntent = Intent(service, MusicService::class.java)
        stopIntent.action = MusicService.ACTION_STOP
        val stopPendingIntent = PendingIntent.getService(service, 0, stopIntent, 0)

        builder.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0,1,2)
                .setMediaSession(service.mediaSession.sessionToken)
                .setCancelButtonIntent(stopPendingIntent)
                .setShowCancelButton(true))

        val notIntent = Intent(service, MainActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val launchNowPlayingPendingIntent = PendingIntent.getActivity(service, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(launchNowPlayingPendingIntent)

        //Build the notification object.
        val notification = builder.build()

        NotificationManagerCompat.from(service).notify(Companion.NOTIFY_ID, notification)

        if (isPlay) {
            service.startForeground(Companion.NOTIFY_ID, notification)
        } else {
            service.stopForeground(false)
        }
    }

    private fun createNotificationChannel(): NotificationChannel? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val id = "coffee_player_channel"
            val name = "Coffee Player"
            val description = "Play Music of Coffee Player"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(id, name, importance)

            channel.description = description
            channel.enableLights(true)
            channel.enableVibration(false)
            val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            return channel
        } else {
            return null
        }
    }

    private fun removeNotification(){
        NotificationManagerCompat.from(mContext).cancel(Companion.NOTIFY_ID)
        service.stopForeground(true)
    }

    companion object {
        private const val NOTIFY_ID = 1998
        private const val TAG = "NotifyManager"
    }

}