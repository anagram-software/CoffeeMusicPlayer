package com.udeshcoffee.android.playback

import com.udeshcoffee.android.service.AudioFXHelper

/** Gives the equalizer screen access to the effects attached to the running player. */
object AudioEffects {
    @Volatile
    var helper: AudioFXHelper? = null
        internal set
}
