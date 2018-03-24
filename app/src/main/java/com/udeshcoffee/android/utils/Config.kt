package com.udeshcoffee.android.utils

import androidx.net.toUri

/**
* Created by Udathari on 8/25/2017.
*/
val ArtworkURI = "content://media/external/audio/albumart".toUri()

object PreferenceUtil {
    val LAST_QUEUE = "queue"
    val LAST_PLAYED_POS = "lastplayed"
    val LAST_CURRENT_POS = "lastpos"

    val REPEAT_MODE = "repeatmode"
    val SHUFFLE = "shuffle"
    val LYRICS = "lyrics"
    val LYRICS_SIZE = "lyricssize"
    val HOME_INFO_DISMISSED = "homeinfodismissed"
    val LAST_VIEWED_WHATS_NEW = "lastviewedwhatsnew"

    val APP_START_HOME = 0
    val APP_START_LIB = 1

    val DEFAULT_LYRICS = false

    var PREF_APP_START = "pref_app_start"
    var PREF_LIB_START = "pref_lib_start"
    var PREF_CALL = "pref_call"
    var PREF_FADE = "pref_fade"
    var PREF_COLLECT_CONTENT = "pref_collect_content"
    var PREF_WIFI_ONLY = "pref_wifi_only"
    var PREF_COLLECT_LYRICS = "pref_collect_lyrics"
    var PREF_OPEN = "pref_open_source"
    var PREF_WHATS_NEW = "pref_whats_new"
    var PREF_VERSION = "pref_version"
}
