package com.udeshcoffee.android.ui.main.detail.playlistdetail

import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.getService
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playSong
import com.udeshcoffee.android.queueSong
import com.udeshcoffee.android.service.MusicService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * Created by Udathari on 9/12/2017.
 */
class PlaylistDetailPresenter(val playlist: Playlist,
                              val view: PlaylistDetailContract.View,
                              private val mediaRepository: MediaRepository,
                              private val dataRepository: DataRepository): PlaylistDetailContract.Presenter {

    val TAG = "PlaylistDetailPresenter"

    private var disposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null

    init {
        view.presenter = this
    }

    override fun start() {
        view.setPlaylist(playlist.type == Playlist.USER)

        fetchSongs()

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

    private fun fetchSongs() {
        dispose()
        disposable = mediaRepository.getPlaylistSongs(playlist.id, playlist.type)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe{
                    view.populateItems(it)
                    getService()?.currentSong()?.id?.let { view.setCurrentSong(it) }
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
        view.showAddToPlaylistDialog(songs, playlist.id)
    }

    override fun rename() {
        view.showRenameUI(playlist.title, playlist.id)
    }

    override fun delete() {
        view.showDeleteUI(playlist)
    }

    override fun itemClicked(position: Int, allItems: List<Song>) {
        playSong(position, allItems, true)
    }

    override fun itemLongClicked(item: Song) {
        view.showSongLongDialog(item)
    }

    override fun itemMoved(fromPosition: Int, toPosition: Int) {
        mediaRepository.movePlaylistItem(playlist.id, fromPosition, toPosition)
    }

    override fun itemRemoved(id: Long, position: Int) {
        mediaRepository.removePlayistItem(playlist.id, id)
    }

    override fun addToPlaylist() {
        view.showAddToPlaylistUI(playlist.id, playlist.title)
    }
}