package com.udeshcoffee.android.data.remote.lastfm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Last.fm read API (authenticated with the app's API key, added by the HTTP client). */
interface LastFmService {
    @GET("2.0/?method=artist.getinfo")
    suspend fun getArtistInfo(@Query("artist") artist: String,
                              @Query("autocorrect") autoCorrect: Int = 1): ArtistResponse

    @GET("2.0/?method=track.getInfo")
    suspend fun getTrackInfo(@Query("track") track: String,
                             @Query("artist") artist: String,
                             @Query("autocorrect") autoCorrect: Int = 1): TrackInfoResponse

    @GET("2.0/?method=track.search")
    suspend fun searchTrack(@Query("track") track: String,
                            @Query("artist") artist: String? = null,
                            @Query("limit") limit: Int = 5): TrackSearchResponse
}

// artist.getinfo

@Serializable
data class ArtistResponse(val artist: Artist? = null, val error: Int? = null)

@Serializable
data class Artist(val name: String = "", val url: String = "", val tags: Tags? = null, val bio: Bio? = null)

@Serializable
data class Tags(val tag: List<Tag> = emptyList())

@Serializable
data class Tag(val name: String = "", val url: String = "")

@Serializable
data class Bio(val summary: String = "")

data class ImageArtist(val artist: Artist, val image: String)

// track.getInfo / track.search

@Serializable
data class TrackInfoResponse(val track: TrackInfo? = null, val error: Int? = null)

@Serializable
data class TrackInfo(
        val name: String = "",
        val artist: TrackArtist? = null,
        val album: TrackAlbum? = null,
        @SerialName("toptags") val topTags: Tags? = null
)

@Serializable
data class TrackArtist(val name: String = "")

@Serializable
data class TrackAlbum(val title: String = "", val image: List<Image> = emptyList())

@Serializable
data class Image(@SerialName("#text") val url: String = "", val size: String = "")

@Serializable
data class TrackSearchResponse(val results: TrackSearchResults? = null)

@Serializable
data class TrackSearchResults(@SerialName("trackmatches") val trackMatches: TrackMatches? = null)

@Serializable
data class TrackMatches(val track: List<TrackMatch> = emptyList())

@Serializable
data class TrackMatch(val name: String = "", val artist: String = "")

/** Metadata found for a song, used by the tag editor. Last.fm has no release year. */
data class TrackMetadata(
        val title: String,
        val artist: String,
        val album: String?,
        val genre: String?,
        val artworkUrl: String?
)
