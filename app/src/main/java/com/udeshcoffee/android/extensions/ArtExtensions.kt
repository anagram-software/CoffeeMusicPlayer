package com.udeshcoffee.android.extensions

import android.content.Context
import android.widget.ImageView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.udeshcoffee.android.R
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.artworkData

/** Loads the song's album art into a View-based ImageView (used by the widget configuration screen). */
fun Song.loadArtwork(@Suppress("UNUSED_PARAMETER") context: Context, imageView: ImageView) {
    imageView.load(artworkData()) {
        placeholder(R.drawable.ic_music_note_white_24dp)
        error(R.drawable.ic_music_note_white_24dp)
        crossfade(true)
    }
}
