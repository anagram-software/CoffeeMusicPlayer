package com.udeshcoffee.android.ui.main.editor

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
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
): AndroidViewModel(application) {

    val song = MutableLiveData<Song>()
    val path = MutableLiveData<String>()
    var genre = MutableLiveData<String>()
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
    val finish = SingleLiveEvent<Void>()
    val reloadImage = SingleLiveEvent<Int>()
    val showPermissionDialog = SingleLiveEvent<Void>()

    private var file: File? = null

    private var collectionDisposable: Disposable? = null
    private var saveDisposable: Disposable? = null

    fun start(song: Song) {
        this.song.value = song

        fetchFile()
        fetchData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            file?.let {
                if (!hasPermission() && isFromSdCard(it.path))
                    showPermissionDialog.call()
            }
        }
        resetEnabled.value = false
    }

    fun stop() {
        disposeCollectionDisposable()
        disposeSaveDisposable()
    }

    private fun fetchFile() {
        val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.value!!.id)
        val path = mediaRepository.getRealPath(trackUri)
        if (path != null) {
            this.path.value = path
            file = File(path)
        }
    }

    private fun fetchData(){
        genre.value = mediaRepository.getGenreOfSong(song.value!!.id)?.name
        reloadImage.value = ImageType.SONG
    }

    fun imageSelected(uri: Uri) {
        Log.d(Companion.TAG, "imageSelected")
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
                .subscribe({
                    if (it.resultCount > 0 && it.results != null) {
                        it.results!![0].let {
                            val releaseYear = it.releaseDate?.split("-")?.get(0)
                            var tempSong = song.value
                            tempSong = it.trackName?.let { tempSong?.copy(title = it)}
                            tempSong = it.collectionName?.let { tempSong?.copy(albumName = it)}
                            tempSong = it.artistName?.let { tempSong?.copy(artistName = it)}
                            tempSong = releaseYear?.let { tempSong?.copy(year = it.toInt())}
                            genre.value = it.primaryGenreName
                            song.value = tempSong
                            val image = it.artworkUrl100?.replace("100x100", "500x500")
                            image?.let {
                                albumArtUrl = it
                                reloadImage.value = ImageType.URL
                            }
                        }
                    } else {
                        showToast.value = "No tracks found"
                    }
                    progressDialog.value = null
                },{
                    showToast.value = "No tracks found"
                    progressDialog.value = null
                })
    }

    fun save(title: String, album: String, artist: String, genre: String, year: String, trackNo: String, discNo: String) {
        Log.d(Companion.TAG, "save")
        progressDialog.value = "Saving"
        saveDisposable = Observable.fromCallable({
            Log.d(Companion.TAG, "saveObservable")
            var tempFile: File

            file?.let {
                Log.d(Companion.TAG, "saveObservable file")
                if (isFromSdCard(it.path)) {
                    tempFile = File(App.instance.externalCacheDir.path, it.name)
                    copyFile(it, tempFile)
                    setTags(tempFile, title, album, artist, genre, year, trackNo, discNo)
                    cutFile(tempFile, it)
                } else {
                    file?.let { setTags(it, title, album, artist, genre, year, trackNo, discNo) }
                }
                MediaScannerConnection.scanFile(App.instance, arrayOf(it.absolutePath), null,
                        object : MediaScannerConnection.MediaScannerConnectionClient {
                            override fun onMediaScannerConnected() {}

                            override fun onScanCompleted(path: String, uri: Uri) {
                                progressDialog.value = null
                                Observable.fromCallable { showToast.value = "Song changed" }
                                        .subscribeOn(AndroidSchedulers.mainThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .take(1)
                                        .subscribe()
                                finish.call()
                            }
                        })
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribeWith(object : DisposableObserver<Unit>() {
                    override fun onError(e: Throwable) {
                        progressDialog.value = null
                        finish.call()
                        showToast.value = "Couldn't edit the song"
                    }

                    override fun onComplete() {}

                    override fun onNext(t: Unit) {}

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
                        year: String, trackNo: String, discNo: String){
        Log.d(Companion.TAG, "setTags")
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

            Log.d(Companion.TAG, "try to save art")
            val artworkBitmap = when {
                albumArtUri != null -> loadAlbumArtworkFromUriSynchronous(albumArtUri)
                albumArtUrl != null -> loadAlbumArtworkFromUriSynchronous(albumArtUrl)
                else -> null
            }

            artworkBitmap?.let {
                val scaledBitmap = Bitmap.createScaledBitmap(it, 500, 500, false)

                val stream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream as OutputStream?)

                val byteArray = stream.toByteArray()

                val artworkFile = File("${Environment.getExternalStorageDirectory()}/artwork.jpg")

                if (!artworkFile.exists())
                    artworkFile.createNewFile()

                val out = FileOutputStream(artworkFile)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)

                val artwork = Artwork.createArtworkFromFile(artworkFile)

                artwork.binaryData = byteArray

                if (tag.firstArtwork != null) {
                    Log.d(Companion.TAG, "firstArtwork not null")
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                } else {
                    Log.d(Companion.TAG, "firstArtwork null")
                    tag.addField(artwork)
                }

                try {
                    val uri = ContentUris.withAppendedId(ArtworkURI, song.value!!.id)
                    val path = mediaRepository.getRealPath(uri)
                    File(path).delete()
                    artworkFile.delete()
                } catch (e: Exception) {

                }
            }

            Log.d(Companion.TAG, "commiting")
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