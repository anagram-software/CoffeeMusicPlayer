package com.udeshcoffee.android.ui.main.detail.playlistdetail

import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.queueSong
import com.udeshcoffee.android.model.Playlist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.service.MusicService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.standalone.KoinComponent
import java.util.*

/**
* Created by Udathari on 9/12/2017.
*/

class PlaylistDetailPresenter(
                              private val mediaRepository: MediaRepository,
                              private val dataRepository: DataRepository
): PlaylistDetailContract.Presenter, KoinComponent {

    val TAG = this.javaClass.simpleName

    override var playlistId: Long = -1
    override var playlistType: Int = -1
    override lateinit var playlistTitle: String
    override lateinit var view: PlaylistDetailContract.View

    private var disposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null

    override fun start() {
        view.setPlaylist(playlistType == Playlist.USER)

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
        disposable = mediaRepository.getPlaylistSongs(playlistId, playlistType)
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
        view.showAddToPlaylistDialog(songs, playlistId)
    }

    override fun rename() {
        view.showRenameUI(playlistId, playlistTitle)
    }

    override fun delete() {
        view.showDeleteUI(playlistId, playlistTitle)
    }

    override fun itemClicked(position: Int, allItems: List<Song>) {
        playSong(position, allItems, true)
    }

    override fun itemLongClicked(item: Song) {
        view.showSongLongDialog(item)
    }

    override fun itemMoved(fromPosition: Int, toPosition: Int) {
        mediaRepository.movePlaylistItem(playlistId, fromPosition, toPosition)
    }

    override fun itemRemoved(id: Long, position: Int) {
        mediaRepository.removePlayistItem(playlistId, id)
    }

    override fun addToPlaylist() {
        view.showAddToPlaylistUI(playlistId, playlistTitle)
    }
}