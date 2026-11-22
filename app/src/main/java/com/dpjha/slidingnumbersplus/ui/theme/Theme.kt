package com.dpjha.slidingnumbersplus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CityNight = Color(0xFF101322)
private val RaisedNavy = Color(0xFF202944)
private val Coral = Color(0xFFFF687A)
private val Gold = Color(0xFFFFD166)
private val Mint = Color(0xFF48D6B0)
private val SoftText = Color(0xFFB7C0D8)

private val GameColors = darkColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = CityNight,
    tertiary = Mint,
    background = CityNight,
    surface = RaisedNavy,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF303A56),
    onSurfaceVariant = SoftText,
)

@Composable
fun SlidingNumbersTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameColors,
        content = content,
    )
}
