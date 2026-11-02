package com.openjarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisDarkColorScheme = darkColorScheme(
    primary = VoidColor.Violet,
    onPrimary = VoidColor.TextPrimary,
    secondary = VoidColor.Cyan,
    onSecondary = VoidColor.Void950,
    background = VoidColor.Void950,
    onBackground = VoidColor.TextPrimary,
    surface = VoidColor.Void900,
    onSurface = VoidColor.TextPrimary,
    surfaceVariant = VoidColor.Void800,
    onSurfaceVariant = VoidColor.TextSecondary,
    error = VoidColor.Red,
    onError = VoidColor.Void950
)

@Composable
fun OpenJarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        content = content
    )
}
