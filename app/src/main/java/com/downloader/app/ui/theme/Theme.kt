package com.downloader.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.downloader.app.core.storage.AccentPalette
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.storage.ThemeMode
import com.downloader.app.core.storage.normalizeHexColor

private val DefaultAccent = Color(0xFF3B82F6)

@Composable
fun DownloaderTheme(content: @Composable () -> Unit) {
    val themeMode by PrefsRepository.themeMode.collectAsState()
    val accentPalette by PrefsRepository.accentPalette.collectAsState()
    val customAccentHex by PrefsRepository.customAccentHex.collectAsState()
    val fontChoice by PrefsRepository.fontChoice.collectAsState()
    val customFontPath by PrefsRepository.customFontPath.collectAsState()

    val dark = themeMode == ThemeMode.DARK
    val accent = remember(accentPalette, customAccentHex) {
        resolveAccentColor(accentPalette, customAccentHex)
    }
    val fontFamily = remember(fontChoice, customFontPath) {
        resolveAppFontFamily(fontChoice, customFontPath)
    }

    // Token lama dipakai di ratusan tempat. SideEffect menjaga nilainya sinkron
    // setelah komposisi (bukan menulis state saat komposisi berlangsung).
    SideEffect {
        DownloaderColors.configure(dark = dark, accent = accent)
        DownloaderFonts.configure(fontFamily)
    }

    MaterialTheme(
        colorScheme = buildColorScheme(dark = dark, accent = accent),
        typography = buildDownloaderTypography(fontFamily),
        shapes = DownloaderShapes,
        content = content,
    )
}

fun resolveAccentColor(palette: AccentPalette, customHex: String): Color = when (palette) {
    AccentPalette.CUSTOM -> parseHexColor(customHex) ?: DefaultAccent
    else -> parseHexColor(palette.hex.orEmpty()) ?: DefaultAccent
}

fun parseHexColor(value: String): Color? {
    val normalized = normalizeHexColor(value) ?: return null
    return runCatching { Color(0xFF000000L or normalized.drop(1).toLong(16)) }.getOrNull()
}

/**
 * Scheme Material dibangun dari nilai lokal (bukan dari [DownloaderColors])
 * supaya perubahan mode/aksen langsung terlihat pada frame yang sama.
 */
private fun buildColorScheme(dark: Boolean, accent: Color): ColorScheme {
    fun pick(darkValue: Color, lightValue: Color): Color = if (dark) darkValue else lightValue

    fun blend(base: Color, target: Color, amount: Float): Color =
        lerp(base, target, amount.coerceIn(0f, 1f))

    val brand = accessibleAccentColor(accent, dark)
    val background = pick(Color(0xFF080A10), Color(0xFFDDE8FA))
    val surface = pick(Color(0xFF151821), Color(0xFFEAF2FF))
    val surfaceElevated = pick(Color(0xFF1D2230), Color(0xFFF2F7FF))
    val surfaceVariant = pick(Color(0xFF1D2230), Color(0xFFCFDEF5))
    val border = pick(Color(0xFF2B3242), Color(0xFFA8BEDF))
    val textPrimary = pick(Color(0xFFF8FAFC), Color(0xFF081A33))
    val textSecondary = pick(Color(0xFFA2ABBC), Color(0xFF3E5273))
    val primarySoft = if (dark) {
        blend(brand, background, 0.80f)
    } else {
        blend(brand, surface, 0.82f)
    }
    val cyan = if (dark) blend(brand, Color.White, 0.30f) else blend(brand, Color.Black, 0.08f)
    val textOnSoft = if (dark) blend(brand, Color.White, 0.66f) else blend(brand, Color.Black, 0.52f)
    val onPrimary = if (brand.luminance() > 0.48f) Color(0xFF071018) else Color.White
    val success = pick(Color(0xFF22C55E), Color(0xFF16A34A))
    val successSoft = pick(Color(0xFF14382A), Color(0xFFD7F5E3))
    val danger = pick(Color(0xFFF87171), Color(0xFFDC2626))
    val dangerSoft = pick(Color(0xFF3B202A), Color(0xFFFFE1E1))
    val onDanger = if (danger.luminance() > 0.35f) Color(0xFF2A0A0A) else Color.White
    val inversePrimary = if (dark) blend(brand, Color.Black, 0.16f) else blend(brand, Color.Black, 0.24f)
    val outlineVariant = pick(Color(0xFF222836), Color(0xFFC4D4EA))
    val scrim = pick(Color(0xFF000000), Color(0xFF080A10))

    return if (dark) {
        darkColorScheme(
            primary = brand,
            onPrimary = onPrimary,
            primaryContainer = primarySoft,
            onPrimaryContainer = textOnSoft,
            inversePrimary = inversePrimary,
            secondary = cyan,
            onSecondary = onPrimary,
            secondaryContainer = primarySoft,
            onSecondaryContainer = textOnSoft,
            tertiary = success,
            onTertiary = Color.White,
            tertiaryContainer = successSoft,
            onTertiaryContainer = success,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            surfaceTint = brand,
            inverseSurface = textPrimary,
            inverseOnSurface = background,
            outline = border,
            outlineVariant = outlineVariant,
            error = danger,
            onError = onDanger,
            errorContainer = dangerSoft,
            onErrorContainer = danger,
            scrim = scrim,
        )
    } else {
        lightColorScheme(
            primary = brand,
            onPrimary = onPrimary,
            primaryContainer = primarySoft,
            onPrimaryContainer = textOnSoft,
            inversePrimary = inversePrimary,
            secondary = cyan,
            onSecondary = onPrimary,
            secondaryContainer = primarySoft,
            onSecondaryContainer = textOnSoft,
            tertiary = success,
            onTertiary = Color.White,
            tertiaryContainer = successSoft,
            onTertiaryContainer = success,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            surfaceTint = brand,
            inverseSurface = textPrimary,
            inverseOnSurface = background,
            outline = border,
            outlineVariant = outlineVariant,
            error = danger,
            onError = onDanger,
            errorContainer = dangerSoft,
            onErrorContainer = danger,
            scrim = scrim,
        )
    }
}
