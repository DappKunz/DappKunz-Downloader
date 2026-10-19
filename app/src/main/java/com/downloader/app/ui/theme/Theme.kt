package com.downloader.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = DownloaderColors.Primary,
    onPrimary = Color.White,
    primaryContainer = DownloaderColors.PrimarySoft,
    onPrimaryContainer = Color(0xFFBFDBFE),
    inversePrimary = DownloaderColors.PrimaryDeep,
    secondary = DownloaderColors.Cyan,
    onSecondary = Color(0xFF06121F),
    secondaryContainer = DownloaderColors.PrimarySoft,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = DownloaderColors.Success,
    onTertiary = Color(0xFF04210F),
    tertiaryContainer = Color(0xFF0E3A20),
    onTertiaryContainer = Color(0xFFBBF7D0),
    background = DownloaderColors.Background,
    onBackground = DownloaderColors.TextPrimary,
    surface = DownloaderColors.Surface,
    onSurface = DownloaderColors.TextPrimary,
    surfaceVariant = DownloaderColors.SurfaceVariant,
    onSurfaceVariant = DownloaderColors.TextSecondary,
    surfaceTint = DownloaderColors.Primary,
    inverseSurface = DownloaderColors.TextPrimary,
    inverseOnSurface = DownloaderColors.Background,
    outline = DownloaderColors.Border,
    outlineVariant = Color(0xFF222836),
    error = DownloaderColors.Danger,
    onError = Color(0xFF2A0A0A),
    errorContainer = Color(0xFF3A1416),
    onErrorContainer = Color(0xFFFCA5A5),
    scrim = Color(0xFF000000),
)

@Composable
fun DownloaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = DownloaderTypography,
        shapes = DownloaderShapes,
        content = content,
    )
}
