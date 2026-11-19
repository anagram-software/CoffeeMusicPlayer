package com.udeshcoffee.android.ui.compose.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.data.lyrics.LyricLine
import com.udeshcoffee.android.data.lyrics.Lyrics
import com.udeshcoffee.android.data.lyrics.activeLineIndex
import com.udeshcoffee.android.data.lyrics.isSectionHeader
import com.udeshcoffee.android.playback.PlayerConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Lyrics for the playing song (the music server's, else LRCLIB's), with a text size toggle. Synced lyrics follow
 * playback (current line highlighted and kept centred; tap a line to seek), plain lyrics just scroll.
 */
@Composable
fun LyricsPanel(modifier: Modifier = Modifier, viewModel: LyricsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val size by viewModel.size.collectAsStateWithLifecycle()

    // One step above the player's background, so the lyrics read as a card on it.
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Lyrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::cycleSize) { Icon(Icons.Default.TextFields, contentDescription = "Text size") }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (val s = state) {
                    LyricsState.NoSong -> Text("Nothing playing")
                    LyricsState.Loading -> Text("Loading…")
                    LyricsState.NotFound -> Text("No lyrics found for this song", textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    LyricsState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load lyrics")
                        TextButton(onClick = viewModel::retry) { Text("Try again") }
                    }
                    is LyricsState.Found ->
                        if (s.lyrics.synced) SyncedLyrics(s.lyrics.lines, size)
                        else PlainLyrics(s.lyrics.lines, size)
                }
                // LRCLIB's lyrics are community contributed: credit the source.
                if ((state as? LyricsState.Found)?.fromLrclib == true) {
                    Text("Lyrics from LRCLIB", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 6.dp))
                }
            }
        }
    }
}

private fun fontSize(size: Int): TextUnit = when (size) { 3 -> 30.sp; 2 -> 25.sp; else -> 21.sp }
private fun plainFontSize(size: Int): TextUnit = when (size) { 3 -> 24.sp; 2 -> 20.sp; else -> 16.sp }

/** How long after the user scrolls the lyrics before auto scrolling takes over again. */
private const val USER_SCROLL_HOLD_MS = 4_000L

@Composable
private fun SyncedLyrics(lines: List<LyricLine>, size: Int) {
    val player: PlayerConnection = koinInject()
    // Map positions to the line index, so the list only recomposes when the line changes.
    val active by remember(lines) {
        player.progress(100).map { activeLineIndex(lines, it) }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = activeLineIndex(lines, player.currentPosition))
    val listState = rememberLazyListState()
    var userScrolledAt by remember { mutableLongStateOf(0L) }

    // A drag by the user pauses auto scrolling for a few seconds.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start || it is DragInteraction.Stop) userScrolledAt = System.currentTimeMillis()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val halfHeight = maxHeight / 2
        val halfHeightPx = with(LocalDensity.current) { halfHeight.roundToPx() }

        // Jump to the current line when the lyrics appear, then follow it with an animation
        // (after any user scroll hold). Resizing recentres too.
        val jumped = remember(lines) { BooleanArray(1) }
        LaunchedEffect(lines, active, userScrolledAt, halfHeightPx) {
            val wait = USER_SCROLL_HOLD_MS - (System.currentTimeMillis() - userScrolledAt)
            if (wait > 0) delay(wait)
            listState.centre(active.coerceAtLeast(0), animate = jumped[0])
            jumped[0] = true
        }

        LazyColumn(
                state = listState,
                // Half a viewport of padding at each end lets the first and last lines reach the centre.
                contentPadding = PaddingValues(vertical = halfHeight),
                modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.15f to Color.Black,
                                    0.85f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                        }) {
            itemsIndexed(lines) { i, line ->
                SyncedLine(line, i == active, i < active, size) {
                    userScrolledAt = 0L
                    line.startMs?.let { player.currentPosition = it }
                }
            }
        }
    }
}

/**
 * Scrolls so line [index] sits in the middle of the viewport. With half a viewport of content
 * padding, offset 0 puts the item's top at the centre, so the offset is half the item's height
 * (measured if visible, else estimated from the visible lines).
 */
private suspend fun LazyListState.centre(index: Int, animate: Boolean) {
    val visible = layoutInfo.visibleItemsInfo
    val height = visible.firstOrNull { it.index == index }?.size
            ?: visible.takeIf { it.isNotEmpty() }?.let { v -> v.sumOf { it.size } / v.size }
            ?: 0
    if (animate) animateScrollToItem(index, height / 2) else scrollToItem(index, height / 2)
    // The estimate may be off for a line that wasn't visible; correct once it's laid out.
    val actual = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size ?: return
    if (actual != height) {
        if (animate) animateScrollToItem(index, actual / 2) else scrollToItem(index, actual / 2)
    }
}

@Composable
private fun SyncedLine(line: LyricLine, active: Boolean, past: Boolean, size: Int, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val color by animateColorAsState(
            when {
                active -> onSurface
                past -> onSurface.copy(alpha = 0.45f)
                else -> onSurface.copy(alpha = 0.3f)
            }, label = "lyric colour")
    val scale by animateFloatAsState(if (active) 1.04f else 1f, label = "lyric scale")
    Text(
            line.text.ifBlank { "♪" },
            color = color,
            fontSize = fontSize(size),
            lineHeight = fontSize(size) * 1.25f,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    })
}

@Composable
private fun PlainLyrics(lines: List<LyricLine>, size: Int) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
        itemsIndexed(lines) { _, line ->
            val header = isSectionHeader(line.text)
            Text(
                    line.text,
                    fontSize = plainFontSize(size),
                    lineHeight = plainFontSize(size) * 1.5f,
                    fontWeight = if (header) FontWeight.Medium else null,
                    color = if (header) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = if (header) 12.dp else 0.dp))
        }
    }
}
