package com.udeshcoffee.android.utils

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse

/**
 * Coil model for an album's artwork. [version] (e.g. a song's date modified) is part of the
 * cache key so edited artwork isn't served stale from the cache.
 */
data class AlbumArt(val albumId: Long, val version: Long = 0)

/**
 * Loads album artwork from MediaStore. From API 29 the legacy `albumart` URIs don't resolve,
 * so MediaStore thumbnails are used instead.
 */
class AlbumArtFetcher(private val data: AlbumArt, private val options: Options) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val resolver = options.context.contentResolver
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val width = options.size.width.pxOrElse { 512 }.coerceIn(64, 1024)
            val height = options.size.height.pxOrElse { 512 }.coerceIn(64, 1024)
            val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, data.albumId)
            resolver.loadThumbnail(albumUri, Size(width, height), null)
        } else {
            resolver.openInputStream(ContentUris.withAppendedId(ArtworkURI, data.albumId))!!.use { BitmapFactory.decodeStream(it) }
                    ?: throw IllegalStateException("No album art for ${data.albumId}")
        }
        return ImageFetchResult(image = bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory : Fetcher.Factory<AlbumArt> {
        override fun create(data: AlbumArt, options: Options, imageLoader: ImageLoader): Fetcher = AlbumArtFetcher(data, options)
    }

    class Key : Keyer<AlbumArt> {
        override fun key(data: AlbumArt, options: Options): String = "albumart:${data.albumId}:${data.version}"
    }
}
