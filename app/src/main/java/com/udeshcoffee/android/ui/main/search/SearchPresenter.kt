package com.udeshcoffee.android.ui.main.search

import android.content.IntentFilter
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
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
class SearchPresenter(
                      private val mediaRepository: MediaRepository
): SearchContract.Presenter, KoinComponent {

    val TAG = "SearchPresenter"

    override lateinit var view: SearchContract.View

    private var broadcastDisposable: Disposable? = null

    override fun start() {
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
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    override fun search(query: String) {
        if (query == "") {
            view.populateItems(ArrayList())
            view.populateAlbumItems(ArrayList())
            view.populateArtistItems(ArrayList())
            return
        }
        fetchSongs(query)
        fetchAlbums(query)
        fetchArtist(query)
    }

    private fun fetchSongs(query: String) {
        mediaRepository.getSongs()
                .map { songs ->
                    Stream.of(songs).filter { it.title.contains(query, true) }.collect(Collectors.toList())
                }
                .firstOrError()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    songs -> view.populateItems(songs)
                }, {})
        getService()?.currentSong()?.id?.let { view.setCurrentSong(it) }
    }

    private fun fetchAlbums(query: String) {
        mediaRepository.getAlbums()
                .map { albums ->
                    Stream.of(albums).filter{ it.title.contains(query, true) }.collect(Collectors.toList())
                }
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ view.populateAlbumItems(it) }
    }

    private fun fetchArtist(query: String) {
        mediaRepository.getArtists()
                .map { artists ->
                    Stream.of(artists).filter{ it.name.contains(query, true) }.collect(Collectors.toList())
                }
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ view.populateArtistItems(it) }
    }


    override fun playClick(songs: ArrayList<Song>) {}

    override fun queueClick(songs: ArrayList<Song>) {}

    override fun playNextClick(songs: java.util.ArrayList<Song>) {}

    override fun addToFavoritesClick(songs: java.util.ArrayList<Song>) {}

    override fun addToPlaylistClick(songs: java.util.ArrayList<Song>) {}

    override fun itemClicked(position: Int, allItems: List<Song>) {
        playSong(position, allItems, true)
    }

    override fun itemLongClicked(item: Song) {
        view.showSongLongDialog(item)
    }

    override fun albumItemClicked(position: Int) {
        view.showAlbum(position)
    }

    override fun albumItemLongClicked(album: Album) {
        mediaRepository.getAlbumSongs(album.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribe {
                    view.showCollectionLongDialog(album.title, it)
                }
    }

    override fun albumItemOptionClicked(album: Album) {
        mediaRepository.getAlbumSongs(album.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .firstOrError()
                .subscribe({
                    playSong(0, it, true)
                }, {})
    }

    override fun artistItemClicked(position: Int) {
        view.showArtist(position)
    }

    override fun artistItemLongClicked(artist: Artist) {
        mediaRepository.getArtistSongs(artist.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribe {
                    view.showCollectionLongDialog(artist.name, it)
                }
    }

    override fun artistItemOptionClicked(artist: Artist) {
        mediaRepository.getArtistSongs(artist.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .firstOrError()
                .subscribe({
                    playSong(0, it, true)
                }, {})
    }

}