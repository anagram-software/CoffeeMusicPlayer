package com.udeshcoffee.android.utils

import androidx.core.net.toUri

/**
* Created by Udathari on 8/25/2017.
*/
val ArtworkURI = "content://media/external/audio/albumart".toUri()

object PreferenceUtil {
    const val LAST_PLAYED_POS = "lastplayed"
    const val LAST_CURRENT_POS = "lastpos"

    const val REPEAT_MODE = "repeatmode"
    const val SHUFFLE = "shuffle"
    const val LYRICS = "lyrics"
    const val LYRICS_SIZE = "lyricssize"

    const val DEFAULT_LYRICS = false

    const val  PREF_CALL = "pref_call"
    const val  PREF_FADE = "pref_fade"
    /** Look up lyrics the server doesn't have (and all device songs' lyrics) on LRCLIB. */
    const val  PREF_LRCLIB = "pref_lrclib"
    const val  DEFAULT_LRCLIB = true
    /** Show the playing track's format (codec, bit depth or bit rate, sample rate) below the player's artwork. */
    const val  PREF_SHOW_QUALITY = "pref_show_quality"
    const val  DEFAULT_SHOW_QUALITY = true
    /** Submit listens to the connected ListenBrainz account (default: off for a music server, which usually does it, on for device music). */
    const val  PREF_LISTENBRAINZ_SCROBBLE = "pref_listenbrainz_scrobble"
}
