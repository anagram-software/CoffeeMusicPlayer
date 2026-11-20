package com.udeshcoffee.android.ui.compose.player

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.utils.PreferenceUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.channels.awaitClose
import android.provider.MediaStore
import com.udeshcoffee.android.data.lyrics.Lrclib
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onStart
import com.udeshcoffee.android.data.lyrics.Lyrics
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import android.util.Log
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModel(
        private val player: PlayerConnection,
        private val dataRepository: DataRepository,
        private val subsonic: SubsonicRepository,
        private val prefs: SharedPreferences
) : ViewModel() {

    /** Device songs: the app's favorites. Server songs: starred on the server. */
    val isFavorite: StateFlow<Boolean> = player.state.map { it.song }.distinctUntilChanged { a, b -> a?.id == b?.id }
            .flatMapLatest { song ->
                val remote = song?.remoteId
                when {
                    song == null -> flowOf(false)
                    remote != null -> subsonic.starredIds.onStart { subsonic.ensureStarredLoaded() }.map { remote in it }
                    else -> dataRepository.observeIsFavorite(song.id)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Whether lyrics can be looked up online, which makes them available for device songs too. */
    val lrclibEnabled: StateFlow<Boolean> = prefs.booleanFlow(PreferenceUtil.PREF_LRCLIB, PreferenceUtil.DEFAULT_LRCLIB)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getBoolean(PreferenceUtil.PREF_LRCLIB, PreferenceUtil.DEFAULT_LRCLIB))

    /** Whether the track quality is shown below the artwork. */
    val showQuality: StateFlow<Boolean> = prefs.booleanFlow(PreferenceUtil.PREF_SHOW_QUALITY, PreferenceUtil.DEFAULT_SHOW_QUALITY)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), prefs.getBoolean(PreferenceUtil.PREF_SHOW_QUALITY, PreferenceUtil.DEFAULT_SHOW_QUALITY))

    private val _showLyrics = MutableStateFlow(prefs.getBoolean(PreferenceUtil.LYRICS, PreferenceUtil.DEFAULT_LYRICS))
    /** Lyrics shown instead of the artwork. */
    val showLyrics: StateFlow<Boolean> = _showLyrics.asStateFlow()

    fun toggleLyrics() {
        _showLyrics.value = !_showLyrics.value
        prefs.edit { putBoolean(PreferenceUtil.LYRICS, _showLyrics.value) }
    }

    fun toggleFavorite() {
        val song = player.state.value.song ?: return
        viewModelScope.launch {
            val remote = song.remoteId
            if (remote != null) runCatching { subsonic.setStarred(listOf(remote), remote !in subsonic.starredIds.value) }
            else dataRepository.toggleFavorite(song.id)
        }
    }
}

sealed interface LyricsState {
    object NoSong : LyricsState
    object Loading : LyricsState
    /** Synced (timed lines) or plain lyrics; [fromLrclib] when they came from LRCLIB rather than the server. */
    data class Found(val lyrics: Lyrics, val fromLrclib: Boolean = false) : LyricsState
    object NotFound : LyricsState
    /** Every source that was tried failed (network, auth or an unexpected response); offers a retry. */
    object Failed : LyricsState
}

/** A boolean preference as a Flow: the current value, then every change. */
internal fun SharedPreferences.booleanFlow(key: String, default: Boolean): Flow<Boolean> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, k -> if (k == key) trySend(p.getBoolean(key, default)) }
    registerOnSharedPreferenceChangeListener(listener)
    trySend(getBoolean(key, default))
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}.distinctUntilChanged()

/**
 * Lyrics for the playing song: the music server's (OpenSubsonic `getLyricsBySongId`, else the
 * classic `getLyrics`), with LRCLIB as the fallback when enabled ([PreferenceUtil.PREF_LRCLIB]):
 * used when the server has none or only unsynced lyrics, and the only source for device songs.
 */
class LyricsViewModel(
        private val player: PlayerConnection,
        private val subsonic: SubsonicRepository,
        private val prefs: SharedPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<LyricsState>(LyricsState.NoSong)
    val state: StateFlow<LyricsState> = _state.asStateFlow()

    /** 1 = small, 2 = medium, 3 = large. */
    private val _size = MutableStateFlow(prefs.getInt(PreferenceUtil.LYRICS_SIZE, 1))
    val size: StateFlow<Int> = _size.asStateFlow()

    private var song: Song? = null
    private var lrclib = prefs.getBoolean(PreferenceUtil.PREF_LRCLIB, PreferenceUtil.DEFAULT_LRCLIB)
    private var job: Job? = null

    init {
        viewModelScope.launch {
            combine(player.state.map { it.song }.distinctUntilChanged { a, b -> a?.id == b?.id },
                    prefs.booleanFlow(PreferenceUtil.PREF_LRCLIB, PreferenceUtil.DEFAULT_LRCLIB)) { s, online -> s to online }
                    .collect { (s, online) ->
                        song = s
                        lrclib = online
                        load(s)
                    }
        }
    }

    private fun load(s: Song?) {
        job?.cancel()
        if (s == null) {
            _state.value = LyricsState.NoSong
            return
        }
        if (!s.isRemote && !lrclib) {
            _state.value = LyricsState.NotFound
            return
        }
        job = viewModelScope.launch {
            _state.value = LyricsState.Loading
            var failed = false
            val server = if (s.isRemote) {
                try {
                    subsonic.lyrics(s)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't load lyrics from the server", e)
                    failed = true
                    null
                }
            } else null
            // LRCLIB fills gaps, and upgrades plain server lyrics to synced ones when it can.
            val online = if (lrclib && server?.synced != true) {
                try {
                    Lrclib.lyrics(s.title, s.artistName.takeUnless { it == MediaStore.UNKNOWN_STRING } ?: "",
                            s.albumName.takeUnless { it == MediaStore.UNKNOWN_STRING }, s.duration)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't load lyrics from LRCLIB", e)
                    failed = true
                    null
                }
            } else null
            _state.value = when {
                online != null && (server == null || online.synced) -> LyricsState.Found(online, fromLrclib = true)
                server != null -> LyricsState.Found(server)
                failed -> LyricsState.Failed
                else -> LyricsState.NotFound
            }
        }
    }

    fun retry() = load(song)

    fun cycleSize() {
        _size.value = _size.value % 3 + 1
        prefs.edit { putInt(PreferenceUtil.LYRICS_SIZE, _size.value) }
    }

    companion object {
        private const val TAG = "LyricsViewModel"
    }
}
