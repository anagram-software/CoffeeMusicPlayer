package com.udeshcoffee.android.data.metabrainz

import android.util.Log
import com.udeshcoffee.android.data.remote.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/** Metadata found for a song, used by the tag editor. */
data class TrackMetadata(
        val title: String,
        val artist: String,
        val album: String?,
        val genre: String?,
        /** Year of the first release. */
        val year: Int?,
        val artworkUrl: String?
)

/**
 * Song metadata for the tag editor, by title and artist. With a ListenBrainz token, ListenBrainz's
 * metadata lookup (the mapping it uses for listens, which finds the canonical recording and its
 * original release) plus a MusicBrainz release lookup for the year, genre and cover. Without one,
 * or when it finds nothing, a MusicBrainz recording search, which is weaker for popular songs
 * (hundreds of live and compilation recordings tie with the original). Covers come from the Cover
 * Art Archive. Main-safe; returns null when nothing matches or on failure.
 */
object MusicBrainz {
    private const val TAG = "MusicBrainz"

    suspend fun lookupRecording(title: String, artist: String, listenBrainzToken: String? = null): TrackMetadata? {
        if (listenBrainzToken != null && artist.isNotBlank()) {
            val viaListenBrainz = try {
                val lookup = ListenBrainzApi.get("metadata/lookup/", mapOf("artist_name" to artist.trim(), "recording_name" to title.trim()),
                        listenBrainzToken)
                val releaseMbid = lookup?.let { MusicBrainzParser.lookupRelease(it) }
                if (lookup == null || releaseMbid == null) null
                else MusicBrainzParser.fromLookup(lookup, release(releaseMbid))
            } catch (e: MetaBrainzException) {
                Log.w(TAG, "ListenBrainz lookup failed: ${e.message}")
                null
            }
            if (viaListenBrainz != null) return viaListenBrainz
        }
        return searchRecording(title, artist)
    }

    /** A release with its release group (and the group's genres), or null. */
    private suspend fun release(mbid: String): JsonObject? =
            get("https://musicbrainz.org/ws/2/release/$mbid".toHttpUrl().newBuilder()
                    .addQueryParameter("inc", "release-groups genres").addQueryParameter("fmt", "json").build())

    private suspend fun get(url: okhttp3.HttpUrl): JsonObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("User-Agent", MetaBrainz.USER_AGENT).header("Accept", "application/json").build()
        try {
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} from MusicBrainz")
                    null
                } else MetaBrainz.json.parseToJsonElement(response.body.string()).jsonObject
            }
        } catch (e: Exception) {
            Log.w(TAG, "MusicBrainz request failed", e)
            null
        }
    }

    private suspend fun searchRecording(title: String, artist: String): TrackMetadata? {
        val url = "https://musicbrainz.org/ws/2/recording".toHttpUrl().newBuilder()
                .addQueryParameter("query", MusicBrainzParser.query(title, artist))
                .addQueryParameter("limit", "10")
                .addQueryParameter("fmt", "json")
                .build()
        return get(url)?.let { MusicBrainzParser.bestMatch(it) }
    }
}

/** Builds MusicBrainz searches and picks results (unit tested). */
object MusicBrainzParser {

    /** A Lucene query: the title, and the artist when given, as quoted phrases. */
    fun query(title: String, artist: String): String {
        fun phrase(s: String) = "\"" + s.trim().replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        return if (artist.isBlank()) "recording:${phrase(title)}" else "recording:${phrase(title)} AND artist:${phrase(artist)}"
    }

    /**
     * The best match of a recording search: the top-scored recording without a disambiguation note, on its original album
     * (an official album release, not a compilation or live one, earliest first, when there is one).
     */
    fun bestMatch(response: JsonObject): TrackMetadata? {
        val recordings = response.list("recordings")
        // Among the best-scored, skip ones MusicBrainz labels (live, demo, music video...).
        val best = recordings.maxOfOrNull { (it["score"] as? JsonPrimitive)?.longOrNull ?: 0 }
        val recording = recordings.firstOrNull { ((it["score"] as? JsonPrimitive)?.longOrNull ?: 0) == best && it.string("disambiguation").isNullOrBlank() }
                ?: recordings.firstOrNull() ?: return null
        val title = recording.string("title") ?: return null
        val artist = recording.list("artist-credit").joinToString("") { (it.string("name") ?: "") + (it.string("joinphrase") ?: "") }
                .ifBlank { return null }
        val release = recording.list("releases").sortedWith(compareBy<JsonObject>({ rank(it) }, { it.string("date")?.takeIf { d -> d.isNotBlank() } ?: "9999" }))
                .firstOrNull()
        val year = (recording.string("first-release-date") ?: release?.string("date"))?.take(4)?.toIntOrNull()
        val genre = recording.list("tags").maxByOrNull { (it["count"] as? JsonPrimitive)?.longOrNull ?: 0 }
                ?.string("name")?.replaceFirstChar { it.uppercase() }
        val releaseGroup = (release?.get("release-group") as? JsonObject)?.string("id")
        return TrackMetadata(title, artist, release?.string("title"), genre, year,
                releaseGroup?.let { "https://coverartarchive.org/release-group/$it/front-500" })
    }

    /** The release a ListenBrainz metadata lookup matched (null when it found nothing). */
    fun lookupRelease(lookup: JsonObject): String? = lookup.string("release_mbid")?.takeIf { it.isNotBlank() }

    /**
     * Metadata from a ListenBrainz metadata lookup and the matched MusicBrainz release (with its
     * release group): the year of the group's first release, its most-voted genre and its cover.
     */
    fun fromLookup(lookup: JsonObject, release: JsonObject?): TrackMetadata? {
        val title = lookup.string("recording_name")?.takeIf { it.isNotBlank() } ?: return null
        val artist = lookup.string("artist_credit_name")?.takeIf { it.isNotBlank() } ?: return null
        val group = release?.get("release-group") as? JsonObject
        val year = (group?.string("first-release-date") ?: release?.string("date"))?.take(4)?.toIntOrNull()
        val genre = (group?.list("genres").orEmpty() + release?.list("genres").orEmpty())
                .maxByOrNull { (it["count"] as? JsonPrimitive)?.longOrNull ?: 0 }?.string("name")?.replaceFirstChar { it.uppercase() }
        val cover = group?.string("id")?.let { "https://coverartarchive.org/release-group/$it/front-500" }
                ?: lookupRelease(lookup)?.let { MetaBrainz.releaseCover(it) }
        return TrackMetadata(title, artist, lookup.string("release_name") ?: release?.string("title"), genre, year, cover)
    }

    /** Lower is better: official albums, then other official releases, then the rest. */
    private fun rank(release: JsonObject): Int {
        val group = release["release-group"] as? JsonObject
        val album = group?.string("primary-type") == "Album" && (group["secondary-types"] as? JsonArray).isNullOrEmpty()
        val official = release.string("status") == "Official"
        return when {
            official && album -> 0
            official -> 1
            else -> 2
        }
    }

    private fun JsonObject.list(key: String): List<JsonObject> = (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
