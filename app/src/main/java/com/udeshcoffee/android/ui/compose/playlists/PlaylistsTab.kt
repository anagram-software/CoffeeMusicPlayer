package com.udeshcoffee.android.ui.compose.playlists

import com.udeshcoffee.android.ui.compose.components.rememberOptionsAnchor
import com.udeshcoffee.android.ui.compose.components.PageLoadingIndicator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.PageBottomPadding
import com.udeshcoffee.android.ui.compose.components.PlaylistArtwork
import com.udeshcoffee.android.ui.compose.components.icon
import com.udeshcoffee.android.ui.compose.components.songCount
import com.udeshcoffee.android.ui.compose.detail.PlaylistsViewModel
import com.udeshcoffee.android.ui.compose.detail.ServerNotice
import org.koin.compose.viewmodel.koinViewModel

/** The Library's Playlists tab: the current music source's playlists, with "New playlist" on top. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsTab(viewModel: PlaylistsViewModel = koinViewModel()) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val list = playlists
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PageLoadingIndicator() }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = PageBottomPadding)) {
        item {
            ListItem(
                    onClick = { actions.newPlaylist() },
                    leadingContent = {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp).padding(6.dp))
                    }) { Text("New playlist") }
        }
        error?.let { item { ServerNotice("Couldn't load playlists: $it", viewModel::retry) } }
        if (list.isEmpty() && error == null) {
            item {
                ListItem {
                    Text("No playlists yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(list, key = { "${it.type}:${it.id}:${it.remoteId}" }) { playlist ->
            // Separates the smart playlists from the user's (server playlists are all user playlists).
            if (playlist.type == Playlist.USER && playlist != list.first() && list.firstOrNull { it.type == Playlist.USER } == playlist) HorizontalDivider()
            val rowAnchor = rememberOptionsAnchor()
            val buttonAnchor = rememberOptionsAnchor()
            ListItem(
                    onClick = { actions.openPlaylist(playlist) },
                    onLongClick = rowAnchor.openAtTouch { actions.playlistOptions(playlist) },
                    modifier = rowAnchor.modifier,
                    leadingContent = {
                        PlaylistArtwork(playlist, Modifier.size(40.dp)) { modifier ->
                            Icon(playlist.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                                    modifier = modifier.padding(6.dp))
                        }
                    },
                    supportingContent = {
                        val details = listOfNotNull(playlist.songCount?.let { songCount(it) }, "read-only".takeIf { playlist.readonly })
                        if (details.isNotEmpty()) Text(details.joinToString(" · "))
                    },
                    trailingContent = {
                        IconButton(onClick = buttonAnchor.open { actions.playlistOptions(playlist) }, modifier = buttonAnchor.modifier) { Icon(Icons.Default.MoreVert, contentDescription = "Playlist options") }
                    }) { Text(playlist.title) }
        }
    }
}
