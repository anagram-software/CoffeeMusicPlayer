package com.udeshcoffee.android.ui.main.detail.genredetail

import android.app.Application
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongAlbumViewModel
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*

/**
* Created by Udathari on 9/12/2017.
*/
class GenreDetailViewModel(
        application: Application,
        dataRepository: DataRepository,
        mediaRepository: MediaRepository
): DetailSongAlbumViewModel(application, dataRepository, mediaRepository) {

    var genreId: Long = -1

    private var songsDisposable: Disposable? = null
    private var albumsDisposable: Disposable? = null

    override fun fetchSongs() {
        disposeSongs()
        songsDisposable = mediaRepository.getGenreSongs(genreId)
                .observeOn(AndroidSchedulers.mainThread())
                .map({ songs ->
                    SortManager.sortGenreSongs(songs)

                    if (!songSortAscending) {
                        Collections.reverse(songs)
                    }

                    return@map songs
                })
                .subscribe{
                    songs.value = it
                }
    }

    override fun fetchAlbums() {
        disposeAlbums()
        albumsDisposable = mediaRepository.getGenreAlbums(genreId)
                .observeOn(AndroidSchedulers.mainThread())
                .map({ albums ->
                    SortManager.sortGenreAlbums(albums)

                    if (!albumSortAscending) {
                        Collections.reverse(albums)
                    }

                    return@map albums
                })
                .subscribe{
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

    override var songSortOrder: Int
        get() = SortManager.genreSongsSortOrder
        set(value) {
            SortManager.genreSongsSortOrder = value}

    override var songSortAscending: Boolean
        get() = SortManager.genreSongsAscending
        set(value) {
            SortManager.genreSongsAscending = value}

    override var albumSortOrder: Int
        get() = SortManager.genreAlbumsSortOrder
        set(value) {
            SortManager.genreAlbumsSortOrder = value}

    override var albumSortAscending: Boolean
        get() = SortManager.genreAlbumsAscending
        set(value) {
            SortManager.genreAlbumsAscending = value}

    companion object {
        const val TAG = "GenreDetailViewModel"
    }
}