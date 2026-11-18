package com.udeshcoffee.android.playback

import android.content.ContentUris
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.ArtworkURI
import com.udeshcoffee.android.utils.MediaAccess

private const val EXTRA_ALBUM_ID = "album_id"
private const val EXTRA_ARTIST_ID = "artist_id"
private const val EXTRA_DURATION = "duration"
private const val EXTRA_DATE_ADDED = "date_added"
private const val EXTRA_DATE_MODIFIED = "date_modified"
private const val EXTRA_DISC = "disc"

/** A playable MediaItem for this song. The media ID is the MediaStore audio ID. */
fun Song.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_ALBUM_ID, albumId)
        putLong(EXTRA_ARTIST_ID, artistId)
        putLong(EXTRA_DURATION, duration)
        putInt(EXTRA_DATE_ADDED, dateAdded)
        putLong(EXTRA_DATE_MODIFIED, dateModified)
        putInt(EXTRA_DISC, discNumber)
    }
    return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(MediaAccess.songUri(id))
            .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artistName)
                    .setAlbumTitle(albumName)
                    .setArtworkUri(ContentUris.withAppendedId(ArtworkURI, albumId))
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
    return Song(
            id = mediaId.toLongOrNull() ?: -1,
            title = m.title?.toString() ?: "",
            albumId = extras.getLong(EXTRA_ALBUM_ID),
            albumName = m.albumTitle?.toString() ?: "<unknown>",
            artistId = extras.getLong(EXTRA_ARTIST_ID),
            artistName = m.artist?.toString() ?: "<unknown>",
            duration = extras.getLong(EXTRA_DURATION, m.durationMs ?: 0),
            year = m.releaseYear ?: 0,
            dateAdded = extras.getInt(EXTRA_DATE_ADDED),
            dateModified = extras.getLong(EXTRA_DATE_MODIFIED),
            track = m.trackNumber ?: 0
    ).also { it.discNumber = extras.getInt(EXTRA_DISC) }
}

/** Restores the playback URI, which media controllers don't send to the session. */
fun MediaItem.withPlaybackUri(): MediaItem {
    if (localConfiguration != null) return this
    val id = mediaId.toLongOrNull() ?: return this
    return buildUpon().setUri(MediaAccess.songUri(id)).build()
}
