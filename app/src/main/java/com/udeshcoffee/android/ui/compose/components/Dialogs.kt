package com.udeshcoffee.android.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.ui.compose.AppActions
import com.udeshcoffee.android.ui.compose.AppDialog
import org.koin.compose.koinInject

/** Dialog with a single text field (playlist names...). */
@Composable
fun TextInputDialog(title: String, label: String, initial: String = "", confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()); onDismiss() }) { Text(confirm) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** The playlist dialogs requested through [AppActions]. */
@Composable
fun AppDialogHost(actions: AppActions) {
    val dialog = actions.dialog ?: return
    val dismiss = { actions.dialog = null }
    when (dialog) {
        is AppDialog.AddToPlaylist -> AddToPlaylistDialog(dialog, actions, dismiss)
        is AppDialog.NewPlaylist -> TextInputDialog("New playlist", "Name",
                confirm = "Create", onDismiss = dismiss) {
            actions.createPlaylist(it, dialog.songs, dialog.onServer)
        }
        is AppDialog.RenamePlaylist -> TextInputDialog("Rename playlist", "Name", dialog.playlist.title, "Rename", dismiss) {
            actions.applyRename(dialog.playlist, it)
        }
        is AppDialog.DeletePlaylist -> ConfirmDialog("Delete playlist", "Delete ${dialog.playlist.title}?", "Delete", dismiss) {
            actions.applyDelete(dialog.playlist, dialog.goBack)
        }
        is AppDialog.RemoveDownloads -> ConfirmDialog("Remove downloads",
                if (dialog.songs.size == 1) "Remove ${dialog.songs[0].title} from this device? You can still stream it."
                else "Remove these ${dialog.songs.size} songs from this device? You can still stream them.",
                "Remove", dismiss) {
            actions.applyRemoveDownloads(dialog.songs)
        }
    }
}

@Composable
private fun AddToPlaylistDialog(dialog: AppDialog.AddToPlaylist, actions: AppActions, dismiss: () -> Unit) {
    val mediaRepository: MediaRepository = koinInject()
    val subsonic: SubsonicRepository = koinInject()
    var playlists by remember { mutableStateOf<List<Playlist>?>(null) }
    // The current music source's playlists (device playlists hold device songs, server playlists server songs).
    LaunchedEffect(dialog) {
        playlists = if (actions.isServer) {
            runCatching { subsonic.playlists() }.getOrDefault(emptyList()).filter { !it.readonly && it.id != dialog.excludePlaylistId }
        } else mediaRepository.loadPlaylistsExcept(dialog.excludePlaylistId ?: -1)
    }
    AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Add to playlist") },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item {
                        ListItem(
                                onClick = { dismiss(); actions.newPlaylist(dialog.songs) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = { Icon(Icons.Default.Add, contentDescription = null) }) { Text("New playlist") }
                    }
                    items(playlists.orEmpty(), key = { it.id }) { playlist ->
                        ListItem(
                                onClick = { dismiss(); actions.addSongsToPlaylist(playlist, dialog.songs) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    PlaylistArtwork(playlist, Modifier.size(40.dp)) { m ->
                                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, modifier = m.padding(8.dp))
                                    }
                                }) { Text(playlist.title) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
