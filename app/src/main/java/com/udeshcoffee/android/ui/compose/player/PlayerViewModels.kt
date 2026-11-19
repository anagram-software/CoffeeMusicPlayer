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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onStart
import com.udeshcoffee.android.data.subsonic.SubsonicException
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
    /** Plain text lines, as the server sends them. */
    data class Found(val lines: List<String>) : LyricsState
    object NotFound : LyricsState
    /** The server request failed (network, auth or an unexpected response); offers a retry. */
    object Failed : LyricsState
}

/**
 * Lyrics for the playing song, from the music server only (OpenSubsonic `getLyricsBySongId`, else
 * the classic `getLyrics`). Device songs have none.
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
    private var job: Job? = null

    init {
        viewModelScope.launch {
            player.state.map { it.song }.distinctUntilChanged { a, b -> a?.id == b?.id }.collect { s ->
                song = s
                load(s)
            }
        }
    }

    /** Runs one lyrics request at a time; a failure shows [LyricsState.Failed] instead of crashing the app. */
    private fun launch(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't load lyrics", e)
                _state.value = LyricsState.Failed
            }
        }
    }

    private fun load(s: Song?) {
        if (s == null) {
            job?.cancel()
            _state.value = LyricsState.NoSong
            return
        }
        if (!s.isRemote) {
            job?.cancel()
            _state.value = LyricsState.NotFound
            return
        }
        launch {
            _state.value = LyricsState.Loading
            val lines = try {
                subsonic.lyrics(s)
            } catch (e: SubsonicException) {
                Log.w(TAG, "Couldn't load lyrics from the server", e)
                _state.value = LyricsState.Failed
                return@launch
            }
            _state.value = if (lines != null) LyricsState.Found(lines) else LyricsState.NotFound
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
