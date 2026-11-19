package com.udeshcoffee.android.playback

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.udeshcoffee.android.utils.ArtworkURI
import java.util.concurrent.Executors

/**
 * Loads notification and lock-screen artwork. The legacy `albumart` content URIs used in the
 * media metadata don't resolve on API 29+, so those use MediaStore thumbnails instead.
 */
@OptIn(UnstableApi::class)
class ArtworkBitmapLoader(private val context: Context) : BitmapLoader {

    private val executor: ListeningExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    private val fallback = DataSourceBitmapLoader.Builder(context).build()

    override fun supportsMimeType(mimeType: String): Boolean = fallback.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = fallback.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val isAlbumArt = uri.toString().startsWith(ArtworkURI.toString())
        if (!isAlbumArt || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return fallback.loadBitmap(uri)
        return executor.submit<Bitmap> {
            val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ContentUris.parseId(uri))
            context.contentResolver.loadThumbnail(albumUri, Size(512, 512), null)
        }
    }
}
