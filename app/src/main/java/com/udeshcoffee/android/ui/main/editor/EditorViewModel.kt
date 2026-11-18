package com.udeshcoffee.android.ui.main.editor

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
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.datatype.Artwork
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

    private var collectionDisposable: Disposable? = null
    private var saveDisposable: Disposable? = null

    fun start(song: Song) {
        this.song.value = song

        fetchFile()
        fetchData()
        resetEnabled.value = false
    }

    fun stop() {
        disposeCollectionDisposable()
        disposeSaveDisposable()
    }

    /** MediaStore URI of the song being edited. */
    val songUri: Uri
        get() = MediaAccess.songUri(song.value!!.id)

    private fun fetchFile() {
        val trackUri = songUri
        val path = mediaRepository.getRealPath(trackUri)
        if (path != null) {
            this.path.value = path
            file = File(path)
        }
    }

    private fun fetchData() {
        genre.value = mediaRepository.getGenreOfSong(song.value!!.id)?.name
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

    fun search(title: String, artist: String) {
        val searchableTitle = title.trim()
        val searchableArtist = artist.trim()
        progressDialog.value = "Collecting Information"
        disposeCollectionDisposable()
        collectionDisposable = dataRepository.searchItunes(searchableTitle, searchableArtist)
                .subscribe({ songs ->
                    if (songs.isNotEmpty()) {
                        Log.d("Search", "Fuck yeah")
                        songs[0].attributes?.let { results ->
                            val releaseYear = results.releaseDate.split("-")[0]
                            var tempSong = song.value
                            tempSong = results.name.let { tempSong?.copy(title = it) }
                            tempSong = results.albumName.let { tempSong?.copy(albumName = it) }
                            tempSong = results.artistName.let { tempSong?.copy(artistName = it) }
                            tempSong = releaseYear.let { tempSong?.copy(year = it.toInt()) }
                            genre.value = results.genreNames?.getOrElse(0) {
                                ""
                            }
                            song.value = tempSong
                            val image = results.artwork.url
                            image.let {
                                albumArtUrl = it
                                reloadImage.value = ImageType.URL
                            }
                        }
                    } else {
                        Log.d("Search", "Fuck empty")
                        showToast.value = "No tracks found"
                    }
                    progressDialog.value = null
                }, {
                    Log.d("Search", "Fuck ${it.message}")
                    showToast.value = "No tracks found"
                    progressDialog.value = null
                })
    }

    /**
     * Writes the tags. The file is copied to the cache, edited there with jaudiotagger (which
     * needs a real file), then written back through MediaStore. On API 30+ the caller must
     * first obtain write consent ([MediaAccess.writeRequest]).
     */
    fun save(title: String, album: String, artist: String, genre: String, year: String, trackNo: String, discNo: String) {
        Log.d(TAG, "save")
        progressDialog.value = "Saving"
        val uri = songUri
        val context = App.instance
        // jaudiotagger picks the format from the file extension.
        val fileName = file?.name ?: "${song.value!!.id}.mp3"
        saveDisposable = Completable.fromAction {
            val resolver = context.contentResolver
            val tempFile = File(File(context.cacheDir, "tag-edit").apply { mkdirs() }, fileName)
            try {
                resolver.openInputStream(uri)!!.use { input -> tempFile.outputStream().use { input.copyTo(it) } }
                setTags(tempFile, title, album, artist, genre, year, trackNo, discNo)
                resolver.openOutputStream(uri, "wt")!!.use { output -> tempFile.inputStream().use { it.copyTo(output) } }
            } finally {
                tempFile.delete()
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val onDone = {
                        progressDialog.value = null
                        showToast.value = "Song changed"
                        finish.call()
                    }
                    val path = path.value
                    if (path == null) {
                        onDone()
                    } else {
                        // Refresh MediaStore so the library shows the new tags.
                        MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ ->
                            AndroidSchedulers.mainThread().scheduleDirect { onDone() }
                        }
                    }
                }, {
                    Log.w(TAG, "save failed", it)
                    progressDialog.value = null
                    showToast.value = "Couldn't edit the song"
                })
    }

    fun disposeCollectionDisposable() {
        collectionDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun disposeSaveDisposable() {
        saveDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun setTags(file: File, title: String, album: String, artist: String, genre: String,
                        year: String, trackNo: String, discNo: String) {
        Log.d(TAG, "setTags")
        val audioFile = AudioFileIO.read(file)

        if (audioFile != null) {
            val tag = audioFile.tag

            if (title != song.value!!.title)
                tag.setField(FieldKey.TITLE, title)
            if (album != song.value!!.albumName)
                tag.setField(FieldKey.ALBUM, album)
            if (artist != song.value!!.artistName)
                tag.setField(FieldKey.ARTIST, artist)
            if (year != song.value!!.year.toString())
                tag.setField(FieldKey.YEAR, year)
            if (genre != this.genre.value)
                tag.setField(FieldKey.GENRE, genre)
            if (trackNo != "")
                tag.setField(FieldKey.TRACK, trackNo)

            if (discNo != "")
                tag.setField(FieldKey.DISC_NO, discNo)

            Log.d(TAG, "try to save art")
            val artworkBitmap = when {
                albumArtUri != null -> loadAlbumArtworkFromUriSynchronous(albumArtUri)
                albumArtUrl != null -> loadAlbumArtworkFromUriSynchronous(albumArtUrl)
                else -> null
            }

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

                val artwork = Artwork.createArtworkFromFile(artworkFile)

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