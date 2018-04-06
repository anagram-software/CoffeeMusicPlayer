package com.udeshcoffee.android.ui.common.viewmodels

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

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

    abstract fun disposeAlbums()

    fun albumItemClicked(position: Int) {
        albums.value?.get(position)?.let { showAlbum.value = it }
    }

    fun albumItemLongClicked(position: Int) {
        albums.value?.get(position)?.let {album ->
            mediaRepository.getAlbumSongs(album.id)
                    .map({ songs ->
                        SortManager.sortAlbumSongs(songs)

                        if (!SortManager.albumSongsAscending) {
                            Collections.reverse(songs)
                        }

                        return@map songs
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .firstOrError()
                    .subscribe({
                        showCollectionLongDialog.value = album.title to it
                    }, {})
        }
    }

    fun albumItemOptionClicked(position: Int) {
        albums.value?.get(position)?.let { album ->
            mediaRepository.getAlbumSongs(album.id)
                    .map({ songs ->
                        SortManager.sortAlbumSongs(songs)

                        if (!SortManager.albumSongsAscending) {
                            Collections.reverse(songs)
                        }

                        return@map songs
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .firstOrError()
                    .subscribe({
                        playSong(0, it, true)
                    }, {})
        }
    }

    abstract var albumSortOrder: Int

    abstract var albumSortAscending: Boolean

}