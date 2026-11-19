package com.udeshcoffee.android.ui.compose.theme

import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTintTest {

    private fun hct(argb: Int) = Hct.fromInt(argb)

    @Test
    fun `keeps the hue at a fixed tone`() {
        val red = 0xFFD0503F.toInt()
        val light = hct(cardTint(red, dark = false).toArgb())
        val dark = hct(cardTint(red, dark = true).toArgb())
        assertEquals(hct(red).hue, light.hue, 5.0)
        assertEquals(hct(red).hue, dark.hue, 5.0)
        assertEquals(92.0, light.tone, 1.0)
        assertEquals(24.0, dark.tone, 1.0)
    }

    @Test
    fun `caps the chroma so cards stay calm`() {
        val vivid = 0xFF00FF40.toInt()
        assertTrue(hct(cardTint(vivid, dark = true).toArgb()).chroma <= 21.0)
    }

    @Test
    fun `grey artwork stays grey`() {
        val grey = 0xFF808080.toInt()
        // HCT gives neutral grey a chroma of ~2; anything under ~4 still reads as grey.
        assertTrue(hct(cardTint(grey, dark = false).toArgb()).chroma < 4.0)
    }
}
