package com.udeshcoffee.android.ui.compose.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.lastfm.LastFmAccount
import com.udeshcoffee.android.data.lastfm.LastFmException
import com.udeshcoffee.android.data.lastfm.LastFmStats
import com.udeshcoffee.android.data.lastfm.loadStats
import com.udeshcoffee.android.data.lastfm.LastFmScrobbler
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.utils.CoverArt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LastFmStatsState {
    /** Not connected to Last.fm: nothing is shown. */
    object Hidden : LastFmStatsState
    object Loading : LastFmStatsState
    data class Loaded(val stats: LastFmStats) : LastFmStatsState
    data class Failed(val message: String) : LastFmStatsState
}

/**
 * The connected Last.fm user's stats for Home. Fetched from Last.fm only when the account
 * connects, the app starts, and the user navigates to Home ([refresh]). In between, the listen
 * count is kept locally: each scrobble Last.fm accepts adds to it, and a fetch never lowers it
 * (Last.fm can lag behind a scrobble it just accepted).
 */
class LastFmStatsViewModel(private val account: LastFmAccount, scrobbler: LastFmScrobbler, private val sourceStore: MusicSourceStore,
                           private val subsonic: SubsonicRepository) : ViewModel() {

    private val _state = MutableStateFlow<LastFmStatsState>(LastFmStatsState.Hidden)
    val state: StateFlow<LastFmStatsState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            account.user.collect { user ->
                if (user == null) { job?.cancel(); _state.value = LastFmStatsState.Hidden } else load()
            }
        }
        viewModelScope.launch {
            scrobbler.accepted.collect { count ->
                (_state.value as? LastFmStatsState.Loaded)?.let { loaded ->
                    _state.value = LastFmStatsState.Loaded(loaded.stats.copy(scrobbles = loaded.stats.scrobbles + count))
                }
            }
        }
    }

    /** Fetches the stats from Last.fm (on navigating to Home, or Retry); keeps showing the old ones meanwhile. */
    fun refresh() = load()

    private fun load() {
        if (!account.isConnected || job?.isActive == true) return
        if (_state.value !is LastFmStatsState.Loaded) _state.value = LastFmStatsState.Loading
        job = viewModelScope.launch {
            try {
                val stats = account.loadStats(artistArtwork = ::serverArtistArtwork) ?: return@launch
                // Keep the local count if it's ahead (scrobbles Last.fm hasn't counted yet).
                val local = (_state.value as? LastFmStatsState.Loaded)?.stats?.takeIf { it.name == stats.name }?.scrobbles ?: 0
                _state.value = LastFmStatsState.Loaded(stats.copy(scrobbles = maxOf(stats.scrobbles, local)))
            } catch (e: LastFmException) {
                Log.w(TAG, "Couldn't load Last.fm stats", e)
                // Keep stale stats rather than replacing them with an error.
                if (_state.value !is LastFmStatsState.Loaded) _state.value = LastFmStatsState.Failed(e.message ?: "Couldn't load Last.fm")
            }
        }
    }

    /** The music server's picture of the artist named [name] (an exact name match from search3), as a [CoverArt] model. */
    private suspend fun serverArtistArtwork(name: String): Any? {
        if (!sourceStore.isServer || !subsonic.isSignedIn) return null
        val artists = subsonic.search(name).artists
        val match = artists.firstOrNull { it.name.equals(name, ignoreCase = true) && it.coverArt != null } ?: return null
        return CoverArt(match.coverArt!!)
    }

    private companion object {
        const val TAG = "LastFmStatsViewModel"

    }
}
