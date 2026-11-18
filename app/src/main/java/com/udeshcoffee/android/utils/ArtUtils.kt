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

/** Where the artist's downloaded or chosen image is stored. */
fun artistImageFile(artistId: Long): File = File(artistImageDir(), "$artistId.jpg")

/**
 * Downloads the artist's image from Last.fm when settings allow it (collect content, Wi-Fi only).
 * Returns true if an image was saved.
 */
suspend fun collectArtistImage(context: Context, id: Long, name: String): Boolean {
    val pref = PreferenceManager.getDefaultSharedPreferences(context)
    if (!pref.getBoolean(PreferenceUtil.PREF_COLLECT_CONTENT, true)) return false
    if (!isNetworkAvailable(context, pref.getBoolean(PreferenceUtil.PREF_WIFI_ONLY, true))) return false
    val url = RemoteDataSource.searchLastFMArtist(name.toLastFMArtistQuery())?.image?.takeIf { it.isNotEmpty() } ?: return false
    val bitmap = loadBitmap(context, url) ?: return false
    withContext(Dispatchers.IO) { saveArtistArt(id, bitmap) }
    return true
}

/** Saves an image the user picked as the artist's image. */
suspend fun saveArtistImage(context: Context, id: Long, uri: Uri) = withContext(Dispatchers.IO) {
    saveArtistArt(context.contentResolver, id, uri)
}
