package com.udeshcoffee.android.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource

/**
 * Where ExoPlayer reads media from: content:// URIs for device songs, authenticated HTTP stream
 * URLs for server songs.
 *
 * This is the hook for caching / offline listening of server songs: wrap the data source factory
 * in a `CacheDataSource.Factory` backed by a `SimpleCache` (and serve downloads from the same
 * cache). Server items carry `customCacheKey` = their media ID (see [toMediaItem]), so cached audio
 * stays valid when the stream URL changes (new auth token, format or bit rate).
 */
@OptIn(UnstableApi::class)
fun playbackMediaSourceFactory(context: Context): MediaSource.Factory =
        DefaultMediaSourceFactory(DefaultDataSource.Factory(context))
