package de.danoeh.antennapod.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearColors = Colors(
    primary = Color(0xFF3D8BFF),
    primaryVariant = Color(0xFF0078C2),
    secondary = Color(0xFF16D0FF),
    secondaryVariant = Color(0xFF0E9DBF),
    error = Color(0xFFCF6679),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onError = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

@Composable
fun WearAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WearColors,
        content = content
    )
}
