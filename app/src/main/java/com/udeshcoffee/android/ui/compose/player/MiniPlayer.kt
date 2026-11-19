package com.udeshcoffee.android.ui.compose.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.ui.compose.components.SongArtwork
import com.udeshcoffee.android.ui.compose.rememberPlayerState
import org.koin.compose.koinInject

/** Collapsed player shown above the navigation bar. Tap to expand; the caller adds the drag-up gesture ([playerSheetDraggable]). */
@Composable
fun MiniPlayer(onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val state by rememberPlayerState()
    val song = state.song ?: return
    val player: PlayerConnection = koinInject()
    val position by remember(song.id, state.isPlaying) { player.progress(500) }.collectAsState(initial = player.currentPosition)

    Surface(
            tonalElevation = 3.dp,
            modifier = modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .semantics { contentDescription = "Open player" }) {
        Column {
            LinearProgressIndicator(
                    progress = { if (state.duration > 0) (position.toFloat() / state.duration).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth())
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SongArtwork(song, Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artistName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { if (state.isPlaying) player.pause() else player.play() }) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play")
                }
                IconButton(onClick = { player.next() }) { Icon(Icons.Default.SkipNext, contentDescription = "Next") }
            }
        }
    }
}
