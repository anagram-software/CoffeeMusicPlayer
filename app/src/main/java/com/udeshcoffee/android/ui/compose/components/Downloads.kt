package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.data.download.DownloadRepository
import com.udeshcoffee.android.data.download.DownloadStatus
import com.udeshcoffee.android.data.download.downloadSummary
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.LocalAppActions
import org.koin.compose.koinInject

/** IDs of the fully downloaded server songs (provided once by the app scaffold, for list badges). */
val LocalDownloadedIds = compositionLocalOf { emptySet<String>() }

/**
 * Download action for a set of server songs (an album, a playlist), for detail screen top bars:
 * "Download" when not (fully) downloaded, a progress ring while downloading (tap to stop), and
 * "Downloaded" when every song is saved (tap to remove, after confirming). Nothing for device songs.
 */
@Composable
fun DownloadButton(songs: List<Song>) {
    if (songs.none { it.isRemote }) return
    val repository: DownloadRepository = koinInject()
    val actions = LocalAppActions.current
    val downloads by repository.downloads.collectAsStateWithLifecycle()
    val summary = remember(songs, downloads) { downloadSummary(songs, downloads) }
    when {
        summary.allDone -> IconButton(onClick = { actions.removeDownloads(songs) }) {
            Icon(Icons.Default.DownloadDone, contentDescription = "Downloaded, tap to remove", tint = MaterialTheme.colorScheme.primary)
        }
        // Stops the unfinished downloads; finished songs stay.
        summary.isDownloading -> IconButton(onClick = { actions.removeDownloads(songs.filter { downloads[it.remoteId]?.status != DownloadStatus.DONE }) }) {
            CircularProgressIndicator(progress = { summary.progress }, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        }
        else -> IconButton(onClick = { actions.download(songs) }) {
            Icon(Icons.Default.Download, contentDescription = "Download")
        }
    }
}
