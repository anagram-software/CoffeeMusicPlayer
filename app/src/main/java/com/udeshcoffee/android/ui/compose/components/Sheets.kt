package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.udeshcoffee.android.ui.compose.AppActions
import com.udeshcoffee.android.data.download.DownloadRepository
import com.udeshcoffee.android.data.download.DownloadStatus
import com.udeshcoffee.android.data.download.downloadSummary
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import com.udeshcoffee.android.ui.compose.OptionsSheet

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
            onClick = onClick,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { Icon(icon, contentDescription = null) }) { Text(label) }
}

/** Song or collection options, replacing the old long-press dialogs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheetHost(actions: AppActions) {
    val sheet = actions.sheet ?: return
    val dismiss = { actions.sheet = null }
    val downloadRepository: DownloadRepository = koinInject()
    val downloads by downloadRepository.downloads.collectAsStateWithLifecycle()
    fun run(block: () -> Unit) = { dismiss(); block() }
    ModalBottomSheet(onDismissRequest = dismiss) {
        // Space below the last option (above the navigation bar), as pages have (PageBottomPadding).
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = PageBottomPadding)) {
            when (sheet) {
                is OptionsSheet.ForSong -> {
                    val song = sheet.song
                    ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { SongArtwork(song, Modifier.size(48.dp)) },
                            supportingContent = { Text("${song.artistName} • ${song.albumName}", maxLines = 1, overflow = TextOverflow.Ellipsis) }) { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    HorizontalDivider()
                    SheetAction(Icons.Default.PlayArrow, "Play", run { actions.play(listOf(song)) })
                    SheetAction(Icons.Default.SkipNext, "Play next", run { actions.queue(listOf(song), next = true) })
                    SheetAction(QueueIcon, "Add to queue", run { actions.queue(listOf(song), next = false) })
                    SheetAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist", run { actions.addToPlaylist(listOf(song)) })
                    sheet.playlist?.takeIf { !it.readonly }?.let { playlist ->
                        SheetAction(Icons.Default.RemoveCircleOutline, "Remove from ${playlist.title}",
                                run { actions.removeFromPlaylist(playlist, song) })
                    }
                    SheetAction(Icons.Default.Favorite, "Favorite / unfavorite", run { actions.toggleFavorite(song) })
                    // Offline listening, for server songs.
                    if (song.isRemote) when (downloads[song.remoteId]?.status) {
                        DownloadStatus.DONE -> SheetAction(Icons.Default.DownloadDone, "Remove download", run { actions.removeDownloads(listOf(song)) })
                        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING ->
                            SheetAction(Icons.Default.Close, "Stop download", run { actions.removeDownloads(listOf(song)) })
                        else -> SheetAction(Icons.Default.Download, "Download", run { actions.download(listOf(song)) })
                    }
                    if (!song.isRemote || song.albumRemoteId != null)
                        SheetAction(Icons.Default.Album, "Go to album", run { actions.openAlbum(song) })
                    if (!song.isRemote || song.artistRemoteId != null)
                        SheetAction(Icons.Default.Person, "Go to artist", run { actions.openArtist(song) })
                    // File operations only make sense for songs on this device.
                    if (!song.isRemote) {
                        SheetAction(Icons.Default.Edit, "Edit tags", run { actions.openEditor(song) })
                        SheetAction(Icons.Default.Share, "Share", run { actions.share(song) })
                        SheetAction(Icons.Default.Delete, "Delete from device", run { actions.deleteSongs(listOf(song)) })
                    }
                }
                is OptionsSheet.ForCollection -> {
                    ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            supportingContent = { Text(songCount(sheet.songs.size)) }) { Text(sheet.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    HorizontalDivider()
                    SheetAction(Icons.Default.PlayArrow, "Play", run { actions.play(sheet.songs) })
                    SheetAction(Icons.Default.SkipNext, "Play next", run { actions.queue(sheet.songs, next = true) })
                    SheetAction(QueueIcon, "Add to queue", run { actions.queue(sheet.songs, next = false) })
                    SheetAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist",
                            run { actions.addToPlaylist(sheet.songs, sheet.playlist?.id) })
                    SheetAction(Icons.Default.Favorite, "Add to favorites", run { actions.addToFavorites(sheet.songs) })
                    val summary = downloadSummary(sheet.songs, downloads)
                    if (summary.total > 0 && summary.done + summary.active < summary.total)
                        SheetAction(Icons.Default.Download, "Download", run { actions.download(sheet.songs) })
                    if (summary.done + summary.active > 0)
                        SheetAction(Icons.Default.DownloadDone, "Remove downloads", run { actions.removeDownloads(sheet.songs) })
                    sheet.playlist?.takeIf { !it.readonly }?.let { playlist ->
                        SheetAction(Icons.Default.Edit, "Rename playlist", run { actions.renamePlaylist(playlist) })
                        SheetAction(Icons.Default.Delete, "Delete playlist", run { actions.deletePlaylist(playlist) })
                    }
                }
            }
        }
    }
}
