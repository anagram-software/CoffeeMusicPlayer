package com.udeshcoffee.android.ui.main.detail.albumdetail

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
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.standalone.KoinComponent
import java.util.*

/**
* Created by Udathari on 9/12/2017.
*/
class AlbumDetailPresenter(
        private val mediaRepository: MediaRepository,
        private val dataRepository: DataRepository
): AlbumDetailContract.Presenter, KoinComponent {

    private var disposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null

    override var albumId: Long = -1
    override lateinit var view: AlbumDetailContract.View

    override fun start() {
        fetchData()

        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        val service = getService()
        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            val id = service?.currentSong()?.id ?: -1
                            view.setCurrentSong(id)
                        }
                    }
                }
        service?.currentSong()?.id?.let { view.setCurrentSong(it) }
    }

    override fun stop() {
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

    override fun fetchData() {
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
                    songs -> view.populateItems(songs)
                }
    }

    override fun playClick(songs: ArrayList<Song>) {
        playSong(0, songs, true)
    }

    override fun playNextClick(songs: ArrayList<Song>) {
        queueSong(songs, true)
    }

    override fun queueClick(songs: ArrayList<Song>) {
        queueSong(songs, false)
    }

    override fun addToFavoritesClick(songs: ArrayList<Song>) {
        if (songs.size > 0) {
            dataRepository.addToFavorites(songs)
            view.showFavoritesToast(true)
        } else {
            view.showFavoritesToast(false)
        }
    }

    override fun addToPlaylistClick(songs: ArrayList<Song>) {
        view.showAddToPlaylistDialog(songs)
    }

    override fun itemClicked(position: Int, allItems: List<Song>) {
        playSong(position, allItems, true)
    }

    override fun itemLongClicked(item: Song) {
        view.showSongLongDialog(item)
    }

    override fun shuffleClicked(allItems: List<Song>) {
        shuffle(allItems)
    }

    override var sortOrder: Int
        get() = SortManager.albumSongsSortOrder
        set(value) {SortManager.albumSongsSortOrder = value}

    override var sortAscending: Boolean
        get() = SortManager.albumSongsAscending
        set(value) {SortManager.albumSongsAscending = value}

}