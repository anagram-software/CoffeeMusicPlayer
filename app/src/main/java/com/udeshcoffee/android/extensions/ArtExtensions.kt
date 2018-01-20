package com.udeshcoffee.android.extensions

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.MediaStoreSignature
import com.udeshcoffee.android.R
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Artist
import com.udeshcoffee.android.model.Genre
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.ArtworkURI
import com.udeshcoffee.android.utils.getPalette
import com.udeshcoffee.android.utils.loadAlbumArtwork
import com.udeshcoffee.android.utils.loadArtistArtwork
import com.udeshcoffee.android.views.FadableLayout


/**
 * Created by Udathari on 9/12/2017.
 */
fun Song.loadFadableBack(context: Context, background: FadableLayout, callback: (()->Unit)? = null) {
    val uri = ContentUris.withAppendedId(ArtworkURI, albumId)
    if (uri != null) {
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .apply(RequestOptions().placeholder(R.drawable.ic_music_note_white_24dp)
                        .signature(MediaStoreSignature("image/*", this.dateModified, 0)))
                .into(object : SimpleTarget<Bitmap>(50, 50) {

                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        resource?.let {
                            getPalette(it, {
                                val nextColor = it.getMutedColor(Color.DKGRAY)
                                background.fadeToColor(nextColor, 500)
                            })
                            callback?.let { it() }
                        }
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        background.fadeToColor(Color.DKGRAY, 500)
                        callback?.let { it() }
                    }

                })
    }
}


fun Song.loadArtwork(context: Context, imageView: ImageView, background: FadableLayout? = null) {
    val uri = ContentUris.withAppendedId(ArtworkURI, albumId)
    if (uri != null) {
        Glide.with(context)
                .asBitmap()
                .load(uri)
                .listener(object : RequestListener<Bitmap>{
                    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?,
                                                 dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        background?.let {
                            resource?.let {
                                getPalette(it, {
                                    val nextColor = it.getMutedColor(Color.DKGRAY)
                                    background.fadeToColor(nextColor, 250)
                                })
                            }
                        }
                        return false
                    }

                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        background?.fadeToColor(ContextCompat.getColor(context, R.color.colorPrimary), 250)
                        return false
                    }
                })
                .apply(RequestOptions().placeholder(R.drawable.ic_music_note_white_24dp))
                .into(imageView)
    }
}

fun Album.loadArtwork(context: Context, imageView: ImageView) {
    loadAlbumArtwork(context, id, imageView)
}

fun Artist.loadArtwork(context: Context,
                       glide: RequestManager,
                       imageView: ImageView,
                       shouldCollect: Boolean = false) {
    loadArtistArtwork(context, glide, id, name, imageView, shouldCollect)
}

fun Genre.loadArtwork(context: Context, imageViews: Array<ImageView>) {
    this.arts?.let {
        for (count in 0 until it.size) {
            val uri = ContentUris.withAppendedId(ArtworkURI, it[count])
            val imageView = imageViews[count]
            if (uri != null) {
                Glide.with(context)
                        .load(uri)
                        .apply(RequestOptions().placeholder(R.drawable.ic_music_note_white_24dp).centerCrop())
                        .into(object : SimpleTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable?, transition: Transition<in Drawable>?) {
                                imageView.setImageDrawable(resource)
                                imageView.fadeIn(250)
                            }

                        })
            }
        }
    }
}