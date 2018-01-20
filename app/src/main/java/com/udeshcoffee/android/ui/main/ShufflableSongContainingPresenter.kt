package com.udeshcoffee.android.ui.main

import com.udeshcoffee.android.model.Song

interface ShufflableSongContainingPresenter<T> : SongContainingPresenter<T> {
    fun shuffleClicked(allItems: List<Song>)
}