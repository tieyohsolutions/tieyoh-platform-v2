package com.tieyoh.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TieyohAccent,
    onPrimary = Color.White,
    secondary = TieyohNavy,
    tertiary = TieyohSponsoredLabel,
    background = Color(0xFF0B1F1C),
    surface = Color(0xFF102C27)
)

private val LightColorScheme = lightColorScheme(
    primary = TieyohAccent,
    onPrimary = Color.White,
    secondary = TieyohNavy,
    onSecondary = Color.White,
    tertiary = TieyohSponsoredLabel,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = TieyohCardBg,
    onSurface = TieyohNavy,
    onBackground = TieyohNavy
)

// Dynamic color (Material You) is intentionally left off by default so the
// Tieyoh teal brand always shows, regardless of the device wallpaper.
@Composable
fun TieyohTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
