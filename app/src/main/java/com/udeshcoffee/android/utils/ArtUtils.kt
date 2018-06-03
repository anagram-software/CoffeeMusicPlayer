package com.udeshcoffee.android.utils

import android.content.ComponentCallbacks
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.udeshcoffee.android.App
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.remote.RemoteDataSource
import com.udeshcoffee.android.extensions.toLastFMArtistQuery
import com.udeshcoffee.android.utils.blur.BlurTransformation
import com.udeshcoffee.android.views.FadableLayout
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Udathari on 9/23/2017.
 */
fun loadAlbumArtwork(context: Context, id: Long, imageView: ImageView, animate: Boolean = true, isBlurred: Boolean = false,
                     callback: ((success: Boolean) -> Unit)? = null){
    val uri = ContentUris.withAppendedId(ArtworkURI, id)
    if (uri != null) {
        val options = RequestOptions().error(R.drawable.ic_album_white_24dp)
        if (isBlurred)
            options.transform(BlurTransformation())
        val glide = Glide.with(context)
                .load(uri)
                .apply(options)
        if (animate)
            glide.transition(DrawableTransitionOptions.withCrossFade(250))
        glide.listener(object : RequestListener<Drawable>{
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?,
                                      isFirstResource: Boolean): Boolean {
                callback?.let { it(false) }
                return false
            }

            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?,
                                         dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                callback?.let { it(true) }
                return false
            }
        })
                .into(imageView)
    }
}

fun <T> loadAlbumArtworkFromUri(context: Context, t: T, imageView: ImageView){
    Glide.with(context)
            .asBitmap()
            .load(t)
            .apply(RequestOptions().placeholder(R.drawable.ic_music_note_white_24dp)
                    .diskCacheStrategy(DiskCacheStrategy.DATA).skipMemoryCache(true))
            .into(imageView)
}

fun <T> loadAlbumArtworkFromUriSynchronous(t: T): Bitmap{
    return Glide.with(App.instance)
            .asBitmap()
            .load(t)
            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.DATA).skipMemoryCache(true))
            .into(500, 500)
            .get()
}

fun loadUnknownArtist(imageView: ImageView) {
    imageView.setImageResource(R.drawable.ic_person_white_24dp)
}

fun loadArtistArtwork(context: Context,
                      glide: RequestManager,
                      id: Long,
                      name: String,
                      imageView: ImageView,
                      shouldCollect: Boolean,
                      isBlur: Boolean = false) {
    if (name == "<unknown>") {
        loadUnknownArtist(imageView)
        return
    }

    val root = Environment.getExternalStorageDirectory().toString()
    val myDir = File("$root/CoffeePlayer/ArtistImages/$id.jpg")
    if (myDir.exists()) {
        Log.d("loadArtwork", "file found")
        loadFileArtistArtwork(glide, myDir, imageView, isBlur)
    } else {
        loadUnknownArtist(imageView)
        if (shouldCollect)
            collectArtistArtwork(context, glide, id, name, imageView)
    }
}

fun loadFileArtistArtwork(glide: RequestManager,
                          dir: File,
                          imageView: ImageView,
                          isBlur: Boolean){

    val colors = IntArray(2)
    colors[1] = Color.TRANSPARENT

    val options = RequestOptions()
            .error(R.drawable.ic_person_white_24dp)
            .signature(ObjectKey(dir.lastModified()))
    if (isBlur) {
        options.override(imageView.width / 5, imageView.height / 5)
                .transform(BlurTransformation(25))
    }

    glide.load(dir)
            .apply(options)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
}

fun collectArtistArtwork(context: Context,
                         glide: RequestManager,
                         id: Long,
                         name: String,
                         imageView: ImageView) {
    val pref = PreferenceManager.getDefaultSharedPreferences(context)
    if (!pref.getBoolean(PreferenceUtil.PREF_COLLECT_CONTENT, true))
        return
    if (!isNetworkAvailable(context, pref.getBoolean(PreferenceUtil.PREF_WIFI_ONLY, true)) )
        return
    var tempName = name
    tempName = tempName.toLastFMArtistQuery()
    RemoteDataSource.searchLastFMArtist(tempName)
            .subscribe ({
                val image = it.artist?.image
                glide.asBitmap().load(image?.get(image.size - 1)?.text)
                        .apply(RequestOptions().placeholder(R.drawable.ic_person_white_24dp))
                        .listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?,
                                                      isFirstResource: Boolean): Boolean = false

                            override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?,
                                                         dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                resource?.let { saveArtistArt(id, it) }
                                return false
                            }
                        })
                        .transition(BitmapTransitionOptions.withCrossFade(250))
                        .into(imageView)
            }, {})
}


fun saveArtistArt(contentResolver: ContentResolver, id: Long, uri: Uri) {
    Log.d("saveArtistArt", "uri")
    try {
        Log.d("saveArtistArt", "saving")
        val imageStream = contentResolver.openInputStream(uri)
        val selectedImage = BitmapFactory.decodeStream(imageStream)
        saveArtistArt(id, selectedImage)
    } catch (e: FileNotFoundException) {
    }
}

private fun saveArtistArt(id: Long?, bm: Bitmap) {
    Log.d("saveArtistArt", "bitmap")
    val root = Environment.getExternalStorageDirectory().toString()
    val myDir = File(root + "/CoffeePlayer/ArtistImages")

    if (!myDir.exists()) {
        myDir.mkdirs()
    }

    val noMediaFile = File(myDir, ".nomedia")
    if (!noMediaFile.exists()) {
        try {
            noMediaFile.mkdirs()
            noMediaFile.createNewFile()
        } catch (e: IOException) {
            Log.d("saveArtistArt", "noMediaFileError: $e")
        }
    }
    val fName = id!!.toString() + ".jpg"
    Log.d("saveArtistArt", "filename: $fName")
    val file = File(myDir, fName)
    if (file.exists()) {
        Log.d("saveArtistArt", "file exists")
        file.delete()
    }
    try {
        val out = FileOutputStream(file)
        bm.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        Log.d("saveArtistArt", "saving")
    } catch (e: Exception) {
        Log.d("saveArtistArt", "error $e")
    }
}

fun deleteArtistArt(id: Long) {
    val root = Environment.getExternalStorageDirectory().toString()
    val myDir = File(root + "/CoffeePlayer/ArtistImages")
    myDir.mkdirs()
    val fName = id.toString() + ".jpg"
    val file = File(myDir, fName)
    if (file.exists()) file.delete()

}