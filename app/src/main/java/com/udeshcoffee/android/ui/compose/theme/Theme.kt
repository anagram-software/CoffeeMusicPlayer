package com.udeshcoffee.android.ui.compose.theme

import android.os.Build
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.ktx.toColor
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.DynamicMaterialTheme
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.utils.artworkData
import com.udeshcoffee.android.utils.loadBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CoffeeBrown = Color(0xFF8D5B3C)

private val FallbackDark = darkColorScheme(primary = Color(0xFFFFB68F), secondary = Color(0xFFE6BEAA), tertiary = Color(0xFFCDC991))
private val FallbackLight = lightColorScheme(primary = CoffeeBrown, secondary = Color(0xFF765849), tertiary = Color(0xFF656032))

/**
 * App theme. With a [seedColor] (taken from the playing song's artwork) the whole colour scheme
 * is generated from it; otherwise Material You wallpaper colours (Android 12+) or the app palette.
 */
@Composable
fun CoffeeTheme(seedColor: Color? = null, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    if (seedColor != null) {
        DynamicMaterialTheme(seedColor = seedColor, isDark = darkTheme, animate = true, typography = AppTypography, content = content)
        return
    }
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}

/** A seed colour extracted from [song]'s artwork, or null while loading / without artwork. */
@Composable
fun rememberArtworkSeed(song: Song?): Color? = rememberArtworkSeed(song?.artworkData())

/**
 * A seed colour extracted from an artwork model ([Song.artworkData], [com.udeshcoffee.android.model.Album.artworkData],
 * an image file...), or null while loading / without artwork. [data] (a value type) and [key] key the work.
 */
@Composable
fun rememberArtworkSeed(data: Any?, key: Any? = null): Color? {
    val context = LocalContext.current
    var seed by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(data, key) {
        if (data == null) {
            seed = null
            return@LaunchedEffect
        }
        val bitmap = loadBitmap(context, data, size = 128)
        seed = bitmap?.let {
            withContext(Dispatchers.Default) {
                val palette = Palette.from(it).generate()
                (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let { rgb -> Color(rgb) }
            }
        }
    }
    return seed
}

/**
 * A page in its artwork's colours (album, artist): the whole colour scheme, surfaces included, is
 * generated from [seed], as the player's is from the playing song. Without a seed (loading, no
 * artwork) the section's colours stay; changes animate.
 */
@Composable
fun ArtworkTint(seed: Color?, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val target = remember(seed, darkTheme) { seed?.let { artworkColorScheme(it, darkTheme) } } ?: base
    MaterialTheme(colorScheme = animateColorScheme(target), typography = AppTypography, content = content)
}

/**
 * A colour scheme from [seed] whose surfaces clearly carry its hue: the default style keeps them
 * nearly grey (neutral chroma ~6), so the neutral palettes get the seed's hue at a higher chroma.
 * Light backgrounds (tone 98) can hold almost no colour, so in light mode the page sits at tone 95.
 */
internal fun artworkColorScheme(seed: Color, darkTheme: Boolean): ColorScheme {
    val hue = Hct.fromInt(seed.toArgb()).hue
    val neutral = TonalPalette.fromHueAndChroma(hue, ARTWORK_NEUTRAL_CHROMA)
    val scheme = dynamicColorScheme(seed, darkTheme,
            neutral = neutral.keyColor.toColor(),
            neutralVariant = Hct.from(hue, ARTWORK_NEUTRAL_VARIANT_CHROMA, 50.0).toColor())
    if (darkTheme) return scheme
    val page = Color(neutral.tone(LIGHT_PAGE_TONE))
    return scheme.copy(background = page, surface = page)
}

private const val ARTWORK_NEUTRAL_CHROMA = 16.0
private const val ARTWORK_NEUTRAL_VARIANT_CHROMA = 20.0
private const val LIGHT_PAGE_TONE = 95
