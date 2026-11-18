package com.udeshcoffee.android.ui.main.detail.genredetail

import android.app.Application
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongAlbumViewModel
import com.udeshcoffee.android.utils.SortManager
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


    override fun fetchSongs() {
        collectSongs(mediaRepository.observeGenreSongs(genreId), SortManager::sortGenreSongs) { songSortAscending }
    }

    override fun fetchAlbums() {
        collectAlbums(mediaRepository.observeGenreAlbums(genreId), SortManager::sortGenreAlbums) { albumSortAscending }
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