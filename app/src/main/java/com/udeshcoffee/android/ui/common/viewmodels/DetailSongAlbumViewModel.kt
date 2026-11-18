package com.udeshcoffee.android.ui.common.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.lifecycle.MutableLiveData
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.SortManager

abstract class DetailSongAlbumViewModel (
        application: Application,
        dataRepository: DataRepository,
        protected val mediaRepository: MediaRepository
): DetailSongViewModel(application, dataRepository) {

    val albums = MutableLiveData<List<Album>>()

    // Events
    val showAlbum = SingleLiveEvent<Album>()
    val showCollectionLongDialog = SingleLiveEvent<Pair<String, List<Song>>>()

    override fun start() {
        super.start()
        fetchAlbums()
    }

    override fun stop() {
        super.stop()
        disposeAlbums()
    }

    abstract fun fetchAlbums()

    private var albumsJob: Job? = null

    open fun disposeAlbums() {
        albumsJob?.cancel()
    }

    /** Collects [flow] into [albums], sorted with [sort] and reversed unless ascending, until stopped. */
    protected fun collectAlbums(flow: Flow<List<Album>>, sort: (List<Album>) -> Unit, ascending: () -> Boolean) {
        albumsJob?.cancel()
        albumsJob = viewModelScope.launch {
            flow.collect { albums.value = it.sortedWith(sort, ascending()) }
        }
    }

    private suspend fun sortedAlbumSongs(album: Album): List<Song> =
            mediaRepository.loadAlbumSongs(album.id).sortedWith(SortManager::sortAlbumSongs, SortManager.albumSongsAscending)

    fun albumItemClicked(position: Int) {
        albums.value?.get(position)?.let { showAlbum.value = it }
    }

    fun albumItemLongClicked(position: Int) {
        val album = albums.value?.getOrNull(position) ?: return
        viewModelScope.launch { showCollectionLongDialog.value = album.title to sortedAlbumSongs(album) }
    }

    fun albumItemOptionClicked(position: Int) {
        val album = albums.value?.getOrNull(position) ?: return
        viewModelScope.launch { playSong(0, sortedAlbumSongs(album), true) }
    }

    abstract var albumSortOrder: Int

    abstract var albumSortAscending: Boolean

}