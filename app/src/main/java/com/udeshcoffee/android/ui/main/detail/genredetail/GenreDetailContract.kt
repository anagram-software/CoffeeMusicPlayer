package com.udeshcoffee.android.ui.detail.albumdetail

import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.main.ShufflableSongContainingPresenter
import com.udeshcoffee.android.ui.main.SongContainingView

/**
 * Created by Udathari on 9/12/2017.
 */
interface GenreDetailContract {

    interface View : SongContainingView<Presenter> {

        fun populateAlbumItems(items: List<Album>)

        fun showAlbum(position: Int)

        fun showCollectionLongDialog(title: String, songs: List<Song>)

    }

    interface Presenter : ShufflableSongContainingPresenter {

        fun fetchSongs()

        fun fetchAlbums()

        fun albumItemClicked(position: Int)

        fun albumItemLongClicked(album: Album)

        var albumSortOrder: Int

        var sortOrder: Int

        var albumSortAscending: Boolean

        var sortAscending: Boolean

    }
}