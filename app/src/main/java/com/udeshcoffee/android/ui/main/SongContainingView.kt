package com.udeshcoffee.android.ui.main

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BaseView

interface SongContainingView<T> : BaseView<T> {

    fun populateItems(items: List<Song>)

    fun showSongLongDialog(song: Song)

    fun showAddToPlaylistDialog(songs: ArrayList<Song>)

    fun showFavoritesToast(isFavorite: Boolean)

    fun setCurrentSong(id: Long)

}