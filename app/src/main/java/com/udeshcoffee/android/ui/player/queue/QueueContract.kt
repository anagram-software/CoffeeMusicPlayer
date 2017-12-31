package com.udeshcoffee.android.ui.player.queue

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.BaseView

/**
 * Created by Udathari on 8/25/2017.
 */
interface QueueContract {

    interface View: BaseView<Presenter> {

        fun showQueue(songs: List<Song>)

        fun scrollTo(position: Int)

        fun setCurrentSong(position: Int, refreshAll: Boolean)

        fun showPlayerUI()

        fun hideOnClearQueue()

        fun showAddToPlaylistUI(songs: List<Song>)

    }

    interface Presenter: BasePresenter {

        fun loadQueue()

        fun closeQueue()

        fun addToPlaylist()

        fun clearQueue()

        fun itemClicked(position: Int)

        fun itemMoved(fromPosition: Int, toPosition: Int)

        fun itemRemoved(position: Int)

    }
}