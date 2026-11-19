package com.udeshcoffee.android.ui.compose.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.playback.PlayerUiState
import com.udeshcoffee.android.playback.RepeatModes
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.SongArtwork
import com.udeshcoffee.android.ui.compose.components.formatDuration
import com.udeshcoffee.android.ui.compose.rememberPlayerState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Full-screen player, shown in [PlayerSheetHost], which handles dragging down and back to collapse.
 * [onCollapse] returns to the mini player (close button, or when the queue empties).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(onCollapse: () -> Unit, viewModel: NowPlayingViewModel = koinViewModel()) {
    val state by rememberPlayerState()
    val song = state.song
    var showQueue by remember { mutableStateOf(false) }
    // Back leaves the queue first; otherwise the sheet's predictive back collapses the player.
    BackHandler(enabled = showQueue) { showQueue = false }
    // Collapse only when the queue is really gone, not on transient null songs during track changes.
    LaunchedEffect(state.connected, state.queue.isEmpty()) { if (state.connected && state.queue.isEmpty()) onCollapse() }
    song ?: return

    // Lyrics come from the music server only, so device songs always show their artwork.
    val lyricsPreferred by viewModel.showLyrics.collectAsStateWithLifecycle()
    val showLyrics = lyricsPreferred && song.isRemote
    val actions = LocalAppActions.current

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).safeDrawingPadding()) {
        TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = { IconButton(onClick = onCollapse) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close player") } },
                title = { Text(if (showQueue) "Up next" else "Now playing", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    if (showQueue) {
                        IconButton(onClick = { actions.addToPlaylist(state.queue) }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Save queue as playlist")
                        }
                        val player: PlayerConnection = koinInject()
                        IconButton(onClick = { player.clearQueue() }) { Icon(Icons.Default.ClearAll, contentDescription = "Clear queue") }
                    } else {
                        IconButton(onClick = { actions.songOptions(song) }) { Icon(Icons.Default.MoreVert, contentDescription = "Song options") }
                    }
                })
        if (showQueue) {
            QueueList(state, Modifier.weight(1f))
            TextButton(onClick = { showQueue = false }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp)) { Text("Back to player") }
            return@Column
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight
            if (wide) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArtOrLyrics(song, showLyrics, Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
                    Column(Modifier.weight(1f)) { Controls(state, song, viewModel, showLyrics) { showQueue = true } }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                    ArtOrLyrics(song, showLyrics, Modifier.fillMaxWidth().weight(1f, fill = false).aspectRatio(1f))
                    Controls(state, song, viewModel, showLyrics) { showQueue = true }
                }
            }
        }
    }
}

@Composable
private fun ArtOrLyrics(song: Song, showLyrics: Boolean, modifier: Modifier) {
    AnimatedContent(showLyrics, modifier = modifier, label = "artwork") { lyrics ->
        if (lyrics) LyricsPanel(Modifier.fillMaxSize())
        else SongArtwork(song, Modifier.fillMaxSize(), MaterialTheme.shapes.large)
    }
}

@Composable
private fun Controls(state: PlayerUiState, song: Song, viewModel: NowPlayingViewModel, showLyrics: Boolean, onQueue: () -> Unit) {
    val player: PlayerConnection = koinInject()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val position by remember(song.id, state.isPlaying) { player.progress(250) }.collectAsState(initial = player.currentPosition)
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val duration = state.duration.coerceAtLeast(1)

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artistName} • ${song.albumName}", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconToggleButton(checked = isFavorite, onCheckedChange = { viewModel.toggleFavorite() }) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites")
            }
        }
        Slider(
                value = if (seeking) seekValue else (position.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { seeking = true; seekValue = it },
                onValueChangeFinished = { player.currentPosition = (seekValue * duration).toLong(); seeking = false })
        Row {
            Text(formatDuration(if (seeking) (seekValue * duration).toLong() else position), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(formatDuration(state.duration), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconToggleButton(checked = state.shuffle, onCheckedChange = { player.shuffleEnabled = it }) {
                Icon(Icons.Default.Shuffle, contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off",
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { player.previous() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(onClick = { if (state.isPlaying) player.pause() else player.play() }, modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors()) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play", modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { player.next() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
            }
            val repeat = state.repeatMode
            IconButton(onClick = {
                player.repeatMode = when (repeat) {
                    RepeatModes.PREF_NONE -> RepeatModes.PREF_ALL
                    RepeatModes.PREF_ALL -> RepeatModes.PREF_ONE
                    else -> RepeatModes.PREF_NONE
                }
            }) {
                Icon(if (repeat == RepeatModes.PREF_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = when (repeat) {
                            RepeatModes.PREF_ONE -> "Repeat one"
                            RepeatModes.PREF_ALL -> "Repeat all"
                            else -> "Repeat off"
                        },
                        tint = if (repeat == RepeatModes.PREF_NONE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (song.isRemote) TextButton(onClick = { viewModel.toggleLyrics() }) {
                Icon(Icons.Default.Lyrics, contentDescription = null); Spacer(Modifier.size(8.dp))
                Text(if (showLyrics) "Artwork" else "Lyrics")
            }
            TextButton(onClick = onQueue) {
                Icon(com.udeshcoffee.android.ui.compose.components.QueueIcon, contentDescription = null); Spacer(Modifier.size(8.dp))
                Text("Up next")
            }
        }
    }
}

/** The play queue: tap to play, drag to reorder, swipe to remove. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueList(state: PlayerUiState, modifier: Modifier) {
    val player: PlayerConnection = koinInject()
    val actions = LocalAppActions.current
    // Local copy so drags are smooth; moves are sent to the player as they happen.
    val items = remember { mutableStateListOf<Pair<Int, Song>>() }
    LaunchedEffect(state.queue) {
        items.clear()
        items.addAll(state.queue.mapIndexed { i, s -> i to s })
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.index.coerceAtLeast(0))
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        items.add(to.index, items.removeAt(from.index))
        player.moveInQueue(from.index, to.index)
    }
    Text("${state.index + 1} of ${state.queue.size}", style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    LazyColumn(modifier.fillMaxWidth(), state = listState) {
        itemsIndexed(items, key = { _, item -> item.first }) { index, (key, song) ->
            ReorderableItem(reorder, key = key) { _ ->
                val dismiss = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismiss.currentValue) {
                    if (dismiss.currentValue != androidx.compose.material3.SwipeToDismissBoxValue.Settled) {
                        if (state.queue.size > 1) player.removeFromQueue(index) else player.clearQueue()
                    }
                }
                SwipeToDismissBox(dismiss, backgroundContent = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer)) }) {
                    ListItem(
                            modifier = Modifier.clickable { player.playAt(index) },
                            colors = if (index == state.index) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            else ListItemDefaults.colors(),
                            leadingContent = { SongArtwork(song, Modifier.size(44.dp)) },
                            headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(song.artistName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { actions.songOptions(song) }) { Icon(Icons.Default.MoreVert, contentDescription = "Song options") }
                                    Icon(Icons.Default.DragHandle, contentDescription = "Reorder",
                                            modifier = Modifier.padding(12.dp).draggableHandle())
                                }
                            })
                }
            }
        }
    }
}

/** The music server's lyrics for the playing song, with a text size toggle. */
@Composable
fun LyricsPanel(modifier: Modifier = Modifier, viewModel: LyricsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val size by viewModel.size.collectAsStateWithLifecycle()

    androidx.compose.material3.Surface(modifier, shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Lyrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::cycleSize) { Icon(Icons.Default.TextFields, contentDescription = "Text size") }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                when (val s = state) {
                    LyricsState.NoSong -> Text("Nothing playing")
                    LyricsState.Loading -> Text("Loading…")
                    LyricsState.NotFound -> Text("No lyrics on the server for this song", textAlign = TextAlign.Center)
                    LyricsState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load lyrics")
                        TextButton(onClick = viewModel::retry) { Text("Try again") }
                    }
                    is LyricsState.Found -> {
                        val text = remember(s.lines) { s.lines.joinToString("\n") }
                        Text(text, fontSize = when (size) { 3 -> 24.sp; 2 -> 20.sp; else -> 16.sp },
                                lineHeight = when (size) { 3 -> 34.sp; 2 -> 28.sp; else -> 24.sp },
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp))
                    }
                }
            }
        }
    }
}
