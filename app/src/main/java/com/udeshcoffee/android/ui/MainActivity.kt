package com.udeshcoffee.android.ui

import android.app.Activity.RESULT_OK
import android.app.SearchManager
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.playbackEvents
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playback.PlaybackEvents
import com.udeshcoffee.android.playback.PlayerConnection
import com.udeshcoffee.android.ui.compose.CoffeeApp
import com.udeshcoffee.android.utils.MediaAccess
import com.udeshcoffee.android.utils.Permissions
import com.udeshcoffee.android.utils.PreferenceUtil
import com.udeshcoffee.android.utils.ProcessLifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * The single activity. Hosts the Compose UI ([CoffeeApp]) and handles what needs an activity:
 * runtime permissions, the playback connection, launch intents and file deletion consent.
 */
class MainActivity : ComponentActivity() {

    private val playerConnection: PlayerConnection by inject()
    private val mediaRepository: MediaRepository by inject()
    private var isPlayerConnected = false
    private var connectionJob: Job? = null

    // Launch actions that wait for the player connection.
    private var pendingUri: Uri? = null
    private var pendingSearch: String? = null
    private var restoreQueueOnConnection = false

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (Permissions.hasReadAudio(this)) {
            onAudioPermissionGranted()
        } else {
            Toast.makeText(this, R.string.permission_audio_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val deleteLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) Toast.makeText(this, "Song deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC

        setContent { CoffeeApp(onDeleteSongs = ::deleteSongs) }

        handleIntent(intent)
        if (Permissions.hasReadAudio(this)) onAudioPermissionGranted()
        else permissionLauncher.launch(Permissions.startupPermissions())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionJob?.cancel()
        if (isPlayerConnected) playerConnection.disconnect()
    }

    /** Deletes songs from the device, asking for confirmation in a way that fits the API level. */
    private fun deleteSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        val consent = MediaAccess.deleteRequest(contentResolver, songs.map { MediaAccess.songUri(it.id) })
        if (consent != null) {
            deleteLauncher.launch(IntentSenderRequest.Builder(consent).build())
        } else {
            // API < 30: legacy confirmation dialog and file deletion.
            val song = songs.first()
            android.app.AlertDialog.Builder(this)
                    .setTitle("Delete")
                    .setMessage("Permanently delete ${song.title} from storage?")
                    .setPositiveButton("Delete") { _, _ ->
                        val deleted = MediaAccess.deleteLegacy(this, song.id)
                        Toast.makeText(this, if (deleted) "Song deleted" else "Couldn't delete the song", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
        }
    }

    /** Handles launch intents on the main thread; waits for the player connection when needed. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val player = getPlayerConnected()
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                if (player == null) pendingUri = intent.data else intent.data?.let { player.playUri(it) }
            }
            MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH -> {
                val query = intent.getStringExtra(SearchManager.QUERY) ?: ""
                if (player == null) pendingSearch = query else player.playFromSearch(query)
            }
            Intent.ACTION_MAIN -> {
                if (player == null) restoreQueueOnConnection = true else player.restoreQueueIfEmpty()
            }
        }
        setIntent(Intent())
    }

    private fun getPlayerConnected(): PlayerConnection? = playerConnection.takeIf { it.isConnected }

    private fun onAudioPermissionGranted() {
        connectPlayer()
        importMediaStorePlaylistsOnce()
    }

    private fun connectPlayer() {
        if (isPlayerConnected) return
        connectionJob = lifecycleScope.launch {
            playbackEvents().filter { it == PlaybackEvents.SERVICE_CONNECTED }.collect { onPlayerConnected() }
        }
        playerConnection.connect()
        isPlayerConnected = true
    }

    private fun onPlayerConnected() {
        pendingSearch?.let { playerConnection.playFromSearch(it); pendingSearch = null }
        pendingUri?.let { playerConnection.playUri(it); pendingUri = null }
        if (restoreQueueOnConnection) {
            playerConnection.restoreQueueIfEmpty()
            restoreQueueOnConnection = false
        }
    }

    /** Playlists moved from MediaStore to the app database in DB version 7; copy existing ones once. */
    private fun importMediaStorePlaylistsOnce() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean(PreferenceUtil.PLAYLISTS_IMPORTED, false)) return
        // Application-wide scope: the import should finish even if this activity goes away.
        ProcessLifecycleScope.launch {
            try {
                val count = mediaRepository.importMediaStorePlaylists()
                prefs.edit { putBoolean(PreferenceUtil.PLAYLISTS_IMPORTED, true) }
                if (count > 0)
                    Toast.makeText(applicationContext, resources.getQuantityString(R.plurals.playlists_imported, count, count), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.w(TAG, "Playlist import failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
