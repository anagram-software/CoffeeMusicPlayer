package com.udeshcoffee.android.utils

import android.content.Context
import androidx.core.content.edit
import com.udeshcoffee.android.App
import com.udeshcoffee.android.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * When each playlist was last played from in this app, for the playlists' "Recently played" sort
 * (neither MediaStore/Room nor Subsonic keeps this). Recorded when a playlist's songs are started
 * from its card or page. Stored in the `playlist_plays` SharedPreferences file, read on first use.
 */
object PlaylistPlays {
    private val prefs by lazy { App.instance.getSharedPreferences("playlist_plays", Context.MODE_PRIVATE) }

    private val _plays = MutableStateFlow<Map<String, Long>?>(null)

    /** Last-played times (epoch millis) by [key]. Call [load] first (off the main thread). */
    val plays: StateFlow<Map<String, Long>?> = _plays.asStateFlow()

    /** Reads the stored times (disk access: call off the main thread). */
    fun load() {
        if (_plays.value != null) return
        _plays.value = prefs.all.mapNotNull { (k, v) -> (v as? Long)?.let { k to it } }.toMap()
    }

    fun key(playlist: Playlist): String = "${playlist.type}:${playlist.remoteId ?: playlist.id}"

    fun record(playlist: Playlist, now: Long = System.currentTimeMillis()) {
        val key = key(playlist)
        _plays.update { (it ?: emptyMap()) + (key to now) }
        ProcessLifecycleScope.launch(Dispatchers.IO) { prefs.edit { putLong(key, now) } }
    }
}
