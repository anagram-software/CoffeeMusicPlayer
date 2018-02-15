package com.udeshcoffee.android.ui.main.detail.artistdetail

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
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
import com.udeshcoffee.android.utils.deleteArtistArt
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.koin.standalone.KoinComponent
import java.util.*


/**
* Created by Udathari on 9/12/2017.
*/
class ArtistDetailPresenter(
        private val mediaRepository: MediaRepository,
        private val dataRepository: DataRepository
): ArtistDetailContract.Presenter, KoinComponent {

    val TAG = this.javaClass.simpleName

    override var artistId: Long = -1
    override lateinit var artistName: String
    override lateinit var view: ArtistDetailContract.View

    private var songsDisposable: Disposable? = null
    private var albumsDisposable: Disposable? = null
    private var broadcastDisposable: Disposable? = null

    override fun start() {
        fetchAlbums()
        fetchSongs()
        fetchBio()

        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when (it.action) {
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

    override fun result(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "result")
        if (requestCode == ArtistDetailFragment.SELECT_PHOTO) {
            Log.d(TAG, "select photo")
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "result ok")
                data?.let {
                    Log.d(TAG, "data: $it")
//                    saveArtistArt(contentResolver, artist.id, it.data)
                    view.artistArtChanged(artistId, artistName)
                }
            }
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
        songsDisposable = mediaRepository.getArtistSongs(artistId)
                .map({ songs ->
                    SortManager.sortArtistSongs(songs)

                    if (!sortAscending) {
                        Collections.reverse(songs)
                    }

                    return@map songs
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { songs ->
                    view.populateItems(songs)
                }
        getService()?.currentSong()?.id?.let { view.setCurrentSong(it) }
    }

    override fun fetchAlbums() {
        disposeAlbums()
        albumsDisposable = mediaRepository.getArtistAlbums(artistName)
                .map({ albums ->
                    SortManager.sortArtistAlbums(albums)

                    if (!albumSortAscending) {
                        Collections.reverse(albums)
                    }

                    return@map albums
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.populateAlbumItems(it) }
    }

    // Bio
    private fun fetchBio() {
        Log.d(TAG, "fetchBio")
        dataRepository.getBio(artistId, artistName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d(TAG, "emitBio $it")
                    it?.let { it1 ->
                        view.setBio(it1.first)
                        it1.second?.let { it2 -> view.setTags(it2) }
                    }
                }, {})
    }

    override fun selectImage() {
        view.showSelectImageUI()
    }

    override fun deleteImage() {
        deleteArtistArt(artistId)
        view.artistArtDeleted()
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
                .map({ songs ->
                    SortManager.sortAlbumSongs(songs)

                    if (!sortAscending) {
                        Collections.reverse(songs)
                    }

                    return@map songs
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .take(1)
                .subscribe {
                    view.showCollectionLongDialog(album.title, it)
                }
    }

    override fun albumItemOptionClicked(album: Album) {
        mediaRepository.getAlbumSongs(album.id)
                .map({ songs ->
                    SortManager.sortAlbumSongs(songs)

                    if (!sortAscending) {
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

    override var sortOrder: Int
        get() = SortManager.artistSongsSortOrder
        set(value) {
            SortManager.artistSongsSortOrder = value
        }

    override var sortAscending: Boolean
        get() = SortManager.artistSongsAscending
        set(value) {
            SortManager.artistSongsAscending = value
        }

    override var albumSortOrder: Int
        get() = SortManager.artistAlbumsSortOrder
        set(value) {
            SortManager.artistAlbumsSortOrder = value
        }

    override var albumSortAscending: Boolean
        get() = SortManager.artistAlbumsAscending
        set(value) {
            SortManager.artistAlbumsAscending = value
        }

}