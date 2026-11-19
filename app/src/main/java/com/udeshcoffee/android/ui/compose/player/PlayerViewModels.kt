package com.udeshcoffee.android.ui.compose.player

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.model.Lyric
import com.udeshcoffee.android.data.remote.genius.Result
import com.udeshcoffee.android.extensions.toLastFMArtistQuery
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
import androidx.core.text.htmlEncode
import com.udeshcoffee.android.data.subsonic.SubsonicRepository

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
    object Searching : LyricsState
    data class Found(val html: String) : LyricsState
    object NotFound : LyricsState
    data class Choose(val results: List<Result>) : LyricsState
}

/**
 * Lyrics for the playing song: ones saved in the app, then (for server songs) the server's own,
 * else a Genius search.
 */
class LyricsViewModel(
        private val player: PlayerConnection,
        private val dataRepository: DataRepository,
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

    private fun launch(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch { block() }
    }

    private fun load(s: Song?) {
        if (s == null) {
            job?.cancel()
            _state.value = LyricsState.NoSong
            return
        }
        launch {
            _state.value = LyricsState.Loading
            val stored = dataRepository.getLyrics(s)
            val server = if (stored == null && s.isRemote) runCatching { subsonic.lyrics(s) }.getOrNull() else null
            when {
                stored != null -> _state.value = LyricsState.Found(stored)
                server != null -> _state.value = LyricsState.Found(server.joinToString("<br/>") { it.htmlEncode() })
                else -> search(s, s.title, s.artistName, exact = true)
            }
        }
    }

    private suspend fun search(s: Song, title: String, artist: String, exact: Boolean) {
        _state.value = LyricsState.Searching
        val hits = dataRepository.searchLyrics(title, artist.toLastFMArtistQuery(), exact)
        if (song?.id != s.id) return
        when {
            hits.isEmpty() -> _state.value = LyricsState.NotFound
            hits.size == 1 -> download(s, hits[0].result.path)
            else -> _state.value = LyricsState.Choose(hits.map { it.result })
        }
    }

    private suspend fun download(s: Song, path: String) {
        _state.value = LyricsState.Loading
        val lyrics = dataRepository.loadLyrics(s.id, path)
        if (song?.id != s.id) return
        _state.value = if (lyrics != null) LyricsState.Found(lyrics) else LyricsState.NotFound
    }

    /** Manual search (title/artist edited by the user). */
    fun search(title: String, artist: String) {
        val s = song ?: return
        launch { search(s, title, artist, exact = false) }
    }

    fun choose(result: Result) {
        val s = song ?: return
        launch { download(s, result.path) }
    }

    fun saveCustom(text: String) {
        val s = song ?: return
        val html = text.trim().replace("\n", "<br/>")
        _state.value = LyricsState.Found(html)
        viewModelScope.launch { dataRepository.setLyrics(Lyric(s.id, html)) }
    }

    fun retry() = load(song)

    fun cycleSize() {
        _size.value = _size.value % 3 + 1
        prefs.edit { putInt(PreferenceUtil.LYRICS_SIZE, _size.value) }
    }

    fun currentSong(): Song? = song
}
