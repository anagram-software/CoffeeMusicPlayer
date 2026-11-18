package com.udeshcoffee.android.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.preference.PreferenceManager
import coil3.SingletonImageLoader
import coil3.load
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.toBitmap
import com.udeshcoffee.android.App
import com.udeshcoffee.android.R
import com.udeshcoffee.android.data.remote.RemoteDataSource
import com.udeshcoffee.android.extensions.toLastFMArtistQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/** Background work started from views (artist image downloads) that should outlive the view. */
private val artScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

fun loadAlbumArtwork(context: Context, id: Long, imageView: ImageView, animate: Boolean = true,
                     callback: ((success: Boolean) -> Unit)? = null) {
    imageView.load(AlbumArt(id)) {
        error(R.drawable.ic_album_white_24dp)
        crossfade(animate)
        listener(onError = { _, _ -> callback?.invoke(false) }, onSuccess = { _, _ -> callback?.invoke(true) })
    }
}

/** Shows an image picked by the user ([data] is a content Uri) or found online (a URL). */
fun loadAlbumArtworkFromUri(context: Context, data: Any?, imageView: ImageView) {
    imageView.load(data) {
        placeholder(R.drawable.ic_music_note_white_24dp)
        memoryCachePolicy(CachePolicy.DISABLED)
    }
}

/** Decodes an image (content Uri, URL, [AlbumArt]...) into a software bitmap, or null on failure. */
suspend fun loadBitmap(context: Context, data: Any?, size: Int = 500): Bitmap? {
    val request = ImageRequest.Builder(context)
            .data(data)
            .size(size)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
    return (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
}

fun loadUnknownArtist(imageView: ImageView) {
    imageView.setImageResource(R.drawable.ic_person_white_24dp)
}

fun loadArtistArtwork(context: Context, id: Long, name: String, imageView: ImageView, shouldCollect: Boolean) {
    if (name == "<unknown>") {
        loadUnknownArtist(imageView)
        return
    }
    val file = File(artistImageDir(), "$id.jpg")
    if (file.exists()) {
        loadFileArtistArtwork(file, imageView)
    } else {
        loadUnknownArtist(imageView)
        if (shouldCollect) collectArtistArtwork(context, id, name, imageView)
    }
}

fun loadFileArtistArtwork(file: File, imageView: ImageView) {
    imageView.load(file) {
        error(R.drawable.ic_person_white_24dp)
        crossfade(true)
        // The file is replaced when a new image is downloaded or chosen.
        memoryCacheKey("artist:${file.path}:${file.lastModified()}")
    }
}

/** Downloads the artist's image from Last.fm (if allowed by settings), saves it, and shows it. */
fun collectArtistArtwork(context: Context, id: Long, name: String, imageView: ImageView) {
    val pref = PreferenceManager.getDefaultSharedPreferences(context)
    if (!pref.getBoolean(PreferenceUtil.PREF_COLLECT_CONTENT, true)) return
    if (!isNetworkAvailable(context, pref.getBoolean(PreferenceUtil.PREF_WIFI_ONLY, true))) return
    // Views get recycled; only show the result if this view still displays the same artist.
    imageView.setTag(R.id.artist_art_request, id)
    artScope.launch {
        val url = RemoteDataSource.searchLastFMArtist(name.toLastFMArtistQuery())?.image?.takeIf { it.isNotEmpty() }
                ?: return@launch
        val bitmap = loadBitmap(context, url) ?: return@launch
        withContext(Dispatchers.IO) { saveArtistArt(id, bitmap) }
        if (imageView.getTag(R.id.artist_art_request) == id) loadFileArtistArtwork(File(artistImageDir(), "$id.jpg"), imageView)
    }
}

fun saveArtistArt(contentResolver: ContentResolver, id: Long, uri: Uri) {
    try {
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }?.let { saveArtistArt(id, it) }
    } catch (e: FileNotFoundException) {
        Log.w("ArtUtils", "Couldn't read $uri", e)
    }
}

private fun saveArtistArt(id: Long, bitmap: Bitmap) {
    try {
        File(artistImageDir(), "$id.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    } catch (e: Exception) {
        Log.w("ArtUtils", "Couldn't save artist image", e)
    }
}

fun deleteArtistArt(id: Long) {
    File(artistImageDir(), "$id.jpg").delete()
}

/**
 * Downloaded artist images. App-private storage needs no permission and isn't scanned into the
 * user's gallery (images used to live in /sdcard/CoffeePlayer/ArtistImages).
 */
private fun artistImageDir(): File = File(App.instance.filesDir, "ArtistImages").apply { mkdirs() }
