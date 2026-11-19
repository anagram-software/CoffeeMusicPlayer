package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.CardDefaults
import com.udeshcoffee.android.ui.compose.theme.rememberCardTint
import com.udeshcoffee.android.utils.CoverArt
import com.udeshcoffee.android.utils.artistImageFile
import com.udeshcoffee.android.utils.artworkData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.DownloadDone
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song

/** Song row: artwork, title, "artist • album", options menu. [isCurrent] highlights the playing song. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
        song: Song,
        onClick: () -> Unit,
        onMore: () -> Unit,
        modifier: Modifier = Modifier,
        isCurrent: Boolean = false,
        showArtwork: Boolean = true,
        leading: (@Composable () -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null
) {
    ListItem(
            onClick = onClick,
            onLongClick = onMore,
            modifier = modifier,
            colors = if (isCurrent) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            else ListItemDefaults.colors(),
            leadingContent = leading ?: if (showArtwork) {
                { SongArtwork(song, Modifier.size(48.dp)) }
            } else null,
            supportingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Saved for offline listening.
                    if (song.remoteId != null && song.remoteId in LocalDownloadedIds.current) {
                        Icon(Icons.Default.DownloadDone, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp).size(14.dp))
                    }
                    Text("${song.artistName} • ${song.albumName}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) Icon(Icons.Default.GraphicEq, contentDescription = "Playing", tint = MaterialTheme.colorScheme.primary)
                    trailing?.invoke()
                    IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, contentDescription = "Song options") }
                }
            }) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else null)
            }
}

/** "Shuffle all" row shown at the top of song lists. */
@Composable
fun ShuffleRow(count: Int, subtitle: String = songCount(count), onClick: () -> Unit) {
    ListItem(
            onClick = onClick,
            leadingContent = { Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(48.dp).padding(10.dp)) },
            supportingContent = { Text(subtitle) }) { Text("Shuffle all") }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit) = this.combinedClickable(onClick = onClick)

/**
 * Grid card with square artwork, title, subtitle and a play button. The text area takes the hue of
 * the artwork at [tintKey] (a Coil model, or a key for [tintLoad], which finds the model off the main thread).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
        title: String,
        subtitle: String,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        onPlay: () -> Unit,
        artwork: @Composable (Modifier) -> Unit,
        modifier: Modifier = Modifier,
        tintKey: Any? = null,
        tintLoad: (suspend () -> Any?)? = null
) {
    val container = rememberCardTint(tintKey, CardDefaults.cardColors().containerColor, tintLoad ?: { tintKey })
    Card(modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
            colors = CardDefaults.cardColors(containerColor = container, contentColor = MaterialTheme.colorScheme.onSurface)) {
        Box {
            artwork(Modifier.fillMaxWidth().aspectRatio(1f))
            // White with a dark icon whatever the page's accent, so it reads on any artwork.
            FilledTonalIconButton(onClick = onPlay, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play $title")
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit, onLongClick: () -> Unit, onPlay: () -> Unit, modifier: Modifier = Modifier) =
        MediaCard(album.title, "${album.artist} • ${songCount(album.songCount)}", onClick, onLongClick, onPlay,
                { AlbumArtwork(album, it, MaterialTheme.shapes.extraSmall) }, modifier, tintKey = album.artworkData())

@Composable
fun ArtistCard(artist: Artist, onClick: () -> Unit, onLongClick: () -> Unit, onPlay: () -> Unit, modifier: Modifier = Modifier, collectImage: Boolean = true) =
        // Server artists come with an album count, device artists with a song count.
        MediaCard(artist.name, if (artist.isRemote) albumCount(artist.songCount) else songCount(artist.songCount), onClick, onLongClick, onPlay,
                { ArtistArtwork(artist, it, MaterialTheme.shapes.extraSmall, collectImage) }, modifier,
                tintKey = artistTintKey(artist), tintLoad = { artistTintModel(artist) })

/** Server artists: their cover art. Device artists: their stored image, if any (checked on IO). */
private fun artistTintKey(artist: Artist): Any = (artist.coverArt ?: artist.remoteId)?.let { CoverArt(it) } ?: "artist:${artist.id}"

private suspend fun artistTintModel(artist: Artist): Any? =
        if (artist.isRemote) artistTintKey(artist)
        else withContext(Dispatchers.IO) { artistImageFile(artist.id).takeIf { it.exists() } }

/** Centered message for empty lists. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Default.LibraryMusic) {
    Column(modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        actions()
    }
}

/** Small icon + label row, e.g. a "Queue" text action. */
@Composable
fun IconLabel(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

val QueueIcon: ImageVector get() = Icons.AutoMirrored.Filled.QueueMusic

/** Space below the last item of a page, so it doesn't end flush against the mini player / navigation bar. */
val PageBottomPadding = 24.dp

/** Padding for page grids (Library category pages), with [PageBottomPadding] at the end. */
val GridPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp + PageBottomPadding)

/** [this] with [PageBottomPadding] added at the bottom (a Scaffold's inner padding for a page's list). */
@Composable
fun PaddingValues.withPageBottom(): PaddingValues {
    val direction = androidx.compose.ui.platform.LocalLayoutDirection.current
    return PaddingValues(start = calculateStartPadding(direction), top = calculateTopPadding(),
            end = calculateEndPadding(direction), bottom = calculateBottomPadding() + PageBottomPadding)
}

fun songCount(count: Int) = if (count == 1) "1 song" else "$count songs"

fun albumCount(count: Int) = if (count == 1) "1 album" else "$count albums"

/** "m:ss" or "h:mm:ss". */
fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(java.util.Locale.ROOT, "%d:%02d", m, s)
}

/** The loading indicator for pages and lists while their content loads (Material 3 Expressive's shape-morphing indicator). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageLoadingIndicator(modifier: Modifier = Modifier) = androidx.compose.material3.LoadingIndicator(modifier)
