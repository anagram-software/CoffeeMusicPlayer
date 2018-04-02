package com.udeshcoffee.android.ui.main.editor

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
import com.udeshcoffee.android.model.Genre
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
import org.koin.standalone.KoinComponent
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
* Created by Udathari on 9/28/2017.
*/
class EditorPresenter(
        private val mediaRepository: MediaRepository,
                      private val dataRepository: DataRepository
): EditorContract.Presenter, KoinComponent {

    val TAG = "EditorPresenter"

    override lateinit var song: Song
    override lateinit var view: EditorContract.View

    var file: File? = null
    var genre: Genre? = null
    private var albumArtUri: Uri? = null
        set(value) {
            if (value != null)
                albumArtUrl = null
            view.enableDisableReset(value != null)
            field = value
        }
    private var albumArtUrl: String? = null
        set(value) {
            if (value != null)
                albumArtUri = null
            view.enableDisableReset(value != null)
            field = value
        }


    private var collectionDisposable: Disposable? = null
    private var saveDisposable: Disposable? = null

    override fun start() {
        fetchFile()
        fetchData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            file?.let {
                if (!hasPermission() && isFromSdCard(it.path))
                    view.showPermissionDialog()
            }
        }
        view.enableDisableReset(false)
    }

    override fun stop() {
        disposeCollectionDisposable()
        disposeSaveDisposable()
    }

    private fun fetchFile() {
        val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        val path = mediaRepository.getRealPath(trackUri)
        if (path != null) {
            view.setPath(path)
            file = File(path)
        }
    }

    private fun fetchData(){
        genre = mediaRepository.getGenreOfSong(song.id)
        view.setAlbumArt(song)
        view.setData(song.title, song.albumName, song.artistName, genre?.name, song.year.toString(), song.track, song.discNumber)
    }

    override fun imageSelected(uri: Uri) {
        Log.d(TAG, "imageSelected")
        albumArtUri = uri
        view.setAlbumArt(uri)
    }

    override fun actionSearch() {
        view.showSearchDialog(song.id, song.title, song.artistName)
    }

    override fun actionSelectImage() {
        view.showSelectImageUI()
    }

    override fun actionReset() {
        albumArtUri = null
        view.setAlbumArt(song)
    }

    override fun search(title: String, artist: String) {
        val searchableTitle = title.trim()
        val searchableArtist = artist.trim()
        view.showHideProgressDialog(true, "Collecting Information")
        disposeCollectionDisposable()
        collectionDisposable = dataRepository.searchItunes(searchableTitle, searchableArtist)
                .subscribe({
                    if (it.resultCount > 0 && it.results != null) {
                        it.results!![0].let {
                            val releaseYear = it.releaseDate?.split("-")?.get(0)
                            view.setData(it.trackName, it.collectionName, it.artistName, it.primaryGenreName, releaseYear)
                            val image = it.artworkUrl100?.replace("100x100", "500x500")
                            image?.let {
                                albumArtUrl = it
                                view.setAlbumArt(it)
                            }
                        }
                    } else {
                        view.showToast("No tracks found")
                    }
                    view.showHideProgressDialog(false)
                },{
                    view.showToast("No tracks found")
                    view.showHideProgressDialog(false)
                })
    }

    override fun save(title: String, album: String, artist: String, genre: String, year: String, trackNo: String, discNo: String) {
        Log.d(TAG, "save")
        view.showHideProgressDialog(true, "Saving")
        saveDisposable = Observable.fromCallable({
            Log.d(TAG, "saveObservable")
            var tempFile: File

            file?.let {
                Log.d(TAG, "saveObservable file")
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
                                view.showHideProgressDialog(false)
                                Observable.fromCallable { view.showToast("Song changed") }
                                        .subscribeOn(AndroidSchedulers.mainThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .take(1)
                                        .subscribe()
                                view.finish()
                            }
                        })
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribeWith(object : DisposableObserver<Unit>() {
                    override fun onError(e: Throwable) {
                        view.showHideProgressDialog(false)
                        view.finish()
                        view.showToast("Couldn't edit the song")
                    }

                    override fun onComplete() {}

                    override fun onNext(t: Unit) {}

                })
    }

    override fun disposeCollectionDisposable() {
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
        Log.d(TAG, "setTags")
        val audioFile = AudioFileIO.read(file)

        if (audioFile != null) {
            val tag = audioFile.tag

            if (title != song.title)
                tag.setField(FieldKey.TITLE, title)
            if (album != song.albumName)
                tag.setField(FieldKey.ALBUM, album)
            if (artist != song.artistName)
                tag.setField(FieldKey.ARTIST, artist)
            if (year != song.year.toString())
                tag.setField(FieldKey.YEAR, year)
            if (genre != this.genre?.name)
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
                    Log.d(TAG, "firstArtwork not null")
                    tag.deleteArtworkField()
                    tag.setField(artwork)
                } else {
                    Log.d(TAG, "firstArtwork null")
                    tag.addField(artwork)
                }

                try {
                    val uri = ContentUris.withAppendedId(ArtworkURI, song.id)
                    val path = mediaRepository.getRealPath(uri)
                    File(path).delete()
                    artworkFile.delete()
                } catch (e: Exception) {

                }
            }

            Log.d(TAG, "commiting")
            audioFile.commit()
        }
    }
}