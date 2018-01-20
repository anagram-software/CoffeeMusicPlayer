package com.udeshcoffee.android.ui.main

import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.ui.BasePresenter
import java.util.*

interface SongContainingPresenter<T>: BasePresenter<T> {
    fun playClick(songs: ArrayList<Song>)

    fun playNextClick(songs: ArrayList<Song>)

    fun queueClick(songs: ArrayList<Song>)

    fun addToFavoritesClick(songs: ArrayList<Song>)

    fun addToPlaylistClick(songs: ArrayList<Song>)

    fun itemClicked(position: Int, allItems: List<Song>)

    fun itemLongClicked(item: Song)

}