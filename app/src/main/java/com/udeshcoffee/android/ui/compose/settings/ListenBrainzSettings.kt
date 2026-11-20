package com.udeshcoffee.android.ui.compose.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.data.metabrainz.ListenBrainzAccount
import com.udeshcoffee.android.data.metabrainz.ListenBrainzScrobbler
import com.udeshcoffee.android.data.metabrainz.MetaBrainzException
import com.udeshcoffee.android.data.source.MusicSourceStore
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val TOKEN_PAGE = "https://listenbrainz.org/settings/"

/**
 * Connect or disconnect a ListenBrainz account (independent of the music source) by pasting the
 * user token from listenbrainz.org/settings, and turn submitting listens on or off.
 */
@Composable
fun ListenBrainzSettings() {
    val account: ListenBrainzAccount = koinInject()
    val scrobbler: ListenBrainzScrobbler = koinInject()
    val isServer = koinInject<MusicSourceStore>().isServer
    val user by account.user.collectAsStateWithLifecycle()
    var scrobble by remember { mutableStateOf(scrobbler.enabled) }
    var confirmDisconnect by remember { mutableStateOf(false) }

    val current = user
    if (current == null) {
        TokenForm(account)
    } else {
        ListItem(
                supportingContent = { Text("Connected as ${current.name}") },
                trailingContent = { TextButton(onClick = { confirmDisconnect = true }) { Text("Disconnect") } }) { Text("ListenBrainz") }
        ListItem(
                onClick = { scrobble = !scrobble; scrobbler.enabled = scrobble },
                supportingContent = {
                    Text(if (isServer) "Send the songs you listen to to ListenBrainz. Your music server may already do this; if so, leave this off or plays will count twice."
                    else "Send the songs you listen to to ListenBrainz")
                },
                trailingContent = { Switch(checked = scrobble, onCheckedChange = { scrobble = it; scrobbler.enabled = it }) }) { Text("Submit listens") }
    }

    if (confirmDisconnect && current != null) AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect ListenBrainz?") },
            text = { Text("Coffee will forget ${current.name}'s token, and any listens not sent yet. To revoke the token, reset it on listenbrainz.org/settings.") },
            confirmButton = { TextButton(onClick = { confirmDisconnect = false; account.disconnect() }) { Text("Disconnect") } },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") } })
}

/** Paste the user token, checked with ListenBrainz before it's saved. */
@Composable
private fun TokenForm(account: ListenBrainzAccount) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Not saveable: the token shouldn't end up in saved instance state.
    val token = remember { TextFieldState() }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (busy || token.text.isBlank()) return
        busy = true
        error = null
        scope.launch {
            try {
                account.connect(token.text.toString())
                token.clearText()
            } catch (e: MetaBrainzException) {
                error = if (e.isAuthError) "ListenBrainz didn't accept this token" else e.message
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Connect ListenBrainz", style = MaterialTheme.typography.titleMedium)
        Text("Paste your user token from your ListenBrainz settings page. It's used to show your stats on Home and to submit what you listen to.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedSecureTextField(token, label = { Text("User token") }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                onKeyboardAction = { submit() })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, TOKEN_PAGE.toUri())) } },
                    shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) { Text("Get token") }
            Button(onClick = ::submit, enabled = !busy && token.text.isNotBlank(), shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Connect")
            }
        }
    }
}
