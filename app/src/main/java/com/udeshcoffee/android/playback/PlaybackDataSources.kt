package com.udeshcoffee.android.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource

/**
 * Where ExoPlayer reads media from: content:// URIs for device songs, authenticated HTTP stream
 * URLs for server songs, through [downloads] (the offline downloads, see `DownloadRepository`).
 *
 * Server items carry `customCacheKey` = their media ID (see [toMediaItem]), the key their download
 * is stored under, so a downloaded song is read from storage without opening the stream URL (which
 * may change with a new auth token, format or bit rate). Streaming doesn't write to the cache:
 * only downloads fill it.
 */
@OptIn(UnstableApi::class)
fun playbackMediaSourceFactory(context: Context, downloads: Cache): MediaSource.Factory =
        DefaultMediaSourceFactory(CacheDataSource.Factory()
                .setCache(downloads)
                .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
                .setCacheWriteDataSinkFactory(null))
