package com.udeshcoffee.android.ui.compose.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udeshcoffee.android.data.lastfm.LastFmAccount
import com.udeshcoffee.android.data.lastfm.LastFmConnectState
import com.udeshcoffee.android.data.lastfm.LastFmScrobbler
import com.udeshcoffee.android.data.source.MusicSourceStore
import androidx.compose.material3.Switch
import com.udeshcoffee.android.ui.compose.LocalAppActions
import org.koin.compose.koinInject

/**
 * Connect or disconnect a Last.fm account (independent of the music source). Signing in happens
 * in the browser; Last.fm sends it back to the app, which finishes in [LastFmAccount.handleCallback].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LastFmSettings() {
    val account: LastFmAccount = koinInject()
    val context = LocalContext.current
    val actions = LocalAppActions.current
    val user by account.user.collectAsStateWithLifecycle()
    val state by account.connectState.collectAsStateWithLifecycle()
    var confirmDisconnect by remember { mutableStateOf(false) }
    val scrobbler: LastFmScrobbler = koinInject()
    var scrobble by remember { mutableStateOf(scrobbler.enabled) }
    val isServer = koinInject<MusicSourceStore>().isServer

    fun connect() {
        val uri = account.startSignIn()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            account.cancelSignIn()
            actions.message("No browser found to sign in to Last.fm")
        }
    }

    val current = user
    when {
        !account.isAvailable -> ListItem(enabled = false, supportingContent = { Text("Not available in this build") }) { Text("Connect Last.fm") }
        current != null -> {
            ListItem(
                    supportingContent = { Text("Connected as ${current.name}") },
                    trailingContent = { TextButton(onClick = { confirmDisconnect = true }) { Text("Disconnect") } }) { Text("Last.fm") }
            ListItem(
                    onClick = { scrobble = !scrobble; scrobbler.enabled = scrobble },
                    supportingContent = {
                        Text(if (isServer) "Send the songs you listen to to Last.fm. Turn this off if your music server already scrobbles to Last.fm, or plays will count twice."
                        else "Send the songs you listen to to Last.fm")
                    },
                    trailingContent = { Switch(checked = scrobble, onCheckedChange = { scrobble = it; scrobbler.enabled = it }) }) { Text("Scrobble") }
        }
        state == LastFmConnectState.Connecting -> ListItem(
                supportingContent = { Text("Finishing sign-in…") },
                trailingContent = { LoadingIndicator(Modifier.size(32.dp)) }) { Text("Connecting to Last.fm") }
        state == LastFmConnectState.WaitingForBrowser -> ListItem(
                onClick = ::connect,
                supportingContent = { Text("Allow access on last.fm in your browser, and you'll come back here. Tap to open it again.") },
                trailingContent = { TextButton(onClick = { account.cancelSignIn() }) { Text("Cancel") } }) { Text("Connecting to Last.fm") }
        else -> ListItem(
                onClick = { account.clearError(); connect() },
                supportingContent = {
                    val failed = state as? LastFmConnectState.Failed
                    if (failed != null) Text(failed.message, color = MaterialTheme.colorScheme.error)
                    else Text("Sign in on last.fm in your browser")
                }) { Text("Connect Last.fm") }
    }

    if (confirmDisconnect && current != null) AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect Last.fm?") },
            text = { Text("Coffee will forget ${current.name}'s sign-in. To revoke its access completely, remove it under Settings › Applications on last.fm.") },
            confirmButton = { TextButton(onClick = { confirmDisconnect = false; account.disconnect() }) { Text("Disconnect") } },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") } })
}
