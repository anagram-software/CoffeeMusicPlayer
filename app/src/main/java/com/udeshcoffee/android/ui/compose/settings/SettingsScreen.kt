package com.udeshcoffee.android.ui.compose.settings

import androidx.compose.runtime.mutableIntStateOf
import android.content.SharedPreferences
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import com.udeshcoffee.android.ui.compose.components.PageBottomPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.TopLevelScaffold
import com.udeshcoffee.android.utils.PreferenceUtil
import com.udeshcoffee.android.data.source.MusicSourceStore
import org.koin.compose.koinInject

/** Settings, stored in the default SharedPreferences (keys in [PreferenceUtil]). */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var collect by rememberPref(prefs, PreferenceUtil.PREF_COLLECT_CONTENT, true)
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
    val isServer = koinInject<MusicSourceStore>().isServer
    val appActions = LocalAppActions.current

    TopLevelScaffold("Settings", onBack = { appActions.back() }, showSettings = false) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Header("Music source")
            MusicSourceSettings()

            Header("Playback")
            SwitchPref(prefs, PreferenceUtil.PREF_CALL, false, "Stay paused after interruptions",
                    "Don't resume after a call or another app's sound")
            SwitchPref(prefs, PreferenceUtil.PREF_FADE, true, "Fade on play/pause", "Smoothly fade audio in and out")
            SwitchPref(prefs, PreferenceUtil.PREF_SHOW_QUALITY, PreferenceUtil.DEFAULT_SHOW_QUALITY, "Show track quality",
                    "Format, bit depth or bit rate, and sample rate below the artwork in the player")

            Header("Lyrics")
            SwitchPref(prefs, PreferenceUtil.PREF_LRCLIB, PreferenceUtil.DEFAULT_LRCLIB, "Find missing lyrics online",
                    "When ${if (isServer) "your server has no lyrics for a song" else "playing a song"}, look them up on lrclib.net. " +
                            "This sends the song's title, artist and album to that service.")

            // Device library only: server artists use the server's images and bios.
            if (!isServer) {
                Header("Online content")
                ListItem(
                        onClick = { collect = !collect },
                        supportingContent = { Text("Artist images and bios from Last.fm") },
                        trailingContent = { Switch(checked = collect, onCheckedChange = { collect = it }) }) { Text("Download artist info") }
                SwitchPref(prefs, PreferenceUtil.PREF_WIFI_ONLY, true, "Wi-Fi only", "Download artist images only on Wi-Fi", enabled = collect)
            }

            Header("About")
            ListItem(onClick = { dialog = "whatsnew" }) { Text("What's new") }
            ListItem(onClick = { dialog = "licences" }) { Text("Open source licences") }
            ListItem(supportingContent = { Text(version) }) { Text("Version") }
            Spacer(Modifier.height(PageBottomPadding))
        }
    }

    when (dialog) {
        "whatsnew" -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("What's new") },
                text = {
                    Text("• New Material You design that follows your album art\n" +
                            "• Works with Android Auto and Assistant (\"play … on Coffee Player\")\n" +
                            "• Album art and lyrics work again on recent Android\n" +
                            "• Tag lookup with Last.fm\n" +
                            "• Playlists stored in the app, with drag to reorder\n" +
                            "• Faster, more reliable playback (Media3)")
                },
                confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } })
        "licences" -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Open source licences") },
                text = {
                    AndroidView(factory = { WebView(it).apply { loadUrl("file:///android_asset/licences.html") } },
                            modifier = Modifier.height(420.dp))
                },
                confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } })
    }
}

@Composable
private fun Header(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp))
}

@Composable
private fun rememberPref(prefs: SharedPreferences, key: String, default: Boolean) =
        remember { mutableStateOf(prefs.getBoolean(key, default)) }.also { state ->
            // Persist on change.
            androidx.compose.runtime.LaunchedEffect(state.value) { prefs.edit { putBoolean(key, state.value) } }
        }

@Composable
private fun SwitchPref(prefs: SharedPreferences, key: String, default: Boolean, title: String, summary: String, enabled: Boolean = true) {
    var value by rememberPref(prefs, key, default)
    ListItem(
            onClick = { value = !value },
            enabled = enabled,
            supportingContent = { Text(summary) },
            trailingContent = { Switch(checked = value, onCheckedChange = { value = it }, enabled = enabled) }) { Text(title) }
}
