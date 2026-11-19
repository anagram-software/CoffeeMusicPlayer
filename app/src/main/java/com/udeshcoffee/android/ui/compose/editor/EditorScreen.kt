package com.udeshcoffee.android.ui.compose.editor

import com.udeshcoffee.android.ui.compose.components.PageLoadingIndicator
import android.app.Application
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.compose.LocalAppActions
import com.udeshcoffee.android.ui.compose.components.ArtworkImage
import com.udeshcoffee.android.utils.AlbumArt
import com.udeshcoffee.android.utils.MediaAccess
import com.udeshcoffee.android.utils.TagChanges
import com.udeshcoffee.android.utils.TagWriter
import com.udeshcoffee.android.utils.loadBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import kotlin.coroutines.resume

data class EditorFields(
        val title: String = "", val artist: String = "", val album: String = "", val genre: String = "",
        val year: String = "", val track: String = "", val disc: String = ""
)

data class EditorState(
        val song: Song? = null,
        val path: String? = null,
        val fields: EditorFields = EditorFields(),
        /** New artwork: a picked content Uri or a downloaded URL; null keeps the current art. */
        val newArtwork: Any? = null,
        val busy: String? = null,
        val message: String? = null,
        val done: Boolean = false
)

class EditorScreenViewModel(
        private val songId: Long,
        private val context: Application,
        private val mediaRepository: MediaRepository,
        private val dataRepository: DataRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    private var original = EditorFields()

    init {
        viewModelScope.launch {
            val song = mediaRepository.loadSongs().firstOrNull { it.id == songId } ?: return@launch
            val genre = mediaRepository.getGenreOfSong(songId)?.name ?: ""
            original = EditorFields(song.title, song.artistName, song.albumName, genre,
                    song.year.takeIf { it > 0 }?.toString() ?: "", song.track.takeIf { it > 0 }?.toString() ?: "",
                    song.discNumber.takeIf { it > 0 }?.toString() ?: "")
            _state.value = EditorState(song = song, path = mediaRepository.getRealPath(MediaAccess.songUri(songId)), fields = original)
        }
    }

    val songUri: Uri get() = MediaAccess.songUri(songId)

    fun edit(transform: EditorFields.() -> EditorFields) = _state.update { it.copy(fields = it.fields.transform()) }
    fun pickArtwork(uri: Uri) = _state.update { it.copy(newArtwork = uri) }
    fun resetArtwork() = _state.update { it.copy(newArtwork = null) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    /** Fills fields and artwork from Last.fm (no year there). */
    fun lookup(title: String, artist: String) = viewModelScope.launch {
        _state.update { it.copy(busy = "Looking up…") }
        val metadata = dataRepository.searchTrackMetadata(title.trim(), artist.trim())
        _state.update { s ->
            if (metadata == null) s.copy(busy = null, message = "No match found")
            else s.copy(busy = null,
                    fields = s.fields.copy(title = metadata.title, artist = metadata.artist,
                            album = metadata.album ?: s.fields.album, genre = metadata.genre ?: s.fields.genre),
                    newArtwork = metadata.artworkUrl ?: s.newArtwork)
        }
    }

    /**
     * Writes changed tags: copy to cache, edit with jaudiotagger, write back through MediaStore.
     * On API 30+ the screen obtains write consent first.
     */
    fun save() = viewModelScope.launch {
        val s = _state.value
        val f = s.fields
        _state.update { it.copy(busy = "Saving…") }
        try {
            val artwork = s.newArtwork?.let { loadBitmap(context, it) }
            val changes = TagChanges(
                    title = f.title.takeIf { it != original.title },
                    artist = f.artist.takeIf { it != original.artist },
                    album = f.album.takeIf { it != original.album },
                    genre = f.genre.takeIf { it != original.genre },
                    year = f.year.takeIf { it != original.year && it.isNotBlank() },
                    track = f.track.takeIf { it != original.track && it.isNotBlank() },
                    disc = f.disc.takeIf { it != original.disc && it.isNotBlank() })
            withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val name = s.path?.let { File(it).name } ?: "$songId.mp3" // jaudiotagger needs the extension
                val temp = File(File(context.cacheDir, "tag-edit").apply { mkdirs() }, name)
                try {
                    resolver.openInputStream(songUri)!!.use { input -> temp.outputStream().use { input.copyTo(it) } }
                    TagWriter.write(temp, changes, artwork)
                    resolver.openOutputStream(songUri, "wt")!!.use { out -> temp.inputStream().use { it.copyTo(out) } }
                } finally {
                    temp.delete()
                }
            }
            s.path?.let { scan(it) }
            _state.update { it.copy(busy = null, message = "Song updated", done = true) }
        } catch (e: Exception) {
            android.util.Log.w("Editor", "Save failed", e)
            _state.update { it.copy(busy = null, message = "Couldn't edit the song") }
        }
    }

    private suspend fun scan(path: String) = suspendCancellableCoroutine { cont ->
        MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ -> if (cont.isActive) cont.resume(Unit) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(songId: Long) {
    val viewModel: EditorScreenViewModel = koinViewModel(key = "editor:$songId") { parametersOf(songId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val context = LocalContext.current
    var lookupDialog by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(viewModel::pickArtwork) }
    val writeConsent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        if (it.resultCode == android.app.Activity.RESULT_OK) viewModel.save()
    }
    LaunchedEffect(state.message) { state.message?.let { actions.message(it); viewModel.consumeMessage() } }
    LaunchedEffect(state.done) { if (state.done) actions.back() }

    Scaffold(topBar = {
        TopAppBar(
                title = { Text("Edit tags") },
                navigationIcon = { IconButton(onClick = { actions.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { lookupDialog = true }, enabled = state.song != null) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Look up on Last.fm")
                    }
                    TextButton(enabled = state.song != null && state.busy == null, onClick = {
                        val consent = MediaAccess.writeRequest(context.contentResolver, listOf(viewModel.songUri))
                        if (consent == null) viewModel.save() else writeConsent.launch(IntentSenderRequest.Builder(consent).build())
                    }) { Text("Save") }
                })
    }) { padding ->
        val song = state.song
        if (song == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PageLoadingIndicator() }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.busy?.let { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ArtworkImage(state.newArtwork ?: AlbumArt(song.albumId, song.dateModified), Icons.Default.MusicNote,
                        Modifier.size(140.dp), MaterialTheme.shapes.medium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("Choose image")
                    }
                    if (state.newArtwork != null) TextButton(onClick = viewModel::resetArtwork) { Text("Keep current") }
                }
            }
            val f = state.fields
            Field("Title", f.title, Modifier.fillMaxWidth()) { v -> viewModel.edit { copy(title = v) } }
            Field("Artist", f.artist, Modifier.fillMaxWidth()) { v -> viewModel.edit { copy(artist = v) } }
            Field("Album", f.album, Modifier.fillMaxWidth()) { v -> viewModel.edit { copy(album = v) } }
            Field("Genre", f.genre, Modifier.fillMaxWidth()) { v -> viewModel.edit { copy(genre = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Year", f.year, Modifier.weight(1f), number = true) { v -> viewModel.edit { copy(year = v) } }
                Field("Track", f.track, Modifier.weight(1f), number = true) { v -> viewModel.edit { copy(track = v) } }
                Field("Disc", f.disc, Modifier.weight(1f), number = true) { v -> viewModel.edit { copy(disc = v) } }
            }
            state.path?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    if (lookupDialog) {
        var title by remember { mutableStateOf(state.fields.title) }
        var artist by remember { mutableStateOf(state.fields.artist) }
        AlertDialog(
                onDismissRequest = { lookupDialog = false },
                title = { Text("Look up on Last.fm") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                        OutlinedTextField(artist, { artist = it }, label = { Text("Artist") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = { lookupDialog = false; viewModel.lookup(title, artist) }, enabled = title.isNotBlank()) { Text("Search") } },
                dismissButton = { TextButton(onClick = { lookupDialog = false }) { Text("Cancel") } })
    }
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier = Modifier, number: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = modifier,
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default)
}
