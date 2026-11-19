package com.udeshcoffee.android.ui.compose.theme

import androidx.collection.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.hct.Hct
import com.udeshcoffee.android.utils.loadBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/*
 * Card tints: each album / artist / playlist card's text area takes its artwork's hue. Only the
 * hue (and a capped chroma) is kept, at a fixed tone, so text contrast is the same on every card.
 * Colours are cached in memory by key, so scrolling back costs nothing; images decode at 64px.
 */

private const val SEED_IMAGE_SIZE = 64
private const val MAX_CHROMA = 20.0
private const val LIGHT_TONE = 92.0
private const val DARK_TONE = 24.0

/** Artwork seed colours (ARGB) by card key. Misses aren't stored, so art that appears later is picked up. */
private val seedCache = LruCache<Any, Int>(500)

/**
 * The card colour for an artwork [seed]: its hue at a fixed tone (light pastel / deep muted), with
 * the chroma capped so busy grids stay calm (grey artwork stays nearly grey).
 */
fun cardTint(seed: Int, dark: Boolean): Color {
    val hct = Hct.fromInt(seed)
    return Color(Hct.from(hct.hue, min(hct.chroma, MAX_CHROMA), if (dark) DARK_TONE else LIGHT_TONE).toInt())
}

/**
 * The tinted card colour for the artwork [load] returns (a Coil model, loaded at [key]), or
 * [fallback] while loading / without artwork. Animates when the tint arrives.
 */
@Composable
fun rememberCardTint(key: Any?, fallback: Color, load: suspend () -> Any? = { key }): Color {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    var seed by remember(key) { mutableStateOf(key?.let { seedCache[it] }) }
    LaunchedEffect(key) {
        if (key == null || seed != null) return@LaunchedEffect
        val bitmap = load()?.let { loadBitmap(context, it, size = SEED_IMAGE_SIZE) } ?: return@LaunchedEffect
        val argb = withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).generate()
            (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb
        } ?: return@LaunchedEffect
        seedCache.put(key, argb)
        seed = argb
    }
    val target = seed?.let { cardTint(it, dark) } ?: fallback
    return animateColorAsState(target, label = "cardTint").value
}
