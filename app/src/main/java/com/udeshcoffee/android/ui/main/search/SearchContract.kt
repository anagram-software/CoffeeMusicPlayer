package com.udeshcoffee.android.ui.main.search

import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.main.SongContainingPresenter
import com.udeshcoffee.android.ui.main.SongContainingView

/**
 * Created by Udathari on 9/12/2017.
 */
interface SearchContract {

    interface View : SongContainingView<Presenter> {

        fun populateAlbumItems(items: List<Album>)

        fun populateArtistItems(items: List<Artist>)

        fun showAlbum(position: Int)

        fun showArtist(position: Int)

        fun showCollectionLongDialog(title: String, songs: List<Song>)

    }

    interface Presenter : SongContainingPresenter<View> {

        fun search(query: String)

        fun albumItemClicked(position: Int)

        fun albumItemLongClicked(album: Album)

        fun albumItemOptionClicked(album: Album)

        fun artistItemClicked(position: Int)

        fun artistItemLongClicked(artist: Artist)

        fun artistItemOptionClicked(artist: Artist)

    }
}