package com.udeshcoffee.android.ui.main

import com.udeshcoffee.android.model.Song

interface ShufflableSongContainingPresenter : SongContainingPresenter {
    fun shuffleClicked(allItems: List<Song>)
}