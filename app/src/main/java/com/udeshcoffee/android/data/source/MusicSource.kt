package com.udeshcoffee.android.data.source

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.utils.PreferenceUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the app's music comes from. The app uses one source at a time: the library, search,
 * favorites, playlists, queue and Android Auto all follow it.
 *
 * Offline listening / caching for the server is meant to be a capability of [SERVER] (cached
 * library data and downloaded songs from the same server), not a third source.
 */
enum class MusicSource {
    /** Songs on this device (MediaStore), with playlists and favorites in the app database. */
    DEVICE,

    /** A Subsonic / OpenSubsonic server; playlists and favorites (stars) live on the server. The default. */
    SERVER
}

/**
 * The chosen [MusicSource]. It's a setting that changes rarely: changing it clears the queue and
 * restarts the UI (see `MusicSourceSwitcher`), so screens and ViewModels read [current] and
 * [isConfigured] once instead of reacting to changes.
 *
 * New installs start on [MusicSource.SERVER] but not [isConfigured]: Settings shows the sign-in
 * form, with the option to use this device's music instead.
 */
class MusicSourceStore(context: Context, private val subsonic: SubsonicRepository) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val _source = MutableStateFlow(load())
    val source: StateFlow<MusicSource> = _source.asStateFlow()

    val current: MusicSource get() = _source.value
    val isServer: Boolean get() = _source.value == MusicSource.SERVER

    /** A usable source: this device, or the server while signed in. False until the user sets one up. */
    val isConfigured: Boolean get() = !isServer || subsonic.isSignedIn

    /** Persists the source. Prefer `MusicSourceSwitcher`, which also resets playback and the UI. */
    fun set(source: MusicSource) {
        prefs.edit { putString(KEY, source.name) }
        _source.value = source
    }

    private fun load(): MusicSource {
        prefs.getString(KEY, null)?.let { stored -> MusicSource.entries.firstOrNull { it.name == stored }?.let { return it } }
        // Installs from before the setting existed played device music: keep them on it.
        val existingInstall = prefs.contains(PreferenceUtil.PLAYLISTS_IMPORTED) || prefs.contains(PreferenceUtil.LAST_PLAYED_POS)
        val source = if (existingInstall && !subsonic.isSignedIn) MusicSource.DEVICE else MusicSource.SERVER
        prefs.edit { putString(KEY, source.name) }
        return source
    }

    companion object {
        private const val KEY = "music_source"
    }
}
