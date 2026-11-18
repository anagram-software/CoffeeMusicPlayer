package com.udeshcoffee.android.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatModesTest {

    @Test
    fun `stored preference values map to player modes and back`() {
        assertEquals(Player.REPEAT_MODE_ONE, RepeatModes.fromPreference(RepeatModes.PREF_ONE))
        assertEquals(Player.REPEAT_MODE_ALL, RepeatModes.fromPreference(RepeatModes.PREF_ALL))
        assertEquals(Player.REPEAT_MODE_OFF, RepeatModes.fromPreference(RepeatModes.PREF_NONE))
        for (pref in listOf(RepeatModes.PREF_ONE, RepeatModes.PREF_ALL, RepeatModes.PREF_NONE))
            assertEquals(pref, RepeatModes.toPreference(RepeatModes.fromPreference(pref)))
    }

    @Test
    fun `unknown preference values fall back to repeat all`() {
        assertEquals(Player.REPEAT_MODE_ALL, RepeatModes.fromPreference(0))
    }
}
