package com.udeshcoffee.android.ui.common.viewmodels

import android.app.Application
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.extensions.playSong
import com.udeshcoffee.android.extensions.queueSong
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.SingleLiveEvent
import com.udeshcoffee.android.utils.ToastMessage

abstract class DetailSongViewModel (
        application: Application,
        protected val dataRepository: DataRepository
): SongContainingViewModel(application) {

    // Events
    val showFavoriteToast = ToastMessage()
    val showAddToPlaylistDialog = SingleLiveEvent<List<Song>>()

    fun playClick() {
        songs.value?.let { playSong(0, it, true) }
    }

    fun playNextClick() {
        songs.value?.let { queueSong(it, true) }
    }

    fun queueClick() {
        songs.value?.let { queueSong(it, false) }
    }

    fun addToFavoritesClick() {
        songs.value?.let {
            if (it.isNotEmpty()) {
                dataRepository.addToFavorites(it)
                showFavoriteToast.setMessage("Added to favorites")
            } else {
                showFavoriteToast.setMessage("Removed from favorites")
            }
        }
    }

    fun addToPlaylistClick() {
        songs.value?.let { showAddToPlaylistDialog.value = it }
    }

}