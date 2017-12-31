package com.udeshcoffee.android.utils

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
import com.udeshcoffee.android.toLastFMArtistQuery
import com.udeshcoffee.android.views.FadableLayout
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Udathari on 9/23/2017.
 */
fun loadCoolCardView(context: Context,
                     albumId: Long? = null,
                     artistId: Long? = null,
                     background: FadableLayout) {

    val uri = albumId?.let {
        ContentUris.withAppendedId(ArtworkURI, albumId)
    }

    val myDir = artistId?.let {
        val root = Environment.getExternalStorageDirectory().toString()
        File("$root/CoffeePlayer/ArtistImages/$artistId.jpg")
    }

    if (uri != null || myDir != null) {
        val glide = Glide.with(context)
                .asBitmap()

        val load = when {
            uri != null -> glide.load(uri)
            myDir != null -> glide.load(myDir)
            else -> throw IllegalArgumentException("Should supply one of the twp ids")
        }
        load.apply(RequestOptions().placeholder(R.drawable.ic_music_note_white_24dp))
                .into(object : SimpleTarget<Bitmap>(50, 50) {

                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        resource?.let {
                            getPalette(it, {
                                val nextColor = it.getMutedColor(Color.DKGRAY)
                                background.fadeToColor(nextColor, 300)
                            })
                        }
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        background.fadeToColor(Color.DKGRAY, 300)
                    }

                })
    }
}

fun loadAlbumArtwork(context: Context, id: Long, imageView: ImageView){
    val uri = ContentUris.withAppendedId(ArtworkURI, id)
    if (uri != null) {
        Glide.with(context)
                .load(uri)
                .apply(RequestOptions().placeholder(R.drawable.ic_album_white_24dp))
                .transition(DrawableTransitionOptions.withCrossFade(250))
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
                      shouldCollect: Boolean) {
    if (name == "<unknown>") {
        loadUnknownArtist(imageView)
        return
    }

    val root = Environment.getExternalStorageDirectory().toString()
    val myDir = File("$root/CoffeePlayer/ArtistImages/$id.jpg")
    if (myDir.exists()) {
        Log.d("loadArtwork", "file found")
        loadFileArtistArtwork(glide, myDir, imageView)
    } else {
        loadUnknownArtist(imageView)
        if (shouldCollect)
            collectArtistArtwork(context, glide, id, name, imageView)
    }
}

fun loadFileArtistArtwork(glide: RequestManager,
                          dir: File,
                          imageView: ImageView){

    val colors = IntArray(2)
    colors[1] = Color.TRANSPARENT
    glide.load(dir)
            .apply(RequestOptions().placeholder(R.drawable.ic_person_white_24dp).signature(ObjectKey(dir.lastModified())))
            .transition(DrawableTransitionOptions.withCrossFade(250))
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
    val dataRepository = Injection.provideDataRepository(context.applicationContext)
    dataRepository.searchLastFMArtist(tempName)
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