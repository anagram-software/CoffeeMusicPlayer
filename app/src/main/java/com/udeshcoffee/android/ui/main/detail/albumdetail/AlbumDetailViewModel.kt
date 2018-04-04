package com.udeshcoffee.android.ui.main.detail.albumdetail

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.queueSong
import com.udeshcoffee.android.extensions.shuffle
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.SortManager
import com.udeshcoffee.android.utils.ToastMessage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class AlbumDetailViewModel(
        application: Application,
        private val mediaRepository: MediaRepository,
        private val dataRepository: DataRepository
): AndroidViewModel(application) {
    private var disposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null

    private var albumId: Long = -1
    val currentSongId = MutableLiveData<Long>()
    val songs = MutableLiveData<List<Song>>()

    // Events
    val showFavoriteToast = ToastMessage()
    val showAddToPlaylistDialog = SingleLiveEvent<List<Song>>()
    val showSongLongDialog = SingleLiveEvent<Song>()

    fun start(albumId: Long) {
        this.albumId = albumId

        fetchData()

        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        val service = getService()
        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            currentSongId.value = service?.currentSong()?.id ?: -1
                        }
                    }
                }
        currentSongId.value = service?.currentSong()?.id ?: -1
    }

    fun stop() {
        dispose()

        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun dispose() {
        disposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    fun fetchData() {
        dispose()
        disposable = mediaRepository.getAlbumSongs(albumId)
                .map({ songs ->
                    SortManager.sortAlbumSongs(songs)

                    if (!sortAscending) {
                        Collections.reverse(songs)
                    }

                    return@map songs
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    songs.value = it
                }
    }

    fun playClick() {
        songs.value?.let { playSong(0, it, true) }
    }

    fun playNextClick() {
        songs.value?.let { queueSong(it, true) }
    }

    fun queueClick() {
        songs.value?.let { queueSong(it, false) }
    }

    fun addToFavoritesClick() {
        songs.value?.let {
            if (it.isNotEmpty()) {
                dataRepository.addToFavorites(it)
                showFavoriteToast.setMessage("Added to favorites")
            } else {
                showFavoriteToast.setMessage("Removed from favorites")
            }
        }
    }

    fun addToPlaylistClick() {
        songs.value?.let { showAddToPlaylistDialog.value = it }
    }

    fun itemClicked(position: Int) {
        songs.value?.let { playSong(position, it, true) }
    }

    fun itemLongClicked(position: Int) {
        songs.value?.let { showSongLongDialog.value = it[position] }
    }

    fun shuffleClicked() {
        songs.value?.let { shuffle(it) }
    }

    var sortOrder: Int
        get() = SortManager.albumSongsSortOrder
        set(value) {
            SortManager.albumSongsSortOrder = value}

    var sortAscending: Boolean
        get() = SortManager.albumSongsAscending
        set(value) {
            SortManager.albumSongsAscending = value}
}