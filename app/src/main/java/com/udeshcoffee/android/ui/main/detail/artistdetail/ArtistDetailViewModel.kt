package com.udeshcoffee.android.ui.main.detail.artistdetail

import android.app.Activity.RESULT_OK
import android.app.Application
import androidx.lifecycle.MutableLiveData
import android.content.Intent
import android.util.Log
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongAlbumViewModel
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.deleteArtistArt
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*


/**
 * Created by Udathari on 9/12/2017.
 */
class ArtistDetailViewModel(
        application: Application,
        dataRepository: DataRepository,
        mediaRepository: MediaRepository
): DetailSongAlbumViewModel(application, dataRepository, mediaRepository) {

    val TAG = this.javaClass.simpleName

    var artistId: Long = -1
    var artistName: String = ""

    val bio = MutableLiveData<String>()
    val tags = MutableLiveData<Array<String>>()

    // Events
    val artistArtChanged = SingleLiveEvent<Pair<Long, String>>()
    val artistArtDeleted = SingleLiveEvent<Void>()

    private var songsDisposable: Disposable? = null
    private var albumsDisposable: Disposable? = null

    override fun start() {
        super.start()
        fetchBio()
    }

    fun result(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "result")
        if (requestCode == ArtistDetailFragment.SELECT_PHOTO) {
            Log.d(TAG, "select photo")
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "result ok")
                data?.let {
                    Log.d(TAG, "data: $it")
//                    saveArtistArt(contentResolver, artist.id, it.data) - con
                    artistArtChanged.value = artistId to artistName
                }
            }
        }
    }

    override fun fetchSongs() {
        disposeSongs()
        songsDisposable = mediaRepository.getArtistSongs(artistId)
                .map({ songs ->
                    SortManager.sortArtistSongs(songs)

                    if (!songSortAscending) {
                        Collections.reverse(songs)
                    }

                    return@map songs
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    songs.value = it
                }
    }

    override fun fetchAlbums() {
        disposeAlbums()
        albumsDisposable = mediaRepository.getArtistAlbums(artistName)
                .map({ albums ->
                    SortManager.sortArtistAlbums(albums)

                    if (!albumSortAscending) {
                        Collections.reverse(albums)
                    }

                    return@map albums
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    albums.value = it
                }
    }

    override fun disposeSongs() {
        songsDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    override fun disposeAlbums() {
        albumsDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    // Bio
    private fun fetchBio() {
        Log.d(TAG, "fetchBio")
        dataRepository.getBio(artistId, artistName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d(TAG, "emitBio $it")
                    it?.let {
                        bio.value = it.first
                        tags.value = it.second
                    }
                }, {})
    }

    fun deleteImage() {
        deleteArtistArt(artistId)
        artistArtDeleted.call()
    }

    override var songSortOrder: Int
        get() = SortManager.artistSongsSortOrder
        set(value) {
            SortManager.artistSongsSortOrder = value}

    override var songSortAscending: Boolean
        get() = SortManager.artistSongsAscending
        set(value) {
            SortManager.artistSongsAscending = value}

    override var albumSortOrder: Int
        get() = SortManager.artistAlbumsSortOrder
        set(value) {
            SortManager.artistAlbumsSortOrder = value}

    override var albumSortAscending: Boolean
        get() = SortManager.artistAlbumsAscending
        set(value) {
            SortManager.artistAlbumsAscending = value}
}