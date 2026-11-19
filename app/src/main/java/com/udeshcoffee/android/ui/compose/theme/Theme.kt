package com.udeshcoffee.android.ui.compose.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.materialkolor.dynamicColorScheme
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

/** The colour [ArtworkTint] washes behind a page's header (transparent outside one, or without artwork). */
val LocalArtworkWash = compositionLocalOf { Color.Transparent }

/**
 * A hint of the artwork's colour for a page (album, artist): accents (buttons, selection, chips)
 * from a scheme generated from [seed], over the section's neutral surfaces, plus [LocalArtworkWash]
 * for the gradient behind the header. Without a seed (loading, no artwork) the section's colours
 * stay; changes animate.
 */
@Composable
fun ArtworkTint(seed: Color?, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val target = remember(seed, darkTheme) { seed?.let { dynamicColorScheme(it, darkTheme) } } ?: base
    val spec = spring<Color>(stiffness = 400f)
    @Composable
    fun animated(pick: (ColorScheme) -> Color): Color = animateColorAsState(pick(target), spec, label = "artworkTint").value
    val scheme = base.copy(
            primary = animated { it.primary }, onPrimary = animated { it.onPrimary },
            primaryContainer = animated { it.primaryContainer }, onPrimaryContainer = animated { it.onPrimaryContainer },
            secondary = animated { it.secondary }, onSecondary = animated { it.onSecondary },
            secondaryContainer = animated { it.secondaryContainer }, onSecondaryContainer = animated { it.onSecondaryContainer },
            tertiary = animated { it.tertiary }, onTertiary = animated { it.onTertiary },
            tertiaryContainer = animated { it.tertiaryContainer }, onTertiaryContainer = animated { it.onTertiaryContainer },
            inversePrimary = animated { it.inversePrimary })
    val wash by animateColorAsState(if (seed == null) Color.Transparent else target.primaryContainer.copy(alpha = WASH_ALPHA), spec, label = "artworkWash")
    CompositionLocalProvider(LocalArtworkWash provides wash) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

private const val WASH_ALPHA = 0.55f
