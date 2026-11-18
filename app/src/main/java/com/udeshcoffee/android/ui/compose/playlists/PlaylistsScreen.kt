package com.udeshcoffee.android.ui.compose.playlists

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.udeshcoffee.android.ui.compose.components.TopLevelScaffold
import com.udeshcoffee.android.ui.compose.components.songCount
import com.udeshcoffee.android.ui.compose.detail.PlaylistsViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.TextButton
import com.udeshcoffee.android.ui.compose.AppActions
import com.udeshcoffee.android.ui.compose.detail.ServerNotice

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsScreen(viewModel: PlaylistsViewModel = koinViewModel()) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val serverList by viewModel.serverPlaylists.collectAsStateWithLifecycle()
    val serverError by viewModel.error.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    TopLevelScaffold("Playlists", floatingActionButton = {
        ExtendedFloatingActionButton(onClick = { actions.newPlaylist() }, icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New playlist") })
    }) { padding ->
        val list = playlists
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@TopLevelScaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
            items(list, key = { "${it.type}:${it.id}" }) { playlist ->
                if (playlist.type == Playlist.USER && list.firstOrNull { it.type == Playlist.USER } == playlist) HorizontalDivider()
                ListItem(
                        modifier = Modifier.combinedClickable(onClick = { actions.openPlaylist(playlist) },
                                onLongClick = { actions.playlistOptions(playlist) }),
                        leadingContent = {
                            Icon(when (playlist.type) {
                                Playlist.MOST_PLAYED -> Icons.Default.TrendingUp
                                Playlist.RECENTLY_PLAYED -> Icons.Default.History
                                Playlist.RECENTLY_ADDED -> Icons.Default.NewReleases
                                else -> Icons.AutoMirrored.Filled.PlaylistPlay
                            }, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp).padding(6.dp))
                        },
                        headlineContent = { Text(playlist.title) },
                        supportingContent = { playlist.songCount?.let { Text(songCount(it)) } },
                        trailingContent = {
                            IconButton(onClick = { actions.playlistOptions(playlist) }) { Icon(Icons.Default.MoreVert, contentDescription = "Playlist options") }
                        })
            }
            serverPlaylists(account?.name, serverList, serverError, viewModel::retry, actions)
        }
    }
}

/** The music server's playlists, below the device's. */
private fun LazyListScope.serverPlaylists(serverName: String?, playlists: List<Playlist>?, error: String?, onRetry: () -> Unit, actions: AppActions) {
    serverName ?: return
    item {
        HorizontalDivider()
        ListItem(
                headlineContent = { Text("On $serverName", style = MaterialTheme.typography.titleMedium) },
                trailingContent = { TextButton(onClick = actions::newServerPlaylist) { Text("New") } })
    }
    when {
        error != null -> item { ServerNotice("Couldn't load server playlists: $error", onRetry) }
        playlists == null -> item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        playlists.isEmpty() -> item { ListItem(headlineContent = { Text("No playlists on the server", color = MaterialTheme.colorScheme.onSurfaceVariant) }) }
        else -> items(playlists, key = { "server:${it.remoteId}" }) { playlist ->
            ListItem(
                    modifier = Modifier.combinedClickable(onClick = { actions.openPlaylist(playlist) }, onLongClick = { actions.playlistOptions(playlist) }),
                    leadingContent = {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp).padding(6.dp))
                    },
                    headlineContent = { Text(playlist.title) },
                    supportingContent = { Text(listOfNotNull(playlist.songCount?.let { songCount(it) }, "read-only".takeIf { playlist.readonly }).joinToString(" · ")) },
                    trailingContent = {
                        IconButton(onClick = { actions.playlistOptions(playlist) }) { Icon(Icons.Default.MoreVert, contentDescription = "Playlist options") }
                    })
        }
    }
}
