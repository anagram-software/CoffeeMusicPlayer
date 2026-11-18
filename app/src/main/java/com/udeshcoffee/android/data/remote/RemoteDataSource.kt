package com.udeshcoffee.android.data.remote

import android.util.Log
import com.udeshcoffee.android.BuildConfig
import com.udeshcoffee.android.data.remote.genius.GeniusService
import com.udeshcoffee.android.data.remote.genius.Hit
import com.udeshcoffee.android.data.remote.lastfm.ImageArtist
import com.udeshcoffee.android.data.remote.lastfm.LastFmService
import com.udeshcoffee.android.data.remote.lastfm.TrackMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.text.Normalizer
import java.util.concurrent.TimeUnit

/**
 * Network sources: Genius (lyrics search + page scraping) and Last.fm (artist info and track
 * metadata). All functions are main-safe and return null/empty on failure.
 */
object RemoteDataSource {

    private const val GENIUS_BASE_URL = "https://api.genius.com/"
    private const val LASTFM_BASE_URL = "https://ws.audioscrobbler.com/"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    /** Last.fm's placeholder image ("no artwork"). */
    private const val LASTFM_PLACEHOLDER = "2a96cbd8b46e442fc41c2b86b821562f"
    private const val TAG = "RemoteDataSource"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    /** One client (connection pool, TLS setup) shared by every service; built off the main thread. */
    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    }

    private fun client(params: Map<String, String>): OkHttpClient = baseClient.newBuilder()
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.newBuilder().apply {
                    params.forEach { (k, v) -> addQueryParameter(k, v) }
                }.build()
                chain.proceed(original.newBuilder().url(url).header("User-Agent", "CoffeeMusicPlayer").build())
            }
            .build()

    private fun retrofit(baseUrl: String, params: Map<String, String>): Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client(params))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    private val geniusService: GeniusService by lazy {
        retrofit(GENIUS_BASE_URL, mapOf("access_token" to BuildConfig.GENIUS_ACCESS_TOKEN)).create(GeniusService::class.java)
    }

    private val lastFmService: LastFmService by lazy {
        retrofit(LASTFM_BASE_URL, mapOf("api_key" to BuildConfig.LASTFM_API_KEY, "format" to "json")).create(LastFmService::class.java)
    }

    // Services are created lazily on first use; do that on the IO dispatcher (TLS setup does disk reads).
    private suspend fun genius(): GeniusService = withContext(Dispatchers.IO) { geniusService }
    private suspend fun lastFm(): LastFmService = withContext(Dispatchers.IO) { lastFmService }

    // ---- Genius ----

    /** Genius hits by the given artist; only exact title matches when [exactTitle] and any exist. */
    suspend fun searchGenius(title: String, artist: String, exactTitle: Boolean): List<Hit> {
        val hits = try {
            genius().search(title).response?.hits ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Genius search failed", e)
            return emptyList()
        }
        val byArtist = hits.filter { it.result.primaryArtist.name.contains(artist, ignoreCase = true) }
        val exact = byArtist.filter { it.result.titleWithFeatured.equals(title, ignoreCase = true) }
        return if (exactTitle && exact.isNotEmpty()) exact else byArtist
    }

    /**
     * Scrapes lyrics from a Genius song page ([path] like "/Artist-song-lyrics").
     * Returns HTML with `<br/>` line breaks and highlighted section headers, or null.
     */
    suspend fun loadGeniusLyrics(path: String): String? = withContext(Dispatchers.IO) {
        try {
            extractLyrics(fetchPage("https://genius.com$path") ?: return@withContext null)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load lyrics page", e)
            null
        }
    }

    /** Fetches a web page with browser-like headers; null on any HTTP error. */
    private fun fetchPage(url: String): String? {
        val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
        baseClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP ${response.code} for $url")
                return null
            }
            return response.body.string()
        }
    }

    /** Lyrics HTML from a Genius song page, or null if the page has none. */
    internal fun extractLyrics(pageHtml: String): String? {
        val page = Jsoup.parse(pageHtml).apply { outputSettings().prettyPrint(false) }
        // Current markup: one or more lyrics containers; older pages used a ".lyrics" div.
        val containers = page.select("div[data-lyrics-container=true]").ifEmpty { page.select(".lyrics") }
        if (containers.isEmpty()) return null
        return formatLyrics(containers.flatMap { textLines(it) }).takeIf { it.isNotBlank() }
    }

    private fun textLines(container: Element): List<String> {
        val copy = container.clone()
        // Genius puts the translations/contributors header inside the container, marked like this.
        copy.select("[data-exclude-from-selection=true]").remove()
        val html = copy.html().replace(Regex("(?i)<br\\s*/?>"), "\n")
        val text = Jsoup.clean(html, "", Safelist.none(), Document.OutputSettings().prettyPrint(false))
        return text.lines().map { org.jsoup.parser.Parser.unescapeEntities(it, false).trim() }
    }

    private fun formatLyrics(lines: List<String>): String {
        val header = Regex("^\\[.+]$")
        val builder = StringBuilder()
        for (line in lines.dropLastWhile { it.isEmpty() }) {
            if (line.isEmpty() && builder.isEmpty()) continue
            if (header.matches(line)) {
                builder.append("<font color=#1976D2><b>").append(line.trim('[', ']')).append("</b></font>")
            } else {
                builder.append(line.replace("\\P{Print}".toRegex(), ""))
            }
            builder.append("<br/>")
        }
        return Normalizer.normalize(builder.toString().removeSuffix("<br/>"), Normalizer.Form.NFC)
    }

    // ---- Last.fm ----

    /** Artist bio/tags plus an image scraped from the artist's Last.fm page (the API no longer serves images). */
    suspend fun searchLastFMArtist(artist: String): ImageArtist? {
        val info = try {
            lastFm().getArtistInfo(artist).artist
        } catch (e: Exception) {
            Log.w(TAG, "Last.fm artist lookup failed", e)
            null
        } ?: return null
        return ImageArtist(info, loadLastFmArtistImage(info.url) ?: "")
    }

    private suspend fun loadLastFmArtistImage(url: String): String? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null
        try {
            val page = Jsoup.parse(fetchPage(url) ?: return@withContext null, url)
            page.selectFirst("meta[property=og:image]")?.attr("content")
                    ?.takeIf { it.isNotEmpty() && !it.contains(LASTFM_PLACEHOLDER) }
                    ?: page.selectFirst(".header-new-background-image")?.attr("content")?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Track metadata for the tag editor. Tries an exact track.getInfo lookup (with Last.fm's
     * autocorrect) and falls back to track.search for the best match.
     */
    suspend fun searchTrackMetadata(title: String, artist: String): TrackMetadata? {
        try {
            // "Not found" can come back as an error body or an HTTP error; either way, fall back to search.
            var info = runCatching { lastFm().getTrackInfo(title, artist).track }.getOrNull()
            if (info == null) {
                val match = lastFm().searchTrack(title, artist.ifBlank { null }).results?.trackMatches?.track?.firstOrNull()
                        ?: return null
                info = lastFm().getTrackInfo(match.name, match.artist).track ?: return null
            }
            val artwork = info.album?.image
                    ?.lastOrNull { it.url.isNotEmpty() && !it.url.contains(LASTFM_PLACEHOLDER) }?.url
            return TrackMetadata(
                    title = info.name,
                    artist = info.artist?.name ?: artist,
                    album = info.album?.title?.takeIf { it.isNotEmpty() },
                    genre = info.topTags?.tag?.firstOrNull()?.name?.replaceFirstChar { it.titlecase() },
                    artworkUrl = artwork
            )
        } catch (e: Exception) {
            Log.w(TAG, "Last.fm track lookup failed", e)
            return null
        }
    }
}
