package com.udeshcoffee.android.data.metabrainz

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** The connected ListenBrainz account. */
data class ListenBrainzUser(val name: String) {
    val profileUrl: String get() = "https://listenbrainz.org/user/$name/"
}

/**
 * The user's ListenBrainz connection: their user token (from listenbrainz.org/settings, pasted in
 * Settings), checked with `validate-token`, which also gives the user name. ListenBrainz's API
 * only accepts these tokens (MusicBrainz OAuth has no scope for listens). Independent of the
 * music source. The token and name are stored in the `listenbrainz` prefs file, which is excluded
 * from backup.
 */
class ListenBrainzAccount(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _user = MutableStateFlow(prefs.getString(KEY_NAME, null)?.takeIf { token != null }?.let(::ListenBrainzUser))
    /** The connected account, or null. */
    val user: StateFlow<ListenBrainzUser?> = _user.asStateFlow()

    val isConnected: Boolean get() = _user.value != null

    /** The user token, for authenticated calls; null when not connected. */
    val token: String? get() = prefs.getString(KEY_TOKEN, null)

    /**
     * Checks [token] with ListenBrainz and connects with it. Throws [MetaBrainzException]: status
     * 401 for a token ListenBrainz doesn't accept.
     */
    suspend fun connect(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) throw MetaBrainzException(401, "Enter your ListenBrainz user token")
        val response = ListenBrainzApi.get("validate-token", token = trimmed)
                ?: throw MetaBrainzException(0, "Unexpected response from ListenBrainz")
        val valid = (response["valid"] as? JsonPrimitive)?.booleanOrNull == true
        val name = (response["user_name"] as? JsonPrimitive)?.contentOrNull
        if (!valid || name.isNullOrEmpty()) throw MetaBrainzException(401, "ListenBrainz didn't accept this token")
        prefs.edit(commit = true) { putString(KEY_TOKEN, trimmed); putString(KEY_NAME, name) }
        _user.value = ListenBrainzUser(name)
    }

    /** Forgets the token (and any listens still waiting to be sent). */
    fun disconnect() {
        prefs.edit { clear() }
        _user.value = null
    }

    private companion object {
        const val PREFS = "listenbrainz"
        const val KEY_NAME = "user_name"
        const val KEY_TOKEN = "token"
    }
}
