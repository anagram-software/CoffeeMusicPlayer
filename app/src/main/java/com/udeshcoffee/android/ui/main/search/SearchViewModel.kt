package com.udeshcoffee.android.ui.main.search

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.ui.common.viewmodels.DetailSongAlbumViewModel
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.SortManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Udathari on 9/12/2017.
 */
class SearchViewModel(
        application: Application,
        dataRepository: DataRepository,
        mediaRepository: MediaRepository
): DetailSongAlbumViewModel(application, dataRepository, mediaRepository) {

    val artists = MutableLiveData<List<Artist>>()

    val showArtist = SingleLiveEvent<Artist>()

    // Redundant
    override fun fetchSongs() {}
    override fun disposeSongs() {}
    override fun fetchAlbums() {}
    override fun disposeAlbums() {}

    fun search(query: String) {
        if (query == "") {
            songs.value = ArrayList()
            albums.value = ArrayList()
            artists.value = ArrayList()
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
                    songs.value = it
                }, {})
    }

    private fun fetchAlbums(query: String) {
        mediaRepository.getAlbums()
                .map { albums ->
                    Stream.of(albums).filter{ it.title.contains(query, true) }.collect(Collectors.toList())
                }
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    albums.value = it
                }
    }

    private fun fetchArtist(query: String) {
        mediaRepository.getArtists()
                .map { artists ->
                    Stream.of(artists).filter{ it.name.contains(query, true) }.collect(Collectors.toList())
                }
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    artists.value = it
                }
    }


    fun artistItemClicked(position: Int) {
        artists.value?.get(position)?.let { showArtist.value = it }
    }

    fun artistItemLongClicked(position: Int) {
        artists.value?.get(position)?.let {artist ->
            mediaRepository.getAlbumSongs(artist.id)
                    .map({ songs ->
                        SortManager.sortArtistSongs(songs)

                        if (!SortManager.artistSongsAscending) {
                            Collections.reverse(songs)
                        }

                        return@map songs
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .firstOrError()
                    .subscribe({
                        showCollectionLongDialog.value = artist.name to it
                    }, {})
        }
    }

    fun artistItemOptionClicked(position: Int) {
        artists.value?.get(position)?.let { artist ->
            mediaRepository.getAlbumSongs(artist.id)
                    .map({ songs ->
                        SortManager.sortArtistSongs(songs)

                        if (!SortManager.artistSongsAscending) {
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
    }

    // Redundant
    override var albumSortOrder: Int = 0
    override var albumSortAscending: Boolean = false
    override var songSortOrder: Int = 0
    override var songSortAscending: Boolean = false

    companion object {
        val TAG = "SearchPresenter"
    }

}