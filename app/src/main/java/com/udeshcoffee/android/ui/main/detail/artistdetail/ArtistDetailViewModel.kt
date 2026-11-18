package com.udeshcoffee.android.ui.main.detail.artistdetail

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import android.app.Activity.RESULT_OK
import android.app.Application
import androidx.lifecycle.MutableLiveData
import android.content.Intent
import android.util.Log
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongAlbumViewModel
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.call
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.deleteArtistArt
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
    val artistArtDeleted = SingleLiveEvent<Unit>()


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
        collectSongs(mediaRepository.observeArtistSongs(artistId), SortManager::sortArtistSongs) { songSortAscending }
    }

    override fun fetchAlbums() {
        collectAlbums(mediaRepository.observeArtistAlbums(artistName), SortManager::sortArtistAlbums) { albumSortAscending }
    }

    // Bio
    private fun fetchBio() {
        viewModelScope.launch {
            dataRepository.getBio(artistId, artistName)?.let {
                bio.value = it.first
                tags.value = it.second
            }
        }
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