package com.udeshcoffee.android.ui.main.detail.genredetail

import android.content.IntentFilter
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.queueSong
import com.udeshcoffee.android.extensions.shuffle
import com.udeshcoffee.android.model.Album
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
class GenreDetailPresenter(
        val mediaRepository: MediaRepository,
        val dataRepository: DataRepository
): GenreDetailContract.Presenter, KoinComponent {

    val TAG = this.javaClass.simpleName

    override var genreId: Long = -1
    override lateinit var view: GenreDetailContract.View

    private var songsDisposable: Disposable? = null
    private var albumsDisposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null

    override fun start() {
        fetchSongs()
        fetchAlbums()

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
        disposeSongs()
        disposeAlbums()
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun disposeSongs() {
        songsDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    private fun disposeAlbums() {
        albumsDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    override fun fetchSongs() {
        disposeSongs()
        songsDisposable = mediaRepository.getGenreSongs(genreId)
                .observeOn(AndroidSchedulers.mainThread())
                .map({ songs ->
                    SortManager.sortGenreSongs(songs)

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

    override fun fetchAlbums() {
        disposeAlbums()
        albumsDisposable = mediaRepository.getGenreAlbums(genreId)
                .observeOn(AndroidSchedulers.mainThread())
                .map({ albums ->
                    SortManager.sortGenreAlbums(albums)

                    if (!albumSortAscending) {
                        Collections.reverse(albums)
                    }

                    return@map albums
                })
                .subscribe{view.populateAlbumItems(it)}
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

    override var sortOrder: Int
        get() = SortManager.genreSongsSortOrder
        set(value) {SortManager.genreSongsSortOrder = value}

    override var sortAscending: Boolean
        get() = SortManager.genreSongsAscending
        set(value) {SortManager.genreSongsAscending = value}

    override var albumSortOrder: Int
        get() = SortManager.genreAlbumsSortOrder
        set(value) {SortManager.genreAlbumsSortOrder = value}

    override var albumSortAscending: Boolean
        get() = SortManager.genreAlbumsAscending
        set(value) {SortManager.genreAlbumsAscending = value}

}