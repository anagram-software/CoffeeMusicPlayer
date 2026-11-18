package com.udeshcoffee.android.extensions

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import coil3.SingletonImageLoader
import coil3.load
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.request.target
import coil3.toBitmap
import com.udeshcoffee.android.R
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.AlbumArt
import com.udeshcoffee.android.utils.getPalette
import com.udeshcoffee.android.utils.loadAlbumArtwork
import com.udeshcoffee.android.utils.loadArtistArtwork
import com.udeshcoffee.android.views.FadableLayout

private val Song.albumArt: AlbumArt get() = AlbumArt(albumId, dateModified)

/** Tints [background] and [play] from the song's artwork colours. */
fun Song.loadSongColor(context: Context, background: FadableLayout? = null, play: ImageButton? = null) {
    val request = ImageRequest.Builder(context)
            .data(albumArt)
            .size(64)
            .allowHardware(false) // Palette needs a software bitmap.
            .target(
                    onSuccess = { image ->
                        getPalette(image.toBitmap()) {
                            background?.fadeToColor(it.getMutedColor(Color.DKGRAY), 500)
                            play?.setColorFilter(it.getLightMutedColor(Color.WHITE))
                        }
                    },
                    onError = {
                        background?.fadeToColor(Color.DKGRAY, 500)
                        play?.colorFilter = null
                    })
            .build()
    SingletonImageLoader.get(context).enqueue(request)
}

fun Song.loadArtwork(context: Context, imageView: ImageView, background: FadableLayout? = null) {
    imageView.load(albumArt) {
        placeholder(R.drawable.ic_music_note_white_24dp)
        error(R.drawable.ic_music_note_white_24dp)
        crossfade(true)
        if (background != null) allowHardware(false)
        listener(
                onSuccess = { _, result ->
                    background?.let { bg -> getPalette(result.image.toBitmap()) { bg.fadeToColor(it.getMutedColor(Color.DKGRAY), 250) } }
                },
                onError = { _, _ ->
                    background?.fadeToColor(ContextCompat.getColor(context, R.color.colorPrimary), 250)
                })
    }
}

fun Album.loadArtwork(context: Context, imageView: ImageView) {
    loadAlbumArtwork(context, id, imageView)
}

fun Artist.loadArtwork(context: Context, imageView: ImageView, shouldCollect: Boolean = false) {
    loadArtistArtwork(context, id, name, imageView, shouldCollect)
}

/** Fills the genre's artwork tiles (album IDs in [Genre.arts]). */
fun Genre.loadArtwork(context: Context, imageViews: Array<ImageView>) {
    arts?.forEachIndexed { index, albumId ->
        val imageView = imageViews.getOrNull(index) ?: return@forEachIndexed
        imageView.visibility = View.INVISIBLE
        imageView.load(AlbumArt(albumId)) {
            placeholder(R.drawable.ic_music_note_white_24dp)
            listener(onSuccess = { _, _ -> imageView.fadeIn(250) })
        }
    }
}
