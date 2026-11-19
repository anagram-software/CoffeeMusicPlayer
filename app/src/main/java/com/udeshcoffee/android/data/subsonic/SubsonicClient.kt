package com.udeshcoffee.android.data.subsonic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * What the app keeps after signing in: never the password, only a random salt and
 * md5(password + salt), as in the Subsonic API's token authentication.
 */
@Serializable
data class SubsonicCredentials(
        /** Server base URL without a trailing slash, e.g. https://music.example.com */
        val url: String,
        val username: String,
        val salt: String,
        val token: String
)

class SubsonicException(val code: Int, message: String) : Exception(message) {
    /** 40/41/44: wrong credentials or an unsupported auth method. */
    val isAuthError get() = code == 40 || code == 41 || code == 44
}

object SubsonicAuth {
    const val CLIENT_NAME = "CoffeeAndroid"
    const val API_VERSION = "1.16.1"

    fun normaliseUrl(input: String): String {
        var url = input.trim()
        if (!Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(url)) url = "https://$url"
        return url.trimEnd('/').removeSuffix("/rest").trimEnd('/')
    }

    fun token(password: String, salt: String): String =
            MessageDigest.getInstance("MD5").digest((password + salt).toByteArray()).toHex()

    fun newSalt(): String = ByteArray(8).also { SecureRandom().nextBytes(it) }.toHex()

    fun credentials(url: String, username: String, password: String): SubsonicCredentials {
        val salt = newSalt()
        return SubsonicCredentials(normaliseUrl(url), username.trim(), salt, token(password, salt))
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}

/** A thin Subsonic API client: authenticated URLs and JSON calls that throw [SubsonicException] on failure. */
class SubsonicClient(val credentials: SubsonicCredentials, private val http: () -> OkHttpClient) {

    /** An authenticated URL (for streams and cover art too). Treat it as sensitive: it carries the token. */
    fun url(method: String, params: List<Pair<String, Any?>> = emptyList()): HttpUrl {
        val builder = "${credentials.url}/rest/$method".toHttpUrl().newBuilder()
                .addQueryParameter("u", credentials.username)
                .addQueryParameter("t", credentials.token)
                .addQueryParameter("s", credentials.salt)
                .addQueryParameter("v", SubsonicAuth.API_VERSION)
                .addQueryParameter("c", SubsonicAuth.CLIENT_NAME)
                .addQueryParameter("f", "json")
        for ((key, value) in params) if (value != null) builder.addQueryParameter(key, value.toString())
        return builder.build()
    }

    /** Calls [method] and returns the `subsonic-response` object. */
    suspend fun call(method: String, params: List<Pair<String, Any?>> = emptyList()): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url(method, params)).build()
        val body = try {
            http().newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw SubsonicException(response.code, "Server responded with HTTP ${response.code}")
                response.body.string()
            }
        } catch (e: IOException) {
            throw SubsonicException(0, "Couldn't reach the server (${e.message ?: e.javaClass.simpleName})")
        } catch (e: IllegalArgumentException) {
            throw SubsonicException(0, "That doesn't look like a valid server address")
        }
        parse(body)
    }

    /** Calls [method] and decodes the response's [key] field (an empty object if missing). */
    suspend fun <T> get(method: String, key: String, serializer: KSerializer<T>, params: List<Pair<String, Any?>> = emptyList()): T {
        val response = call(method, params)
        return json.decodeFromJsonElement(serializer, response[key] ?: JsonObject(emptyMap()))
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true; explicitNulls = false }

        /** Unwraps `subsonic-response`, throwing for failed calls and non-Subsonic replies. */
        fun parse(body: String): JsonObject {
            val root = try {
                json.parseToJsonElement(body).jsonObject
            } catch (e: Exception) {
                throw SubsonicException(0, "That doesn't look like a Subsonic server")
            }
            val response = root["subsonic-response"]?.jsonObject ?: throw SubsonicException(0, "That doesn't look like a Subsonic server")
            if (response["status"]?.jsonPrimitive?.content == "failed") {
                val error = response["error"]?.jsonObject
                throw SubsonicException(error?.get("code")?.jsonPrimitive?.intOrNull ?: 0,
                        error?.get("message")?.jsonPrimitive?.content ?: "Request failed")
            }
            return response
        }
    }
}
