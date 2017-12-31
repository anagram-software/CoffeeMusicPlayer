package com.udeshcoffee.android.ui.main.home

import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playSong
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 12/17/2017.
 */
class HomePresenter(val view: HomeContract.View, private val mediaRepository: MediaRepository): HomeContract.Presenter {

    private var compositeDisposable: CompositeDisposable? = null

    init {
        view.presenter = this
    }

    override fun start() {
        compositeDisposable = CompositeDisposable()
        loadLibraryStats()
        loadTopSongs()
        loadTopAlbums()
        loadTopArtists()
        loadRecentlyAdded()
    }

    override fun stop() {
        compositeDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
        compositeDisposable = null
    }

    private fun loadLibraryStats() {
        compositeDisposable?.add(mediaRepository.getSongCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.setLibraryStats(songs = it) })
        compositeDisposable?.add(mediaRepository.getAlbumCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.setLibraryStats(albums = it) })
        compositeDisposable?.add(mediaRepository.getArtistCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.setLibraryStats(artists = it) })
        compositeDisposable?.add(mediaRepository.getPlaylistCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.setLibraryStats(playlists = it) })
    }

    private fun loadTopSongs() {
        compositeDisposable?.add(mediaRepository.getTopSongs()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view.setTopSongs(it)
                })
    }

    private fun loadTopAlbums() {
        compositeDisposable?.add(mediaRepository.getTopAlbums()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.setTopAlbums(it) })
    }

    private fun loadTopArtists() {
        compositeDisposable?.add(mediaRepository.getTopArtists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.setTopArtists(it) })
    }

    private fun loadRecentlyAdded() {
        compositeDisposable?.add(mediaRepository.getRecentlyAdded(5)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.setRecentlyAdded(it) })
    }

    override fun itemClicked(position: Int, allItems: List<Song>) {
        playSong(position, allItems, true)
    }

    override fun itemLongClicked(item: Song) {
        view.showSongLongDialog(item)
    }

    override fun albumItemClicked(position: Int) {
        view.showAlbum(position)
    }

    override fun albumItemLongClicked(item: Album) {
        mediaRepository.getAlbumSongs(item.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribe {
                    view.showCollectionLongDialog(item.title, it)
                }
    }

    override fun artistItemClicked(position: Int) {
        view.showArtist(position)
    }

    override fun artistItemLongClicked(item: Artist) {
        mediaRepository.getArtistSongs(item.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribe {
                    view.showCollectionLongDialog(item.name, it)
                }
    }
}