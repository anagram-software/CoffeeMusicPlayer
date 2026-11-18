package com.udeshcoffee.android.ui.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.data.subsonic.SubsonicException
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.playback.PlayerConnection
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val bitRates = listOf(0, 320, 256, 192, 128, 96)
private val formats = listOf("raw" to "Original", "mp3" to "MP3", "opus" to "Opus")

/** Settings rows for the music server: sign in/out, stream quality, scrobbling. */
@Composable
fun ServerSettings() {
    val subsonic: SubsonicRepository = koinInject()
    val player: PlayerConnection = koinInject()
    val account by subsonic.account.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<String?>(null) }
    var bitRate by remember { mutableIntStateOf(subsonic.maxBitRate) }
    var format by remember { mutableStateOf(subsonic.streamFormat) }
    var scrobble by remember { mutableStateOf(subsonic.scrobbleEnabled) }

    val current = account
    if (current == null) {
        ListItem(modifier = Modifier.clickable { dialog = "signin" },
                leadingContent = { Icon(Icons.Default.CloudOff, contentDescription = null) },
                headlineContent = { Text("Connect to a music server") },
                supportingContent = { Text("Navidrome or another Subsonic-compatible server") })
    } else {
        ListItem(
                leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                headlineContent = { Text(current.name) },
                supportingContent = {
                    Text("${current.credentials.username} · ${current.serverType} ${current.serverVersion}".trim())
                },
                trailingContent = { TextButton(onClick = { dialog = "signout" }) { Text("Sign out") } })
        if (!current.isSecure) {
            ListItem(leadingContent = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    headlineContent = { Text("Not encrypted") },
                    supportingContent = { Text("This server uses HTTP, so your sign-in token and music travel unencrypted. Use HTTPS outside your home network.") })
        }
        ListItem(modifier = Modifier.clickable { dialog = "bitrate" },
                headlineContent = { Text("Streaming quality") },
                supportingContent = { Text(if (bitRate == 0) "No limit" else "Up to $bitRate kbps (when the server transcodes)") })
        ListItem(modifier = Modifier.clickable { dialog = "format" },
                headlineContent = { Text("Streaming format") },
                supportingContent = { Text(formats.firstOrNull { it.first == format }?.second ?: "Original") })
        ListItem(modifier = Modifier.clickable { scrobble = !scrobble; subsonic.scrobbleEnabled = scrobble },
                headlineContent = { Text("Report plays") },
                supportingContent = { Text("Play counts on the server (and Last.fm / ListenBrainz if set up there)") },
                trailingContent = { Switch(checked = scrobble, onCheckedChange = { scrobble = it; subsonic.scrobbleEnabled = it }) })
    }

    when (dialog) {
        "signin" -> SignInDialog(subsonic, onDismiss = { dialog = null })
        "signout" -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Sign out") },
                text = { Text("Server songs will be removed from the queue.") },
                confirmButton = {
                    TextButton(onClick = { player.removeServerSongs(); subsonic.signOut(); dialog = null }) { Text("Sign out") }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } })
        "bitrate" -> ChoiceDialog("Streaming quality", bitRates.map { it to if (it == 0) "No limit" else "$it kbps" }, bitRate,
                onDismiss = { dialog = null }) { bitRate = it; subsonic.maxBitRate = it }
        "format" -> ChoiceDialog("Streaming format", formats, format, onDismiss = { dialog = null }) { format = it; subsonic.streamFormat = it }
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onDismiss: () -> Unit, onSelect: (T) -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        ListItem(modifier = Modifier.clickable { onSelect(value); onDismiss() },
                                leadingContent = { RadioButton(selected = value == selected, onClick = null) },
                                headlineContent = { Text(label) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun SignInDialog(subsonic: SubsonicRepository, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val insecure = url.trim().startsWith("http://", ignoreCase = true)

    fun submit() {
        if (busy || url.isBlank() || username.isBlank()) return
        busy = true
        error = null
        scope.launch {
            try {
                subsonic.signIn(url, username, password)
                onDismiss()
            } catch (e: SubsonicException) {
                error = if (e.isAuthError) "Wrong username or password" else e.message
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
            onDismissRequest = { if (!busy) onDismiss() },
            title = { Text("Connect to a music server") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(url, { url = it }, label = { Text("Server address") }, placeholder = { Text("music.example.com") },
                            singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                            supportingText = if (insecure) ({ Text("HTTP isn't encrypted: your sign-in and music can be seen on the network.") }) else null)
                    OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, enabled = !busy,
                            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, enabled = !busy,
                            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Text("Your password isn't stored: only a salted token, as the Subsonic API defines.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = ::submit, enabled = !busy && url.isNotBlank() && username.isNotBlank()) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Connect")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } })
}
