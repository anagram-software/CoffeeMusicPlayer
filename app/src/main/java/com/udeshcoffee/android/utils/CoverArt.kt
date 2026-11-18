package com.udeshcoffee.android.utils

import coil3.map.Mapper
import coil3.request.Options
import com.udeshcoffee.android.data.subsonic.SubsonicRepository
import com.udeshcoffee.android.model.Album
import com.udeshcoffee.android.model.Song

/** Coil model for music server artwork ([id] is a Subsonic cover art ID). */
data class CoverArt(val id: String, val size: Int = 600)

/**
 * Turns [CoverArt] into the signed-in server's getCoverArt URL (nothing when signed out). The URL
 * carries the auth token, so the memory cache key is the model, not the URL.
 */
class CoverArtMapper(private val subsonic: () -> SubsonicRepository) : Mapper<CoverArt, String> {
    override fun map(data: CoverArt, options: Options): String? = subsonic().coverArtUrl(data.id, data.size)
}

/** Artwork model for a song: MediaStore album art, or server cover art. */
fun Song.artworkData(): Any? =
        if (isRemote) (coverArt ?: albumRemoteId)?.let { CoverArt(it) } else AlbumArt(albumId, dateModified)

/** Artwork model for an album. */
fun Album.artworkData(): Any? =
        if (isRemote) (coverArt ?: remoteId)?.let { CoverArt(it) } else AlbumArt(id, dateModified)
