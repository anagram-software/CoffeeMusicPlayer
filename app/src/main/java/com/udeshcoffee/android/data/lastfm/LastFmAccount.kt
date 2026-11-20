package com.udeshcoffee.android.data.lastfm

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import com.udeshcoffee.android.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The connected Last.fm account. */
data class LastFmUser(val name: String)

sealed interface LastFmConnectState {
    object Idle : LastFmConnectState
    /** The browser was opened; waiting for Last.fm to send the user back. */
    object WaitingForBrowser : LastFmConnectState
    /** Exchanging the token from the callback for a session. */
    object Connecting : LastFmConnectState
    data class Failed(val message: String) : LastFmConnectState
}

/**
 * The user's Last.fm connection, made with Last.fm's web auth: [startSignIn] gives the URL to open
 * in the browser, Last.fm redirects to [LastFmAuth.CALLBACK_URL] with a token, and
 * [handleCallback] exchanges it for a session key (auth.getSession), which never expires.
 *
 * Independent of the music source. Only the user name and session key are stored, in the `lastfm`
 * prefs file, which is excluded from backup. Features that act for the user (scrobbling, loving,
 * recommendations) call [api] with [sessionKey].
 */
class LastFmAccount(context: Context, val api: LastFmApi = LastFmApi(BuildConfig.LASTFM_API_KEY, BuildConfig.LASTFM_API_SECRET)) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _user = MutableStateFlow(prefs.getString(KEY_NAME, null)?.takeIf { sessionKey != null }?.let(::LastFmUser))
    /** The connected account, or null. */
    val user: StateFlow<LastFmUser?> = _user.asStateFlow()

    private val _connectState = MutableStateFlow<LastFmConnectState>(LastFmConnectState.Idle)
    val connectState: StateFlow<LastFmConnectState> = _connectState.asStateFlow()

    /** False when the build has no Last.fm API key or secret (see local.properties). */
    val isAvailable: Boolean get() = api.isConfigured

    val isConnected: Boolean get() = _user.value != null

    /** For signed calls on the user's behalf; null when not connected. */
    val sessionKey: String? get() = prefs.getString(KEY_SESSION, null)

    /** Marks a sign-in as started and returns the Last.fm page to open in the browser. */
    fun startSignIn(): Uri {
        prefs.edit { putLong(KEY_PENDING_SINCE, System.currentTimeMillis()) }
        _connectState.value = LastFmConnectState.WaitingForBrowser
        return LastFmAuth.authUrl(BuildConfig.LASTFM_API_KEY).toString().toUri()
    }

    /** The browser couldn't be opened, or the user came back without finishing. */
    fun cancelSignIn() {
        prefs.edit { remove(KEY_PENDING_SINCE) }
        _connectState.value = LastFmConnectState.Idle
    }

    /** True for the redirect from Last.fm's sign-in page. */
    fun isCallback(uri: Uri?): Boolean = uri?.scheme == LastFmAuth.CALLBACK_SCHEME && uri.host == LastFmAuth.CALLBACK_HOST

    /**
     * Finishes sign-in with the callback's token. Ignores callbacks the app didn't ask for (no
     * sign-in started in the last [PENDING_TIMEOUT_MS]), so another app can't link an account.
     */
    suspend fun handleCallback(uri: Uri) {
        val since = prefs.getLong(KEY_PENDING_SINCE, 0)
        if (since == 0L || System.currentTimeMillis() - since > PENDING_TIMEOUT_MS) {
            Log.w(TAG, "Ignoring a Last.fm callback without a pending sign-in")
            return
        }
        prefs.edit { remove(KEY_PENDING_SINCE) }
        val token = uri.getQueryParameter("token")
        if (token.isNullOrEmpty()) {
            _connectState.value = LastFmConnectState.Failed("Last.fm sign-in was cancelled")
            return
        }
        _connectState.value = LastFmConnectState.Connecting
        try {
            val session = api.call("auth.getSession", mapOf("token" to token), signed = true)["session"]?.jsonObject
                    ?: throw LastFmException(0, "Unexpected response from Last.fm")
            val name = session["name"]?.jsonPrimitive?.content
            val key = session["key"]?.jsonPrimitive?.content
            if (name.isNullOrEmpty() || key.isNullOrEmpty()) throw LastFmException(0, "Unexpected response from Last.fm")
            prefs.edit { putString(KEY_NAME, name); putString(KEY_SESSION, key) }
            _user.value = LastFmUser(name)
            _connectState.value = LastFmConnectState.Idle
        } catch (e: LastFmException) {
            Log.w(TAG, "Last.fm sign-in failed", e)
            _connectState.value = LastFmConnectState.Failed(
                    if (e.isAuthError) "Last.fm didn't accept the sign-in. Try again." else e.message ?: "Couldn't connect to Last.fm")
        }
    }

    /** Forgets the session. (Last.fm has no API to revoke it; the user can do that on last.fm.) */
    fun disconnect() {
        prefs.edit { clear() }
        _user.value = null
        _connectState.value = LastFmConnectState.Idle
    }

    fun clearError() {
        if (_connectState.value is LastFmConnectState.Failed) _connectState.value = LastFmConnectState.Idle
    }

    private companion object {
        const val TAG = "LastFmAccount"
        const val PREFS = "lastfm"
        const val KEY_NAME = "name"
        const val KEY_SESSION = "session_key"
        const val KEY_PENDING_SINCE = "pending_since"
        const val PENDING_TIMEOUT_MS = 15 * 60 * 1000L
    }
}
