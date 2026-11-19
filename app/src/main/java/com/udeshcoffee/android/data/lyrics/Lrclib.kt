package com.udeshcoffee.android.data.lyrics

import com.udeshcoffee.android.BuildConfig
import com.udeshcoffee.android.data.remote.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * LRCLIB (lrclib.net), a free lyrics database: the fallback when the music server has no lyrics,
 * or only unsynced ones, and the only source for device songs. Sends the song's title, artist,
 * album and duration; the user can turn it off in Settings ([com.udeshcoffee.android.utils.PreferenceUtil.PREF_LRCLIB]).
 */
object Lrclib {

    private const val URL = "https://lrclib.net/api/get"
    // LRCLIB asks clients to identify themselves.
    private val userAgent = "CoffeeMusicPlayer/${BuildConfig.VERSION_NAME} (Android)"
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    @Serializable
    private data class Response(val syncedLyrics: String? = null, val plainLyrics: String? = null, val instrumental: Boolean = false)

    /**
     * Lyrics for a track, or null when LRCLIB doesn't have them. Main-safe.
     * @throws IOException when the request fails (network, or an unexpected HTTP status)
     */
    suspend fun lyrics(title: String, artist: String, album: String?, durationMs: Long): Lyrics? = withContext(Dispatchers.IO) {
        if (title.isBlank() || artist.isBlank()) return@withContext null
        val url = URL.toHttpUrl().newBuilder()
                .addQueryParameter("track_name", title)
                .addQueryParameter("artist_name", artist)
                .apply {
                    if (!album.isNullOrBlank()) addQueryParameter("album_name", album)
                    if (durationMs > 0) addQueryParameter("duration", Math.round(durationMs / 1000.0).toString())
                }
                .build()
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        Http.client.newCall(request).execute().use { res ->
            if (res.code == 404) return@withContext null
            if (!res.isSuccessful) throw IOException("LRCLIB responded with HTTP ${res.code}")
            val body = try {
                json.decodeFromString(Response.serializer(), res.body.string())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw IOException("Unexpected LRCLIB response", e)
            }
            val text = body.syncedLyrics?.takeIf { it.isNotBlank() } ?: body.plainLyrics?.takeIf { it.isNotBlank() }
            text?.let(::parseLrc)?.takeIf { l -> l.lines.any { it.text.isNotBlank() } }
        }
    }
}
