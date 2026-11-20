package com.udeshcoffee.android.extensions

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import com.udeshcoffee.android.playback.PlayerConnection
import kotlinx.coroutines.flow.SharedFlow
import org.koin.core.context.GlobalContext
import kotlin.math.roundToInt

/** The playback connection while it is connected to [com.udeshcoffee.android.playback.PlaybackService], else null. */
fun connectedPlayer(): PlayerConnection? = GlobalContext.get().get<PlayerConnection>().takeIf { it.isConnected }

/** Playback change events ([com.udeshcoffee.android.playback.PlaybackEvents] names), delivered on the main thread. */
fun playbackEvents(): SharedFlow<String> = GlobalContext.get().get<PlayerConnection>().events

fun AppCompatActivity.getPlayer(): PlayerConnection? = connectedPlayer()

fun Int.getColorWithAlpha(alpha: Int): Int =
        Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))

fun Int.getColorWithAlpha(factor: Float): Int =
        Color.argb((Color.alpha(this) * factor).roundToInt(), Color.red(this), Color.green(this), Color.blue(this))

