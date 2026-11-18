package com.udeshcoffee.android.ui.main.editor

import com.udeshcoffee.android.utils.loadBitmap
import android.content.Context
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import android.content.ContentUris
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Created by Udathari on 9/28/2017.
 */
class EditorViewModel(
        application: Application,
        private val mediaRepository: MediaRepository,
        private val dataRepository: DataRepository
) : AndroidViewModel(application) {

    val song = MutableLiveData<Song>()
    val path = MutableLiveData<String>()
    var genre = MutableLiveData<String?>()
    val resetEnabled = MutableLiveData<Boolean>()

    val progressDialog = MutableLiveData<String?>()
    var albumArtUri: Uri? = null
        set(value) {
            field = value
            if (value != null) {
                albumArtUrl = null
            }
        }

    var albumArtUrl: String? = null
        set(value) {
            field = value
            if (value != null) {
                albumArtUri = null
            }
        }
    // Events
    val showToast = ToastMessage()
    val finish = SingleLiveEvent<Unit>()
    val reloadImage = SingleLiveEvent<Int>()

    private var file: File? = null

    private var searchJob: Job? = null

    /** The song as it was when editing started; tags are written only where they differ. */
    private var original: Song? = null
    private var originalGenre: String? = null

    fun start(song: Song) {
        this.song.value = song
        original = song

        fetchFile()
        fetchData()
        resetEnabled.value = false
    }

    fun stop() {
        cancelSearch()
    }

    fun cancelSearch() {
        searchJob?.cancel()
        progressDialog.value = null
    }

    /** MediaStore URI of the song being edited. */
    val songUri: Uri
        get() = MediaAccess.songUri(song.value!!.id)

    private fun fetchFile() {
        viewModelScope.launch {
            mediaRepository.getRealPath(songUri)?.let { path ->
                this@EditorViewModel.path.value = path
                file = File(path)
            }
        }
    }

    private fun fetchData() {
        val songId = song.value!!.id
        viewModelScope.launch {
            val name = mediaRepository.getGenreOfSong(songId)?.name
            originalGenre = name
            genre.value = name
        }
        reloadImage.value = ImageType.SONG
    }

    fun imageSelected(uri: Uri) {
        Log.d(TAG, "imageSelected")
        albumArtUri = uri
        reloadImage.value = ImageType.URI
    }

    fun actionReset() {
        albumArtUri = null
        albumArtUrl = null
        reloadImage.value = ImageType.SONG
    }

    /** Looks the song up on Last.fm and fills in title, artist, album, genre and artwork (Last.fm has no year). */
    fun search(title: String, artist: String) {
        progressDialog.value = "Collecting Information"
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val metadata = dataRepository.searchTrackMetadata(title.trim(), artist.trim())
            progressDialog.value = null
            if (metadata == null) {
                showToast.value = "No tracks found"
                return@launch
            }
            song.value = song.value?.copy(
                    title = metadata.title,
                    artistName = metadata.artist,
                    albumName = metadata.album ?: song.value!!.albumName)
            metadata.genre?.let { genre.value = it }
            metadata.artworkUrl?.let {
                albumArtUrl = it
                reloadImage.value = ImageType.URL
            }
        }
    }

    /**
     * Writes the tags. The file is copied to the cache, edited there with jaudiotagger (which
     * needs a real file), then written back through MediaStore. On API 30+ the caller must
     * first obtain write consent ([MediaAccess.writeRequest]).
     */
    fun save(title: String, album: String, artist: String, genre: String, year: String, trackNo: String, discNo: String) {
        progressDialog.value = "Saving"
        val uri = songUri
        val context = App.instance
        // jaudiotagger picks the format from the file extension.
        val fileName = file?.name ?: "${song.value!!.id}.mp3"
        val path = path.value
        viewModelScope.launch {
            try {
                val artwork = when {
                    albumArtUri != null -> loadBitmap(context, albumArtUri)
                    albumArtUrl != null -> loadBitmap(context, albumArtUrl)
                    else -> null
                }
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val tempFile = File(File(context.cacheDir, "tag-edit").apply { mkdirs() }, fileName)
                    try {
                        resolver.openInputStream(uri)!!.use { input -> tempFile.outputStream().use { input.copyTo(it) } }
                        setTags(tempFile, title, album, artist, genre, year, trackNo, discNo, artwork)
                        resolver.openOutputStream(uri, "wt")!!.use { output -> tempFile.inputStream().use { it.copyTo(output) } }
                    } finally {
                        tempFile.delete()
                    }
                }
                // Refresh MediaStore so the library shows the new tags.
                if (path != null) scanFile(context, path)
                progressDialog.value = null
                showToast.value = "Song changed"
                finish.call()
            } catch (e: Exception) {
                Log.w(TAG, "save failed", e)
                progressDialog.value = null
                showToast.value = "Couldn't edit the song"
            }
        }
    }

    private suspend fun scanFile(context: Context, path: String) = suspendCancellableCoroutine { cont ->
        MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ -> if (cont.isActive) cont.resume(Unit) }
    }

    private fun setTags(file: File, title: String, album: String, artist: String, genre: String,
                        year: String, trackNo: String, discNo: String, artworkBitmap: Bitmap?) {
        Log.d(TAG, "setTags")
        val audioFile = AudioFileIO.read(file)

        if (audioFile != null) {
            val tag = audioFile.tag

            if (title != original!!.title)
                tag.setField(FieldKey.TITLE, title)
            if (album != original!!.albumName)
                tag.setField(FieldKey.ALBUM, album)
            if (artist != original!!.artistName)
                tag.setField(FieldKey.ARTIST, artist)
            if (year != original!!.year.toString())
                tag.setField(FieldKey.YEAR, year)
            if (genre != originalGenre)
                tag.setField(FieldKey.GENRE, genre)
            if (trackNo != "")
                tag.setField(FieldKey.TRACK, trackNo)

            if (discNo != "")
                tag.setField(FieldKey.DISC_NO, discNo)

            Log.d(TAG, "try to save art")

            artworkBitmap?.let {
                val scaledBitmap = Bitmap.createScaledBitmap(it, 500, 500, false)

                val stream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

                val byteArray = stream.toByteArray()

                val artworkFile = File(App.instance.cacheDir, "artwork.jpg")

                if (!artworkFile.exists())
                    artworkFile.createNewFile()

                val out = FileOutputStream(artworkFile)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)

                val artwork = ArtworkFactory.createArtworkFromFile(artworkFile)

                artwork.binaryData = byteArray

                if (tag.firstArtwork != null) {
                    Log.d(TAG, "firstArtwork not null")
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                } else {
                    Log.d(TAG, "firstArtwork null")
                    tag.addField(artwork)
                }

                artworkFile.delete()
            }

            Log.d(TAG, "commiting")
            audioFile.commit()
        }
    }

    companion object {
        private const val TAG = "EditorPresenter"
    }

    object ImageType {
        const val SONG = 0
        const val URL = 1
        const val URI = 2
    }
}