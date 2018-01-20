package com.udeshcoffee.android.ui.main.detail.playlistdetail

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.main.SongContainingPresenter
import com.udeshcoffee.android.ui.main.SongContainingView

/**
 * Created by Udathari on 9/12/2017.
 */
interface PlaylistDetailContract {

    interface View : SongContainingView<Presenter> {

        fun setPlaylist(isEditable: Boolean)

        fun showRenameUI(playlistId: Long, playlistTitle: String)

        fun showDeleteUI(playlistId: Long, playlistTitle: String)

        fun showAddToPlaylistUI(id: Long, title: String)

        fun showAddToPlaylistDialog(songs: ArrayList<Song>, thisPlaylistId: Long?)

    }

    interface Presenter : SongContainingPresenter<View> {

        fun addToPlaylist()

        fun rename()

        fun delete()

        fun itemMoved(fromPosition: Int, toPosition: Int)

        fun itemRemoved(id: Long, position: Int)

        var playlistId: Long

        var playlistType: Int

        var playlistTitle: String
    }
}