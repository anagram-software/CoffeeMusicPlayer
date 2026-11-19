package com.udeshcoffee.android.ui.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.data.source.MusicSourceStore
import com.udeshcoffee.android.data.subsonic.SubsonicException
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.ui.MusicSourceSwitcher
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val bitRates = listOf(0, 320, 256, 192, 128, 96)
private val formats = listOf("raw" to "Original", "mp3" to "MP3", "opus" to "Opus")

/**
 * Settings rows for the music source. Until one is set up: the music server sign-in form (the
 * default source), with the option to use this device's music instead. Afterwards: the current
 * source with a "Change" action (confirmed, since it clears the queue and restarts the UI; see
 * [MusicSourceSwitcher]), plus streaming settings for a server.
 */
@Composable
fun MusicSourceSettings() {
    val subsonic: SubsonicRepository = koinInject()
    val sourceStore: MusicSourceStore = koinInject()
    val switcher: MusicSourceSwitcher = koinInject()
    val account by subsonic.account.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<String?>(null) }
    var bitRate by remember { mutableIntStateOf(subsonic.maxBitRate) }
    var format by remember { mutableStateOf(subsonic.streamFormat) }
    var scrobble by remember { mutableStateOf(subsonic.scrobbleEnabled) }

    val current = account
    when {
        // Not set up yet: connect to a server, or use the device.
        !sourceStore.isConfigured -> {
            ServerSignInForm(subsonic, onSignedIn = switcher::useServer)
            HorizontalDivider(Modifier.padding(top = 8.dp))
            ListItem(modifier = Modifier.clickable { switcher.useDevice() },
                    leadingContent = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    headlineContent = { Text("Use music on this device only") },
                    supportingContent = { Text("Play the songs stored on this phone instead of a server") })
        }
        !sourceStore.isServer -> ListItem(
                leadingContent = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                headlineContent = { Text("Music on this device") },
                supportingContent = { Text("Songs stored on this phone") },
                trailingContent = { TextButton(onClick = { dialog = "change" }) { Text("Change") } })
        current != null -> {
            ListItem(
                    leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                    headlineContent = { Text("Music server: ${current.name}") },
                    supportingContent = {
                        Text("${current.credentials.username} · ${current.serverType} ${current.serverVersion}".trim())
                    },
                    trailingContent = { TextButton(onClick = { dialog = "change" }) { Text("Change") } })
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
    }

    when (dialog) {
        "change" -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Change music source?") },
                text = {
                    Text(buildString {
                        if (current != null && sourceStore.isServer) append("You'll be signed out of ${current.name}. ")
                        append("The queue will be cleared, and you can then connect to a music server or use the music on this device.")
                        if (sourceStore.isServer) append(" Server playlists and favorites stay on the server.")
                        else append(" Your playlists and favorites on this device are kept.")
                    })
                },
                confirmButton = { TextButton(onClick = { dialog = null; switcher.change() }) { Text("Change") } },
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
                        ListItem(modifier = Modifier.clickable { onDismiss(); onSelect(value) },
                                leadingContent = { RadioButton(selected = value == selected, onClick = null) },
                                headlineContent = { Text(label) })
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** The music server sign-in form, shown inline in Settings until a source is set up. */
@Composable
private fun ServerSignInForm(subsonic: SubsonicRepository, onSignedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    // Not saveable: the password shouldn't end up in saved instance state.
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val insecure = url.trim().startsWith("http://", ignoreCase = true)
    val canSubmit = !busy && url.isNotBlank() && username.isNotBlank()

    fun submit() {
        if (!canSubmit) return
        busy = true
        error = null
        scope.launch {
            try {
                subsonic.signIn(url, username, password)
                onSignedIn()
            } catch (e: SubsonicException) {
                error = if (e.isAuthError) "Wrong username or password" else e.message
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Connect to your music server", style = MaterialTheme.typography.titleMedium)
        Text("Navidrome or another Subsonic-compatible server. Your library, playlists and favorites come from the server.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(url, { url = it }, label = { Text("Server address") }, placeholder = { Text("music.example.com") },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                supportingText = if (insecure) ({ Text("HTTP isn't encrypted: your sign-in and music can be seen on the network.") }) else null)
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, enabled = !busy,
                modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, enabled = !busy,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Text("Your password isn't stored: only a salted token, as the Subsonic API defines.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = ::submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Connect")
        }
    }
}
