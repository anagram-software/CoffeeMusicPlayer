package com.udeshcoffee.android.data.lastfm

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/** A Last.fm user's listening stats, as shown on Home. */
data class LastFmStats(
        val name: String,
        val realName: String?,
        val imageUrl: String?,
        val profileUrl: String?,
        val scrobbles: Long,
        val artists: Long,
        val tracks: Long,
        val albums: Long,
        /** Unix seconds; null if unknown. */
        val registeredSec: Long?,
        /** This week's top album (Last.fm's `7day` period); null if nothing was scrobbled. */
        val topAlbum: TopItem?,
        /** This week's top artists and tracks (up to 5 each); the first track has artwork if available. */
        val topArtists: List<TopItem>,
        val topTracks: List<TopItem>,
        /** The all-time top artist, with an image from their Last.fm page if there is one. */
        val topArtistAllTime: TopItem? = null
) {
    val topTrack: TopItem? get() = topTracks.firstOrNull()
}

/**
 * An album, track or artist (by [artist]), with its play count for the period and artwork if
 * Last.fm has some. [artwork], when set, is a Coil model to show instead of [imageUrl] (e.g. the
 * music server's artist picture).
 */
data class TopItem(val name: String, val artist: String?, val plays: Long, val imageUrl: String? = null, val artwork: Any? = null) {
    val image: Any? get() = artwork ?: imageUrl
}

/**
 * Reads the connected user's stats: user.getInfo, the top album, artists and tracks of the last 7
 * days, and the all-time top artist, in parallel. Top tracks and artists come without images, so
 * the first track's album art is looked up with track.getInfo, and the artist's picture comes from
 * [artistArtwork] (the music server), else the cover of their top album on Last.fm (both best
 * effort; Last.fm's own artist pages can't be scraped any more). Public data, so unsigned. Throws
 * [LastFmException].
 */
suspend fun LastFmAccount.loadStats(limit: Int = 5, artistArtwork: suspend (String) -> Any? = { null }): LastFmStats? {
    val user = user.value?.name ?: return null
    val params = mapOf("user" to user)
    val week = params + ("period" to "7day")
    val stats = coroutineScope {
        val info = async { api.call("user.getInfo", params) }
        val albums = async { api.call("user.getTopAlbums", week + ("limit" to "1")) }
        val artists = async { api.call("user.getTopArtists", week + ("limit" to limit.toString())) }
        val tracks = async { api.call("user.getTopTracks", week + ("limit" to limit.toString())) }
        val allTime = async { api.call("user.getTopArtists", params + mapOf("period" to "overall", "limit" to "1")) }
        val parsed = LastFmStatsParser.stats(info.await(), albums.await(), artists.await(), tracks.await(), limit)
        parsed.copy(topArtistAllTime = LastFmStatsParser.topArtists(allTime.await(), 1).firstOrNull())
    }
    return coroutineScope {
        val trackArt = async {
            val track = stats.topTrack ?: return@async null
            val artist = track.artist ?: return@async null
            try {
                LastFmStatsParser.trackArtwork(api.call("track.getInfo", mapOf("track" to track.name, "artist" to artist)))
            } catch (e: LastFmException) {
                null
            }
        }
        val artistImage = async {
            val name = stats.topArtistAllTime?.name ?: return@async null
            runCatching { artistArtwork(name) }.getOrNull() ?: try {
                LastFmStatsParser.topAlbumImage(api.call("artist.getTopAlbums", mapOf("artist" to name, "limit" to "1")))
            } catch (e: LastFmException) {
                null
            }
        }
        val track = stats.topTrack
        val art = trackArt.await()
        stats.copy(
                topTracks = if (track != null && art != null) listOf(track.copy(imageUrl = art)) + stats.topTracks.drop(1) else stats.topTracks,
                topArtistAllTime = stats.topArtistAllTime?.let { a ->
                    when (val image = artistImage.await()) {
                        null -> a
                        is String -> a.copy(imageUrl = image)
                        else -> a.copy(artwork = image)
                    }
                })
    }
}

/** Parses Last.fm's JSON (unit tested): numbers come as strings, and a one-item list as a bare object. */
object LastFmStatsParser {

    /** Last.fm's "no artwork" star image. */
    private const val PLACEHOLDER = "2a96cbd8b46e442fc41c2b86b821562f"

    fun stats(info: JsonObject, albums: JsonObject, artists: JsonObject, tracks: JsonObject, limit: Int = 5): LastFmStats {
        val user = info["user"]?.jsonObject ?: throw LastFmException(0, "Unexpected response from Last.fm")
        return LastFmStats(
                name = user.string("name") ?: "",
                realName = user.string("realname")?.takeIf { it.isNotBlank() },
                imageUrl = image(user["image"]),
                profileUrl = user.string("url"),
                scrobbles = user.long("playcount"),
                artists = user.long("artist_count"),
                tracks = user.long("track_count"),
                albums = user.long("album_count"),
                registeredSec = (user["registered"] as? JsonObject)?.long("unixtime")?.takeIf { it > 0 },
                topAlbum = list(albums["topalbums"], "album").firstNotNullOfOrNull { a -> topItem(a) },
                topArtists = topArtists(artists, limit),
                // Track images in top lists are placeholders; the first one's art is looked up separately.
                topTracks = list(tracks["toptracks"], "track").take(limit).mapNotNull { t -> topItem(t)?.copy(imageUrl = null) })
    }

    /** A user.getTopArtists response. Artist images from the API are all placeholders now, so none are kept. */
    fun topArtists(response: JsonObject, limit: Int): List<TopItem> =
            list(response["topartists"], "artist").take(limit).mapNotNull { a ->
                TopItem(a.string("name") ?: return@mapNotNull null, null, a.long("playcount"))
            }

    /** The cover of an artist.getTopAlbums response's first album, if it isn't Last.fm's placeholder. */
    fun topAlbumImage(response: JsonObject): String? =
            list(response["topalbums"], "album").firstNotNullOfOrNull { image(it["image"]) }

    /** The album art of a track.getInfo response, if it isn't Last.fm's placeholder. */
    fun trackArtwork(info: JsonObject): String? =
            ((info["track"] as? JsonObject)?.get("album") as? JsonObject)?.let { image(it["image"]) }

    private fun topItem(o: JsonObject): TopItem? = TopItem(o.string("name") ?: return null,
            (o["artist"] as? JsonObject)?.string("name"), o.long("playcount"), image(o["image"]))

    /** The largest real image URL from Last.fm's `image` array. */
    fun image(element: JsonElement?): String? = (element as? JsonArray)?.mapNotNull { (it as? JsonObject)?.string("#text") }
            ?.lastOrNull { it.isNotBlank() && !it.contains(PLACEHOLDER) }

    private fun list(container: JsonElement?, key: String): List<JsonObject> = when (val items = (container as? JsonObject)?.get(key)) {
        is JsonArray -> items.mapNotNull { it as? JsonObject }
        is JsonObject -> listOf(items)
        else -> emptyList()
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.long(key: String): Long = string(key)?.toLongOrNull() ?: 0
}
