package com.udeshcoffee.android.ui.compose.theme

import android.os.Build
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
import com.udeshcoffee.android.utils.AlbumArt
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
        DynamicMaterialTheme(seedColor = seedColor, isDark = darkTheme, animate = true, content = content)
        return
    }
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDark
        else -> FallbackLight
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** A seed colour extracted from [song]'s artwork, or null while loading / without artwork. */
@Composable
fun rememberArtworkSeed(song: Song?): Color? {
    val context = LocalContext.current
    var seed by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(song?.albumId, song?.dateModified) {
        if (song == null) {
            seed = null
            return@LaunchedEffect
        }
        val bitmap = loadBitmap(context, AlbumArt(song.albumId, song.dateModified), size = 128)
        seed = bitmap?.let {
            withContext(Dispatchers.Default) {
                val palette = Palette.from(it).generate()
                (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let { rgb -> Color(rgb) }
            }
        }
    }
    return seed
}
