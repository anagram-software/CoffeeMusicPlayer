package com.udeshcoffee.android.ui.main.favorites

import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
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
* Created by Udathari on 9/29/2017.
*/
class FavoritesPresenter(
        private val mediaRepository: MediaRepository
): FavoritesContract.Presenter, KoinComponent {

    override lateinit var view: FavoritesContract.View

    var disposable: Disposable? = null
    var broadcastDisposable: Disposable? = null

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


        disposable = mediaRepository.getFavorites()
                .map({ songs ->
                    com.udeshcoffee.android.utils.SortManager.sortFavoritesSongs(songs)

                    if (!sortAscending) {
                        java.util.Collections.reverse(songs)
                    }

                    return@map songs
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{view.populateItems(it)}
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

    override fun addToFavoritesClick(songs: ArrayList<Song>) {}

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
        get() = SortManager.favoritesSongsSortOrder
        set(value) {
            SortManager.favoritesSongsSortOrder = value}

    override var sortAscending: Boolean
        get() = SortManager.favoritesSongsAscending
        set(value) {
            SortManager.favoritesSongsAscending = value}
}