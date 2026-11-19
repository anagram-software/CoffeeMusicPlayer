package com.udeshcoffee.android.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.udeshcoffee.android.data.download.DownloadRepository
import com.udeshcoffee.android.data.source.MusicSource
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.playback.QueueStore

/**
 * Sets up or changes the [MusicSource]. Not a hot swap: the queue is cleared (its songs belong to
 * the old source) and the activity task is restarted, so every screen and ViewModel starts fresh.
 * Call on the main thread (it uses the [PlayerConnection]).
 */
class MusicSourceSwitcher(
        private val context: Context,
        private val store: MusicSourceStore,
        private val subsonic: SubsonicRepository,
        private val player: PlayerConnection,
        private val queueStore: QueueStore,
        private val downloads: DownloadRepository
) {

    /** After signing in with SubsonicRepository.signIn: use that server. */
    fun useServer() {
        check(subsonic.isSignedIn) { "Sign in before switching to the server" }
        apply(MusicSource.SERVER)
    }

    /** Use the music on this device (signing out of any server). */
    fun useDevice() {
        leaveServer()
        apply(MusicSource.DEVICE)
    }

    /**
     * Leaves the current source and goes back to setup: signed out, on the (default) server source
     * but not configured, so Settings offers the sign-in form and the device option again.
     */
    fun change() {
        leaveServer()
        apply(MusicSource.SERVER)
    }

    /** Signs out; the server's downloads go too (they're its songs, and IDs could clash with another server's). */
    private fun leaveServer() {
        downloads.removeAll()
        if (subsonic.isSignedIn) subsonic.signOut()
    }

    private fun apply(source: MusicSource) {
        player.clearQueue()
        queueStore.clear()
        store.set(source)
        // A new task, not recreate(): recreate() would keep the old ViewModels (and their source).
        context.startActivity(Intent.makeRestartActivityTask(ComponentName(context, MainActivity::class.java)))
    }
}
