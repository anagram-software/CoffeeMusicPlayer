package com.udeshcoffee.android.ui.compose.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.metabrainz.ListenBrainzAccount
import com.udeshcoffee.android.data.metabrainz.ListenBrainzScrobbler
import com.udeshcoffee.android.data.metabrainz.ListenBrainzStats
import com.udeshcoffee.android.data.metabrainz.MetaBrainzException
import com.udeshcoffee.android.data.metabrainz.loadStats
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.utils.CoverArt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ListenBrainzStatsState {
    /** Not connected to ListenBrainz: nothing is shown. */
    object Hidden : ListenBrainzStatsState
    object Loading : ListenBrainzStatsState
    data class Loaded(val stats: ListenBrainzStats) : ListenBrainzStatsState
    data class Failed(val message: String) : ListenBrainzStatsState
}

/**
 * The connected ListenBrainz user's stats for Home. Fetched from ListenBrainz only when the
 * account connects, the app starts, and the user navigates to Home ([refresh]). In between, the
 * listen count is kept locally: each listen ListenBrainz accepts adds to it, and a fetch never
 * lowers it (ListenBrainz can lag behind a listen it just accepted).
 */
class ListenBrainzStatsViewModel(private val account: ListenBrainzAccount, scrobbler: ListenBrainzScrobbler, private val sourceStore: MusicSourceStore,
                           private val subsonic: SubsonicRepository) : ViewModel() {

    private val _state = MutableStateFlow<ListenBrainzStatsState>(ListenBrainzStatsState.Hidden)
    val state: StateFlow<ListenBrainzStatsState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            account.user.collect { user ->
                if (user == null) { job?.cancel(); _state.value = ListenBrainzStatsState.Hidden } else load()
            }
        }
        viewModelScope.launch {
            scrobbler.accepted.collect { count ->
                (_state.value as? ListenBrainzStatsState.Loaded)?.let { loaded ->
                    _state.value = ListenBrainzStatsState.Loaded(loaded.stats.copy(listens = loaded.stats.listens + count))
                }
            }
        }
    }

    /** Fetches the stats from ListenBrainz (on navigating to Home, or Retry); keeps showing the old ones meanwhile. */
    fun refresh() = load()

    private fun load() {
        if (!account.isConnected || job?.isActive == true) return
        if (_state.value !is ListenBrainzStatsState.Loaded) _state.value = ListenBrainzStatsState.Loading
        job = viewModelScope.launch {
            try {
                val stats = account.loadStats(artistArtwork = ::serverArtistArtwork) ?: return@launch
                // Keep the local count if it's ahead (listens ListenBrainz hasn't counted yet).
                val local = (_state.value as? ListenBrainzStatsState.Loaded)?.stats?.takeIf { it.name == stats.name }?.listens ?: 0
                _state.value = ListenBrainzStatsState.Loaded(stats.copy(listens = maxOf(stats.listens, local)))
            } catch (e: MetaBrainzException) {
                Log.w(TAG, "Couldn't load ListenBrainz stats", e)
                // Keep stale stats rather than replacing them with an error.
                if (_state.value !is ListenBrainzStatsState.Loaded) _state.value = ListenBrainzStatsState.Failed(e.message ?: "Couldn't load ListenBrainz")
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
        const val TAG = "ListenBrainzStatsViewModel"

    }
}
