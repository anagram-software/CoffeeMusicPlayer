package com.udeshcoffee.android.ui.main.detail.artistdetail

import android.content.Intent
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.main.ShufflableSongContainingPresenter
import com.udeshcoffee.android.ui.main.SongContainingView

/**
* Created by Udathari on 9/12/2017.
*/
interface ArtistDetailContract {

    interface View : SongContainingView<Presenter> {

        fun populateAlbumItems(items: List<Album>)

        fun setTags(tags: Array<String>)

        fun setBio(bio: String)

        fun showAlbum(position: Int)

        fun artistArtChanged(id: Long, name: String)

        fun artistArtDeleted()

        fun showCollectionLongDialog(title: String, songs: List<Song>)

        fun showSelectImageUI()

    }

    interface Presenter : ShufflableSongContainingPresenter<View> {

        fun result(requestCode: Int, resultCode: Int, data: Intent?)

        fun fetchSongs()

        fun fetchAlbums()

        fun albumItemClicked(position: Int)

        fun albumItemLongClicked(album: Album)

        fun selectImage()

        fun deleteImage()

        var artistId: Long

        var artistName: String

        var albumSortOrder: Int

        var sortOrder: Int

        var albumSortAscending: Boolean

        var sortAscending: Boolean

    }
}