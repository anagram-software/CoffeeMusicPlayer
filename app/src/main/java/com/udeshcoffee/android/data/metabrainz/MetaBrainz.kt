package com.udeshcoffee.android.data.metabrainz

import com.udeshcoffee.android.BuildConfig
import com.udeshcoffee.android.data.remote.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Shared bits for the MetaBrainz services (ListenBrainz, MusicBrainz, Cover Art Archive). They
 * require a User-Agent naming the app and a contact URL, or they may block requests.
 */
object MetaBrainz {
    const val CONTACT_URL = "https://github.com/anagram-software/CoffeeMusicPlayer"
    val USER_AGENT = "CoffeeMusicPlayer/${BuildConfig.VERSION_NAME} ( $CONTACT_URL )"
    const val CLIENT_NAME = "Coffee Music Player"

    internal val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** Cover Art Archive front cover of a release (redirects to the image; 404 if there's none). */
    fun releaseCover(releaseMbid: String, size: Int = 500): String = "https://coverartarchive.org/release/$releaseMbid/front-$size"

    /** A specific Cover Art Archive image, as ListenBrainz gives them (`caa_release_mbid`, `caa_id`). */
    fun caaImage(releaseMbid: String, caaId: Long, size: Int = 500): String =
            "https://coverartarchive.org/release/$releaseMbid/$caaId-$size.jpg"
}

/** A ListenBrainz / MusicBrainz error: [status] is the HTTP status (0 for network or parse failures). */
class MetaBrainzException(val status: Int, message: String) : Exception(message) {
    /** The token is wrong or was revoked. */
    val isAuthError get() = status == 401
    /** Offline, rate limited or the service is down: worth retrying later. */
    val isRetryable get() = status == 0 || status == 429 || status >= 500
}

/**
 * The ListenBrainz API (api.listenbrainz.org), authenticated with the user's token
 * (`Authorization: Token …`) where given. Main-safe; throws [MetaBrainzException]. Returns null
 * for 204 (e.g. stats not calculated yet).
 */
object ListenBrainzApi {
    private const val BASE_URL = "https://api.listenbrainz.org/1/"
    private val JSON_TYPE = "application/json".toMediaType()

    suspend fun get(path: String, params: Map<String, String> = emptyMap(), token: String? = null): JsonObject? {
        val url = (BASE_URL + path).toHttpUrl().newBuilder().apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        return execute(Request.Builder().url(url), token)
    }

    suspend fun post(path: String, body: JsonObject, token: String): JsonObject? =
            execute(Request.Builder().url(BASE_URL + path).post(body.toString().toRequestBody(JSON_TYPE)), token)

    private suspend fun execute(builder: Request.Builder, token: String?): JsonObject? = withContext(Dispatchers.IO) {
        builder.header("User-Agent", MetaBrainz.USER_AGENT)
        if (token != null) builder.header("Authorization", "Token $token")
        val (code, body) = try {
            Http.client.newCall(builder.build()).execute().use { it.code to it.body.string() }
        } catch (e: IOException) {
            throw MetaBrainzException(0, "Couldn't reach ListenBrainz")
        }
        if (code == 204) return@withContext null
        val obj = runCatching { MetaBrainz.json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (code !in 200..299) {
            val message = (obj?.get("error") as? JsonPrimitive)?.contentOrNull ?: "ListenBrainz error $code"
            throw MetaBrainzException(code, message)
        }
        obj ?: throw MetaBrainzException(0, "Unexpected response from ListenBrainz")
    }
}
