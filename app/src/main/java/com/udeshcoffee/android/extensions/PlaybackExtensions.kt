package com.udeshcoffee.android.extensions

import android.os.Handler
import android.os.Looper
import com.udeshcoffee.android.model.Song

private val mainHandler = Handler(Looper.getMainLooper())

/** MediaController must be used on the main thread; run there even if called from a background callback. */
private inline fun onMain(crossinline block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
}

/** Replaces the queue with [allItems] and starts at [position]. */
fun playSong(position: Int, allItems: List<Song>, shouldPlay: Boolean) {
    if (allItems.isNotEmpty()) onMain { connectedPlayer()?.setQueue(allItems, position, shouldPlay) }
}

/** Adds songs after the current one ([isNext]) or at the end of the queue. */
fun queueSong(allItems: List<Song>, isNext: Boolean) {
    if (allItems.isNotEmpty()) onMain { connectedPlayer()?.addToQueue(allItems, isNext) }
}

/** Plays [allItems] in random order. */
fun shuffle(allItems: List<Song>) {
    if (allItems.isNotEmpty()) playSong(0, allItems.shuffled(), true)
}
