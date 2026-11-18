package com.udeshcoffee.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.playback.PlayerUiState
import org.koin.compose.koinInject

@Composable
fun rememberPlayerState(): State<PlayerUiState> {
    val connection: PlayerConnection = koinInject()
    return connection.state.collectAsStateWithLifecycle()
}

/** ID of the playing song, for highlighting it in lists. */
@Composable
fun rememberCurrentSongId(): Long? {
    val state by rememberPlayerState()
    return state.song?.id
}
