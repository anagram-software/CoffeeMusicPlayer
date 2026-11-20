package com.udeshcoffee.android.data.lastfm

import com.udeshcoffee.android.data.remote.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest

/**
 * A Last.fm API error. [code] is Last.fm's error number (0 for network or parse failures);
 * see https://www.last.fm/api/errorcodes.
 */
class LastFmException(val code: Int, message: String) : Exception(message) {
    /** 4 invalid auth, 9 invalid session key, 14 unauthorised token, 15 expired token. */
    val isAuthError get() = code == 4 || code == 9 || code == 14 || code == 15
}

/** Pure helpers for Last.fm's authentication (unit tested). */
object LastFmAuth {
    /** Where Last.fm sends the browser back after sign-in; matched by an intent filter on MainActivity. */
    const val CALLBACK_SCHEME = "coffeeplayer"
    const val CALLBACK_HOST = "lastfm-auth"
    const val CALLBACK_URL = "$CALLBACK_SCHEME://$CALLBACK_HOST"

    /** The page the user signs in on: https://www.last.fm/api/webauth */
    fun authUrl(apiKey: String, callback: String = CALLBACK_URL): HttpUrl = "https://www.last.fm/api/auth/".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("cb", callback)
            .build()

    /**
     * `api_sig`: md5 of every parameter as name + value, sorted by name, followed by the shared
     * secret. `format` and `callback` are not signed.
     */
    fun signature(params: Map<String, String>, secret: String): String {
        val text = params.filterKeys { it != "format" && it != "callback" }.toSortedMap()
                .entries.joinToString("") { (k, v) -> k + v } + secret
        return MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Calls ws.audioscrobbler.com. Signed calls (auth and anything for the signed-in user) carry
 * `api_sig`; [sessionKey] adds `sk`. Main-safe; throws [LastFmException].
 */
class LastFmApi(private val apiKey: String, private val secret: String) {

    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = apiKey.isNotEmpty() && secret.isNotEmpty()

    /**
     * Calls [method] and returns the response object. Write methods (scrobbling, loving) must use
     * [post]. Unsigned calls are for public data.
     */
    suspend fun call(method: String, params: Map<String, String> = emptyMap(), sessionKey: String? = null,
                     signed: Boolean = sessionKey != null, post: Boolean = false): JsonObject = withContext(Dispatchers.IO) {
        val all = buildMap {
            putAll(params)
            put("method", method)
            put("api_key", apiKey)
            if (sessionKey != null) put("sk", sessionKey)
        }
        val withSig = if (signed) all + ("api_sig" to LastFmAuth.signature(all, secret)) else all
        val request = if (post) {
            val form = FormBody.Builder().apply { withSig.forEach { (k, v) -> add(k, v) }; add("format", "json") }.build()
            Request.Builder().url(BASE_URL).post(form)
        } else {
            val url = BASE_URL.toHttpUrl().newBuilder().apply { withSig.forEach { (k, v) -> addQueryParameter(k, v) } }
                    .addQueryParameter("format", "json").build()
            Request.Builder().url(url)
        }.header("User-Agent", USER_AGENT).build()

        val body = try {
            // Last.fm answers errors with HTTP 4xx and a JSON body, so read the body either way.
            Http.client.newCall(request).execute().use { it.body.string() }
        } catch (e: IOException) {
            throw LastFmException(0, "Couldn't reach Last.fm")
        }
        val obj = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw LastFmException(0, "Unexpected response from Last.fm")
        }
        obj["error"]?.jsonPrimitive?.intOrNull?.let { code ->
            throw LastFmException(code, obj["message"]?.jsonPrimitive?.content ?: "Last.fm error $code")
        }
        obj
    }

    private companion object {
        const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
        const val USER_AGENT = "CoffeeMusicPlayer"
    }
}
