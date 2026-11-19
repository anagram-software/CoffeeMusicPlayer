@file:OptIn(UnstableApi::class)

package com.udeshcoffee.android.playback

import android.content.ContentUris
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.udeshcoffee.android.data.subsonic.RemoteIds
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.ArtworkURI
import com.udeshcoffee.android.utils.MediaAccess
import org.koin.core.context.GlobalContext

private const val EXTRA_ALBUM_ID = "album_id"
private const val EXTRA_ARTIST_ID = "artist_id"
private const val EXTRA_DURATION = "duration"
private const val EXTRA_DATE_ADDED = "date_added"
private const val EXTRA_DATE_MODIFIED = "date_modified"
private const val EXTRA_DISC = "disc"
private const val EXTRA_COVER_ART = "cover_art"
private const val EXTRA_ALBUM_REMOTE_ID = "album_remote_id"
private const val EXTRA_ARTIST_REMOTE_ID = "artist_remote_id"

/** Media ID prefix of music server songs; device songs use the bare MediaStore ID. */
const val SUBSONIC_MEDIA_PREFIX = "subsonic:"

private val subsonic: SubsonicRepository get() = GlobalContext.get().get()

/** The media ID for a song: the MediaStore audio ID, or [SUBSONIC_MEDIA_PREFIX] + the server's song ID. */
val Song.mediaId: String get() = remoteId?.let { SUBSONIC_MEDIA_PREFIX + it } ?: id.toString()

/** A playable MediaItem for this song (a content URI, or the server's stream URL). */
fun Song.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_ALBUM_ID, albumId)
        putLong(EXTRA_ARTIST_ID, artistId)
        putLong(EXTRA_DURATION, duration)
        putInt(EXTRA_DATE_ADDED, dateAdded)
        putLong(EXTRA_DATE_MODIFIED, dateModified)
        putInt(EXTRA_DISC, discNumber)
        putString(EXTRA_COVER_ART, coverArt)
        putString(EXTRA_ALBUM_REMOTE_ID, albumRemoteId)
        putString(EXTRA_ARTIST_REMOTE_ID, artistRemoteId)
    }
    val remote = remoteId
    val uri = if (remote != null) subsonic.streamUrl(remote)?.toUri() else MediaAccess.songUri(id)
    val artwork = if (remote != null) subsonic.coverArtUrl(coverArt, 512)?.toUri() else ContentUris.withAppendedId(ArtworkURI, albumId)
    return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            // Stream URLs carry the auth token and quality settings; cache server audio by song instead.
            .setCustomCacheKey(if (remote != null) mediaId else null)
            .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artistName)
                    .setAlbumTitle(albumName)
                    .setArtworkUri(artwork)
                    .setTrackNumber(track)
                    .setReleaseYear(year)
                    .setDurationMs(duration)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(extras)
                    .build())
            .build()
}

/** Rebuilds the Song from a MediaItem created by [toMediaItem]. */
fun MediaItem.toSong(): Song {
    val m = mediaMetadata
    val extras = m.extras ?: Bundle.EMPTY
    val remoteId = mediaId.takeIf { it.startsWith(SUBSONIC_MEDIA_PREFIX) }?.removePrefix(SUBSONIC_MEDIA_PREFIX)
    return Song(
            id = remoteId?.let { RemoteIds.song(it) } ?: mediaId.toLongOrNull() ?: -1,
            title = m.title?.toString() ?: "",
            albumId = extras.getLong(EXTRA_ALBUM_ID),
            albumName = m.albumTitle?.toString() ?: "<unknown>",
            artistId = extras.getLong(EXTRA_ARTIST_ID),
            artistName = m.artist?.toString() ?: "<unknown>",
            duration = extras.getLong(EXTRA_DURATION, m.durationMs ?: 0),
            year = m.releaseYear ?: 0,
            dateAdded = extras.getInt(EXTRA_DATE_ADDED),
            dateModified = extras.getLong(EXTRA_DATE_MODIFIED),
            track = m.trackNumber ?: 0,
            remoteId = remoteId,
            coverArt = extras.getString(EXTRA_COVER_ART),
            albumRemoteId = extras.getString(EXTRA_ALBUM_REMOTE_ID),
            artistRemoteId = extras.getString(EXTRA_ARTIST_REMOTE_ID)
    ).also { it.discNumber = extras.getInt(EXTRA_DISC) }
}

/** Restores the playback URI, which media controllers don't send to the session. */
fun MediaItem.withPlaybackUri(): MediaItem {
    if (localConfiguration != null) return this
    if (mediaId.startsWith(SUBSONIC_MEDIA_PREFIX)) {
        val url = subsonic.streamUrl(mediaId.removePrefix(SUBSONIC_MEDIA_PREFIX)) ?: return this
        return buildUpon().setUri(url).setCustomCacheKey(mediaId).build()
    }
    val id = mediaId.toLongOrNull() ?: return this
    return buildUpon().setUri(MediaAccess.songUri(id)).build()
}
