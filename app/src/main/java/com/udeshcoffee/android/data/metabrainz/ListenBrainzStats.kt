package com.udeshcoffee.android.data.metabrainz

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** A ListenBrainz user's listening stats, as shown on Home. */
data class ListenBrainzStats(
        val name: String,
        val profileUrl: String,
        /** All listens ever submitted. */
        val listens: Long,
        /** All-time distinct artists, albums (release groups) and tracks (recordings). */
        val artists: Long,
        val albums: Long,
        val tracks: Long,
        /** Last week's top album; null if there were no listens (or stats aren't calculated yet). */
        val topAlbum: TopItem?,
        /** Last week's top artists and tracks (up to 5 each). */
        val topArtists: List<TopItem>,
        val topTracks: List<TopItem>,
        /** The all-time top artist. */
        val topArtistAllTime: TopItem? = null
) {
    val topTrack: TopItem? get() = topTracks.firstOrNull()
}

/**
 * An album, track or artist (by [artist]), with its listen count for the period and artwork if
 * there is some: [imageUrl] (Cover Art Archive), or [artwork], a Coil model shown instead (e.g. the
 * music server's artist picture).
 */
data class TopItem(val name: String, val artist: String?, val plays: Long, val imageUrl: String? = null, val artwork: Any? = null) {
    val image: Any? get() = artwork ?: imageUrl
}

/**
 * Loads the user's stats from ListenBrainz: the listen count, all-time totals and top artist, and
 * last week's (`range=week`) top album, artists and tracks. Covers come from the Cover Art Archive;
 * the top artist's picture from [artistArtwork] (the music server), else the cover of their most
 * listened album. Calls run one after another (ListenBrainz asks clients not to burst). Public
 * data, but the token is sent for its higher rate limit. Throws [MetaBrainzException].
 */
suspend fun ListenBrainzAccount.loadStats(limit: Int = 5, artistArtwork: suspend (String) -> Any? = { null }): ListenBrainzStats? {
    val user = user.value ?: return null
    val token = token
    suspend fun stat(kind: String, range: String, count: Int) =
            ListenBrainzApi.get("stats/user/${user.name}/$kind", mapOf("range" to range, "count" to count.toString()), token)

    val listenCount = ListenBrainzApi.get("user/${user.name}/listen-count", token = token)
    val allArtists = stat("artists", "all_time", 1)
    // More than one, to find a cover for the top artist.
    val allAlbums = stat("release-groups", "all_time", 25)
    val allTracks = stat("recordings", "all_time", 1)
    val weekArtists = stat("artists", "week", limit)
    val weekAlbums = stat("release-groups", "week", 1)
    val weekTracks = stat("recordings", "week", limit)

    val stats = ListenBrainzStatsParser.stats(user, listenCount, allArtists, allAlbums, allTracks, weekArtists, weekAlbums, weekTracks)
    val top = stats.topArtistAllTime ?: return stats
    val server = runCatching { artistArtwork(top.name) }.getOrNull()
    val image = server ?: ListenBrainzStatsParser.coverByArtist(allAlbums, top.name)
    return when (image) {
        null -> stats
        is String -> stats.copy(topArtistAllTime = top.copy(imageUrl = image))
        else -> stats.copy(topArtistAllTime = top.copy(artwork = image))
    }
}

/** Parses ListenBrainz's stats JSON (unit tested). A null response (204) means stats aren't calculated yet. */
object ListenBrainzStatsParser {

    fun stats(user: ListenBrainzUser, listenCount: JsonObject?, allArtists: JsonObject?, allAlbums: JsonObject?, allTracks: JsonObject?,
              weekArtists: JsonObject?, weekAlbums: JsonObject?, weekTracks: JsonObject?): ListenBrainzStats = ListenBrainzStats(
            name = user.name,
            profileUrl = user.profileUrl,
            listens = listenCount.payload()?.long("count") ?: 0,
            artists = allArtists.payload()?.long("total_artist_count") ?: 0,
            albums = allAlbums.payload()?.long("total_release_group_count") ?: 0,
            tracks = allTracks.payload()?.long("total_recording_count") ?: 0,
            topAlbum = albums(weekAlbums).firstOrNull(),
            topArtists = artists(weekArtists),
            topTracks = tracks(weekTracks),
            topArtistAllTime = artists(allArtists).firstOrNull())

    fun artists(response: JsonObject?): List<TopItem> = response.payload().list("artists").mapNotNull { a ->
        TopItem(a.string("artist_name") ?: return@mapNotNull null, null, a.long("listen_count") ?: 0)
    }

    fun albums(response: JsonObject?): List<TopItem> = response.payload().list("release_groups").mapNotNull { g ->
        TopItem(g.string("release_group_name") ?: return@mapNotNull null, g.string("artist_name"), g.long("listen_count") ?: 0, cover(g))
    }

    fun tracks(response: JsonObject?): List<TopItem> = response.payload().list("recordings").mapNotNull { r ->
        TopItem(r.string("track_name") ?: return@mapNotNull null, r.string("artist_name"), r.long("listen_count") ?: 0,
                cover(r) ?: r.string("release_mbid")?.takeIf { it.isNotBlank() }?.let { MetaBrainz.releaseCover(it) })
    }

    /** The cover of the first album in [response] by [artist] (matched by name), if it has one. */
    fun coverByArtist(response: JsonObject?, artist: String): String? = response.payload().list("release_groups")
            .firstOrNull { it.string("artist_name").equals(artist, ignoreCase = true) && cover(it) != null }?.let(::cover)

    /** The Cover Art Archive image from `caa_release_mbid` and `caa_id`. */
    private fun cover(o: JsonObject): String? {
        val mbid = o.string("caa_release_mbid")?.takeIf { it.isNotBlank() } ?: return null
        val id = o.long("caa_id") ?: return null
        return MetaBrainz.caaImage(mbid, id)
    }

    private fun JsonObject?.payload(): JsonObject? = this?.get("payload") as? JsonObject
    private fun JsonObject?.list(key: String): List<JsonObject> = ((this?.get(key)) as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toLongOrNull() }
}
