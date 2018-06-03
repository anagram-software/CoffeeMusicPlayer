package com.udeshcoffee.android.utils

import androidx.core.net.toUri

/**
* Created by Udathari on 8/25/2017.
*/
val ArtworkURI = "content://media/external/audio/albumart".toUri()

object PreferenceUtil {
    const val LAST_QUEUE = "queue"
    const val LAST_PLAYED_POS = "lastplayed"
    const val LAST_CURRENT_POS = "lastpos"

    const val REPEAT_MODE = "repeatmode"
    const val SHUFFLE = "shuffle"
    const val LYRICS = "lyrics"
    const val LYRICS_SIZE = "lyricssize"

    const val DEFAULT_LYRICS = false

    const val  PREF_LIB_START = "pref_lib_start"
    const val  PREF_NEW_PLAYER = "pref_new_player"
    const val  PREF_CALL = "pref_call"
    const val  PREF_FADE = "pref_fade"
    const val  PREF_COLLECT_CONTENT = "pref_collect_content"
    const val  PREF_WIFI_ONLY = "pref_wifi_only"
    const val  PREF_COLLECT_LYRICS = "pref_collect_lyrics"
    const val  PREF_OPEN = "pref_open_source"
    const val  PREF_WHATS_NEW = "pref_whats_new"
    const val  PREF_VERSION = "pref_version"
}
