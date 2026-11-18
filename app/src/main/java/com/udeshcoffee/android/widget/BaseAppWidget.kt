package com.udeshcoffee.android.widget

import com.udeshcoffee.android.playback.PlaybackEvents
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import androidx.preference.PreferenceManager

import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.getColorWithAlpha
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.MainActivity

/**
 * Implementation of App Widget functionality.
 */
internal const val TAG = "AppWidget"

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
                    PlaybackEvents.METADATA_CHANGED -> {
                        song?.let { onMetadataChange(context, appWidgetManager, id, views, it) }
                    }
                    PlaybackEvents.PLAYBACK_STATE_CHANGED -> {
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
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_layout_holder, openApp)

        // Media key events through Media3's MediaButtonReceiver, which starts the playback
        // service (and resumes the saved queue) even when the app isn't running.
        views.setOnClickPendingIntent(R.id.play_button, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.next_button, mediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT))
        views.setOnClickPendingIntent(R.id.prev_button, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
    }

    @OptIn(UnstableApi::class)
    private fun mediaKey(context: Context, keyCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(ComponentName(context, MediaButtonReceiver::class.java))
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        // Distinct request codes: the intents differ only in their extras.
        return PendingIntent.getBroadcast(context, keyCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
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

