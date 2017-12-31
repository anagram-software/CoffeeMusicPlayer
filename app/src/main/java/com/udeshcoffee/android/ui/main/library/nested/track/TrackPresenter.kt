package com.udeshcoffee.android.ui.main.library.nested.track

import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.getService
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playSong
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.shuffle
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*

/**
 * Created by Udathari on 9/5/2017.
 */
class TrackPresenter(val view: TrackContract.View, private val mediaRepository: MediaRepository): TrackContract.Presenter{

    val TAG = "TrackPresenter"

    private var disposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null

    init {
        view.presenter = this
    }

    override fun start() {
        fetchData()

        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            val id = getService()?.currentSong()?.id ?: -1
                            view.setCurrentSong(id)
                        }
                    }
                }
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
        disposable = mediaRepository.getSongs()
                .observeOn(AndroidSchedulers.mainThread())
                .map({ songs ->
                    SortManager.sortSongs(songs)

                    if (!sortAscending) {
                        Collections.reverse(songs)
                    }

                    return@map songs
                })
                .subscribe{
                    songs -> view.populateItems(songs)
                }
        getService()?.currentSong()?.id?.let { view.setCurrentSong(it) }
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
        get() = SortManager.songsSortOrder
        set(value) {SortManager.songsSortOrder = value}

    override var sortAscending: Boolean
        get() = SortManager.songsAscending
        set(value) {SortManager.songsAscending = value}
}