package com.udeshcoffee.android.ui.main.home

import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.BaseView

/**
* Created by Udathari on 12/17/2017.
*/
interface HomeContract {

    interface View: BaseView<Presenter> {

        fun setLibraryStats(songs: Int? = null, albums: Int? = null,
                            artists: Int? = null, playlists: Int? = null)

        fun showHideInfo(isShow: Boolean)

        fun setTopSongs(items: List<Song>)

        fun setTopAlbums(items: List<Album>)

        fun setTopArtists(items: List<Artist>)

        fun setRecentlyAdded(items: List<Song>)

        fun showSongLongDialog(song: Song)

        fun showCollectionLongDialog(title: String, songs: List<Song>)

        fun showAlbum(position: Int)

        fun showArtist(position: Int)
    }

    interface Presenter: BasePresenter<View> {

        fun itemClicked(position: Int, allItems: List<Song>)

        fun itemLongClicked(item: Song)

        fun albumItemClicked(position: Int)

        fun albumItemLongClicked(item: Album)

        fun artistItemClicked(position: Int)

        fun artistItemLongClicked(item: Artist)

    }

}