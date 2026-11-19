package com.udeshcoffee.android.ui.compose.theme

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Pixels ship Google Sans as a system font; other devices keep their own system font. */
private val isPixel: Boolean = Build.MANUFACTURER.equals("Google", ignoreCase = true)

/**
 * A family from Pixel's system fonts, by name ("google-sans", "google-sans-text"). Pixels also
 * ship per-weight families ("google-sans-medium", ...), tried first for those weights; a name the
 * device doesn't have falls back to the next font and finally to the system default.
 */
private fun googleSans(name: String) = FontFamily(
        Font(DeviceFontFamilyName(name), FontWeight.Normal),
        Font(DeviceFontFamilyName("$name-medium"), FontWeight.Medium),
        Font(DeviceFontFamilyName(name), FontWeight.Medium),
        Font(DeviceFontFamilyName("$name-bold"), FontWeight.Bold),
        Font(DeviceFontFamilyName(name), FontWeight.Bold),
)

private fun TextStyle.with(family: FontFamily) = copy(fontFamily = family)

/**
 * The app's type scale: Material 3 defaults, set in Google Sans on Pixels (Google Sans for display,
 * headline and title styles, Google Sans Text for body and label styles, as Pixel's own apps do).
 */
val AppTypography: Typography = Typography().let { base ->
    if (!isPixel) return@let base
    val display = googleSans("google-sans")
    val text = googleSans("google-sans-text")
    base.copy(
            displayLarge = base.displayLarge.with(display),
            displayMedium = base.displayMedium.with(display),
            displaySmall = base.displaySmall.with(display),
            headlineLarge = base.headlineLarge.with(display),
            headlineMedium = base.headlineMedium.with(display),
            headlineSmall = base.headlineSmall.with(display),
            titleLarge = base.titleLarge.with(display),
            titleMedium = base.titleMedium.with(display),
            titleSmall = base.titleSmall.with(display),
            bodyLarge = base.bodyLarge.with(text),
            bodyMedium = base.bodyMedium.with(text),
            bodySmall = base.bodySmall.with(text),
            labelLarge = base.labelLarge.with(text),
            labelMedium = base.labelMedium.with(text),
            labelSmall = base.labelSmall.with(text),
    )
}
