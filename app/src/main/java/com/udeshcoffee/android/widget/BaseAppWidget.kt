package com.udeshcoffee.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import androidx.preference.PreferenceManager

import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.TAG
import com.udeshcoffee.android.extensions.getColorWithAlpha
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.MainActivity

/**
 * Implementation of App Widget functionality.
 */
open class BaseAppWidget : AppWidgetProvider() {

    open val widgetTheme = ""
    open val widgetButtons = ""
    open val widgetAlpha = ""

    open val layoutId = 0

    internal fun notifyChange(context: Context, what: String, song: Song? = null, isPlaying: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val instances = appWidgetManager.getAppWidgetIds(ComponentName(context, this::class.java))

        if (instances != null && instances.isNotEmpty()) {
            val views = RemoteViews(context.packageName, layoutId)
            for (id in instances) {
                when (what) {
                    MusicService.InternalIntents.METADATA_CHANGED -> {
                        song?.let { onMetadataChange(context, appWidgetManager, id, views, it) }
                    }
                    MusicService.InternalIntents.PLAYBACK_STATE_CHANGED -> {
                        Log.d(TAG, "Play Back State Changed: is Playing $isPlaying")
                        if (!isPlaying) {
                            views.setImageViewResource(R.id.play_button, R.drawable.ic_play)
                        } else {
                            views.setImageViewResource(R.id.play_button, R.drawable.ic_pause)
                        }
                        setStyles(context, views, id)
                    }
                    else -> setStyles(context, views, id)
                }

                setupActions(context, views)

                if (id != -1) {
                    appWidgetManager.updateAppWidget(id, views)
                }
            }
        }
    }

    open fun onMetadataChange(context: Context, appWidgetManager: AppWidgetManager, id: Int, views: RemoteViews, song: Song) {}

    private fun setupActions(context: Context, views: RemoteViews) {
        var intent = Intent(context, MainActivity::class.java)
        var pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.widget_layout_holder, pendingIntent)

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

    internal fun setStyles(context: Context, views: RemoteViews, id: Int) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val color: Int

        color = if (preferences.getBoolean(widgetTheme + id, true))
            Color.BLACK
        else
            Color.WHITE

        if (preferences.getBoolean(widgetButtons + id, false)) {
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

        val alpha = preferences.getInt(widgetAlpha + id, 0).toFloat() / 100
        views.setInt(R.id.widget_layout_holder, "setBackgroundColor",color.getColorWithAlpha(alpha))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val instances = appWidgetManager.getAppWidgetIds(ComponentName(context, this.javaClass))
        if (instances != null && instances.isNotEmpty()) {
            val views = RemoteViews(context.packageName, layoutId)
            for (id in instances) {
                setupActions(context, views)
                setStyles(context, views, id)
                if (id != -1)
                    appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}

