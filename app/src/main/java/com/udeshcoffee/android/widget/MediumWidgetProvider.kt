package com.udeshcoffee.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.udeshcoffee.android.R
import com.udeshcoffee.android.getColorWithAlpha
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.main.MainActivity

/**
 * Created by Udathari on 10/26/2017.
 */
class MediumWidgetProvider : AppWidgetProvider() {

    val TAG = "MediumWidgetProvider"

    companion object {
        var MEDIUM_WIDGET_THEME = "mediumwidgettheme"
        var MEDIUM_WIDGET_BUTTONS = "mediumwidgetbuttons"
        var MEDIUM_WIDGET_ALPHA = "mediumwidgetalpha"

        var instance: MediumWidgetProvider? = null
            @Synchronized get() {
                if (field == null) {
                    field = MediumWidgetProvider()
                }
                return field as MediumWidgetProvider
            }

    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val instances = appWidgetManager.getAppWidgetIds(ComponentName(context, this.javaClass))
        if (instances != null && instances.isNotEmpty()) {
            val views = RemoteViews(context.packageName, R.layout.widget_medium)
            for (id in instances) {
                setupActions(context, views)
                setStyles(context, views, id)
                if (id != -1)
                    appWidgetManager.updateAppWidget(id, views)
            }
//            val songPos = PreferenceManager.getDefaultSharedPreferences(context).getInt(PreferenceUtil.LAST_PLAYED_POS, 0)
//            getQueue(context)?.subscribe{
//                val song = it[songPos]
//
//
//            }
        }
    }

    private fun setupActions(context: Context, views: RemoteViews) {
        var intent = Intent(context, MainActivity::class.java)
        var pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.widget_layout_medium, pendingIntent)

        intent = Intent(MusicService.WidgetIntents.PLAY_PAUSE)
        pendingIntent = PendingIntent.getService(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.play_button, pendingIntent)

        intent = Intent(MusicService.WidgetIntents.NEXT)
        pendingIntent = PendingIntent.getService(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.next_button, pendingIntent)

        intent = Intent(MusicService.WidgetIntents.PREVIOUS)
        pendingIntent = PendingIntent.getService(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.prev_button, pendingIntent)
    }

    fun notifyChange(service: MusicService, what: String) {
        val appWidgetManager = AppWidgetManager.getInstance(service)
        val instances = appWidgetManager.getAppWidgetIds(ComponentName(service, this.javaClass))

        if (instances != null && instances.isNotEmpty()) {
            val song = service.currentSong()
            if (song != null) {
                val views = RemoteViews(service.packageName, R.layout.widget_medium)
                for (id in instances) {
                    if (what == MusicService.InternalIntents.METADATA_CHANGED) {
                        val titleName = song.title
                        val albumName = song.albumName
                        val artistName = song.artistName
                        var errorState: CharSequence? = null

                        // Format title string with track number, or show SD card message
                        val status = Environment.getExternalStorageState()
                        if (status == Environment.MEDIA_SHARED || status == Environment.MEDIA_UNMOUNTED) {
                            errorState = if (Environment.isExternalStorageRemovable()) {
                                "SD Card Busy"
                            } else {
                                "No SD Card"
                            }
                        } else if (status == Environment.MEDIA_REMOVED) {
                            errorState = if (Environment.isExternalStorageRemovable()) {
                                "No Title"
                            } else {
                                "No SD Card"
                            }
                        }

                        if (errorState != null) {
                            // Show error state to user
                            views.setViewVisibility(R.id.text1, View.GONE)
                            views.setTextViewText(R.id.text2, errorState)

                        } else {
                            // No error, so show normal titles
                            views.setViewVisibility(R.id.text1, View.VISIBLE)
                            views.setTextViewText(R.id.text1, titleName)
                            views.setTextViewText(R.id.text2, artistName + " | " + albumName)
                        }

                        views.setImageViewResource(R.id.album_art, R.drawable.default_art)

                        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                        val uri = ContentUris.withAppendedId(sArtworkUri, song.albumId)

                        Glide.with(service).asBitmap().load(uri)
                                .into(object : SimpleTarget<Bitmap>(180, 180) {
                                    override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>) {
                                        views.setImageViewBitmap(R.id.album_art, bitmap)
                                        setStyles(service, views, id)
                                        appWidgetManager.updateAppWidget(id, views)
                                    }

                                    override fun onLoadFailed(errorDrawable: Drawable?) {
                                        super.onLoadFailed(errorDrawable)
                                        views.setImageViewResource(R.id.album_art, R.drawable.default_art)
                                        setStyles(service, views, id)
                                        appWidgetManager.updateAppWidget(id, views)
                                    }

                                })
                    } else if (what == MusicService.InternalIntents.PLAYBACK_STATE_CHANGED) {
                        val isPlaying = service.isPlaying()
                        Log.d(TAG, "$isPlaying")
                        if (!isPlaying) {
                            views.setImageViewResource(R.id.play_button, R.drawable.ic_play)
                        } else {
                            views.setImageViewResource(R.id.play_button, R.drawable.ic_pause)
                        }
                        setupActions(service, views)
                        setStyles(service, views, id)
                    } else {
                        setStyles(service, views, id)
                    }

                    if (id != -1) {
                        appWidgetManager.updateAppWidget(id, views)
                    }
                }
            }
        }
    }

    private fun setStyles(context: Context, views: RemoteViews, id: Int) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val color: Int

        if (preferences.getBoolean(MEDIUM_WIDGET_THEME + id, true))
            color = Color.BLACK
        else
            color = Color.WHITE

        if (preferences.getBoolean(MEDIUM_WIDGET_BUTTONS + id, false)) {
            views.setInt(R.id.prev_button, "setColorFilter", Color.BLACK)
            views.setInt(R.id.play_button, "setColorFilter", Color.BLACK)
            views.setInt(R.id.next_button, "setColorFilter", Color.BLACK)
            views.setInt(R.id.text1, "setTextColor", Color.BLACK)
            views.setInt(R.id.text2, "setTextColor", Color.BLACK)
        } else {
            views.setInt(R.id.prev_button, "setColorFilter", Color.WHITE)
            views.setInt(R.id.play_button, "setColorFilter", Color.WHITE)
            views.setInt(R.id.next_button, "setColorFilter", Color.WHITE)
            views.setInt(R.id.text1, "setTextColor", Color.WHITE)
            views.setInt(R.id.text2, "setTextColor", Color.WHITE)
        }

        val alpha = preferences.getInt(MEDIUM_WIDGET_ALPHA + id, 0).toFloat() / 100
        views.setInt(R.id.widget_layout_holder, "setBackgroundColor",color.getColorWithAlpha(alpha))
    }
}