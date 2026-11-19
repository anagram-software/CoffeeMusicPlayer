package com.udeshcoffee.android.ui.compose.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp

/**
 * App sections and their accent colours, as in the original app and the web player
 * (`CoffeeMusicPlayerWeb/src/index.css`, `data-section`). [accent] fills pills and buttons;
 * [ink] is a deeper shade for accent-coloured text on light backgrounds.
 */
enum class Section(val accent: Color, val onAccent: Color, ink: Color? = null) {
    HOME(Color(0xFFFF5252), Color.White),
    FAVORITES(Color(0xFFFF4081), Color.White),
    LIBRARY(Color(0xFF448AFF), Color.White),
    SEARCH(Color(0xFF448AFF), Color.White),
    PLAYLISTS(Color(0xFF448AFF), Color.White),
    SETTINGS(Color(0xFFFF6E40), Color.White),
    EDIT(Color(0xFF1DE9B6), Color(0xFF1A1A1A), ink = Color(0xFF00897B));

    /** Accent for text and icons on light surfaces (80% accent, 20% black unless set). */
    val lightInk: Color = ink ?: lerp(accent, Color.Black, 0.2f)
}

/** The section whose accent the current screen uses (for pills drawn with the original colours). */
val LocalSection = staticCompositionLocalOf { Section.HOME }

/**
 * Theme for the app's pages: Coffee's neutral surfaces (#181818 dark, #FAFAFA light) with the
 * [section]'s accent as primary, so buttons, tabs, switches and selection follow the page. The
 * accent animates between sections. The player surfaces use [CoffeeTheme] with the artwork colours
 * instead.
 */
@Composable
fun SectionTheme(section: Section, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val spec = spring<Color>(stiffness = 400f)
    // Dark: full-strength accents. Light: deeper ink for text-coloured parts, which also fill buttons.
    val primary by animateColorAsState(if (darkTheme) section.accent else section.lightInk, spec, label = "sectionPrimary")
    val onPrimary by animateColorAsState(if (darkTheme) section.onAccent else Color.White, spec, label = "sectionOnPrimary")
    val accent by animateColorAsState(section.accent, spec, label = "sectionAccent")
    val scheme = if (darkTheme) darkSectionScheme(primary, onPrimary, accent) else lightSectionScheme(primary, onPrimary, accent)
    CompositionLocalProvider(LocalSection provides section) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

private fun darkSectionScheme(primary: Color, onPrimary: Color, accent: Color): ColorScheme {
    val surface = Color(0xFF181818)
    val onSurface = Color(0xFFF7F7F7)
    return darkColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = accent.copy(alpha = 0.30f).compositeOver(surface), onPrimaryContainer = onSurface,
            secondary = primary, onSecondary = onPrimary,
            secondaryContainer = accent.copy(alpha = 0.22f).compositeOver(surface), onSecondaryContainer = onSurface,
            tertiary = primary, onTertiary = onPrimary,
            tertiaryContainer = accent.copy(alpha = 0.22f).compositeOver(surface), onTertiaryContainer = onSurface,
            background = surface, onBackground = onSurface,
            surface = surface, onSurface = onSurface,
            surfaceVariant = Color(0xFF2A2A2A), onSurfaceVariant = Color(0xFFAAAAAA),
            surfaceTint = onSurface,
            surfaceDim = Color(0xFF121212), surfaceBright = Color(0xFF333333),
            surfaceContainerLowest = Color(0xFF121212), surfaceContainerLow = Color(0xFF1C1C1C),
            surfaceContainer = Color(0xFF212121), surfaceContainerHigh = Color(0xFF262626),
            surfaceContainerHighest = Color(0xFF2E2E2E),
            inverseSurface = onSurface, inverseOnSurface = surface, inversePrimary = primary,
            outline = Color(0xFF5C5C5C), outlineVariant = Color(0xFF333333),
            error = Color(0xFFFF5252), onError = Color.White)
}

private fun lightSectionScheme(primary: Color, onPrimary: Color, accent: Color): ColorScheme {
    val surface = Color(0xFFFAFAFA)
    val onSurface = Color(0xFF1A1A1A)
    return lightColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = accent.copy(alpha = 0.22f).compositeOver(surface), onPrimaryContainer = onSurface,
            secondary = primary, onSecondary = onPrimary,
            secondaryContainer = accent.copy(alpha = 0.16f).compositeOver(surface), onSecondaryContainer = onSurface,
            tertiary = primary, onTertiary = onPrimary,
            tertiaryContainer = accent.copy(alpha = 0.16f).compositeOver(surface), onTertiaryContainer = onSurface,
            background = surface, onBackground = onSurface,
            surface = surface, onSurface = onSurface,
            surfaceVariant = Color(0xFFEEEEEE), onSurfaceVariant = Color(0xFF666666),
            surfaceTint = onSurface,
            surfaceDim = Color(0xFFE6E6E6), surfaceBright = Color.White,
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF5F5F5),
            surfaceContainer = Color(0xFFF2F2F2), surfaceContainerHigh = Color(0xFFEDEDED),
            surfaceContainerHighest = Color(0xFFE6E6E6),
            inverseSurface = Color(0xFF2A2A2A), inverseOnSurface = surface, inversePrimary = accent,
            outline = Color(0xFF8A8A8A), outlineVariant = Color(0xFFDDDDDD),
            error = Color(0xFFE53935), onError = Color.White)
}
