package com.downloader.app.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.storage.AccentPalette
import com.downloader.app.ui.components.AnimatedBottomSheet
import com.downloader.app.core.storage.FontChoice
import com.downloader.app.core.storage.ThemeMode
import com.downloader.app.ui.theme.ArkunSansAvailable
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderText
import com.downloader.app.ui.theme.parseHexColor

private const val SheetHeightDp = 285

/**
 * Overlay "Tampilan" — mengikuti frame Figma
 * `Overlay/ Personalization White Version` (52:295) dan
 * `Overlay/ Personalization` (52:252).
 */
@Composable
fun AppearanceSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()
    val palette by viewModel.accentPalette.collectAsStateWithLifecycle()
    val customHex by viewModel.customAccentHex.collectAsStateWithLifecycle()
    val fontChoice by viewModel.fontChoice.collectAsStateWithLifecycle()
    val fontImportBusy = viewModel.fontImportBusy

    // Warna teks ikut dianimasikan agar tidak ada jeda gelap saat mode
    // berganti (background sheet juga dianimasikan di AnimatedBottomSheet).
    val appearanceText by animateColorAsState(
        targetValue = DownloaderColors.AppearanceText,
        animationSpec = tween(220),
        label = "appearanceText",
    )
    val appearanceTextSecondary by animateColorAsState(
        targetValue = DownloaderColors.AppearanceTextSecondary,
        animationSpec = tween(220),
        label = "appearanceTextSecondary",
    )
    val appearanceHandle by animateColorAsState(
        targetValue = DownloaderColors.AppearanceHandle,
        animationSpec = tween(220),
        label = "appearanceHandle",
    )

    var paletteMenuOpen by remember { mutableStateOf(false) }
    var fontMenuOpen by remember { mutableStateOf(false) }
    var customColorDialogOpen by remember { mutableStateOf(false) }

    // Segmen aktif memakai HEX persis dari Figma (#3B82F6 untuk Biru), bukan
    // aksen yang sudah digelapkan untuk keterbacaan teks di mode terang.
    val segmentAccent = when (palette) {
        AccentPalette.CUSTOM -> parseHexColor(customHex)
        else -> parseHexColor(palette.hex.orEmpty())
    } ?: DownloaderColors.Primary
    val segmentOnAccent = if (segmentAccent.luminance() > 0.48f) {
        Color(0xFF071018)
    } else {
        Color.White
    }

    val fontPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importCustomFont(uri) { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    AnimatedBottomSheet(
        title = DownloaderApp.str(R.string.appearance_title),
        onDismiss = onDismiss,
        sheetHeight = SheetHeightDp.dp,
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(65.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(appearanceHandle),
            )
            Spacer(Modifier.height(18.dp))

            Text(
                text = DownloaderApp.str(R.string.appearance_title),
                style = DownloaderText.sheetTitle,
                color = appearanceText,
            )

            Spacer(Modifier.height(20.dp))

            ModeSegmentedControl(
                mode = mode,
                accent = segmentAccent,
                onAccent = segmentOnAccent,
                inactiveTextColor = appearanceText,
                onModeChange = viewModel::setThemeMode,
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(44.dp),
            ) {
                Text(
                    text = DownloaderApp.str(R.string.appearance_palette_label),
                    style = DownloaderText.nameLg,
                    color = appearanceText,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = DownloaderApp.str(R.string.appearance_font_label),
                    style = DownloaderText.nameLg,
                    color = appearanceText,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(44.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppearanceField(
                        description = DownloaderApp.str(R.string.appearance_color_pick),
                        onClick = { paletteMenuOpen = true },
                    ) {
                        PaletteSwatch(
                            palette = palette,
                            customHex = customHex,
                            size = 18.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = paletteLabel(palette),
                            style = DownloaderText.button,
                            fontWeight = FontWeight.Medium,
                            color = appearanceText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = appearanceTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    PaletteMenu(
                        expanded = paletteMenuOpen,
                        palette = palette,
                        customHex = customHex,
                        onDismiss = { paletteMenuOpen = false },
                        onSelect = { selected ->
                            paletteMenuOpen = false
                            if (selected == AccentPalette.CUSTOM) {
                                customColorDialogOpen = true
                            } else {
                                viewModel.setAccentPalette(selected)
                            }
                        },
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    AppearanceField(
                        description = DownloaderApp.str(R.string.appearance_font_pick),
                        onClick = { fontMenuOpen = true },
                    ) {
                        Text(
                            text = fontLabel(fontChoice),
                            style = DownloaderText.button,
                            fontWeight = FontWeight.Medium,
                            color = appearanceText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (fontImportBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = DownloaderColors.Primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = appearanceTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    FontMenu(
                        expanded = fontMenuOpen,
                        selected = fontChoice,
                        onDismiss = { fontMenuOpen = false },
                        onSelect = { selected ->
                            fontMenuOpen = false
                            when (selected) {
                                FontChoice.CUSTOM -> fontPicker.launch(
                                    arrayOf(
                                        "font/ttf",
                                        "font/otf",
                                        "application/x-font-ttf",
                                        "application/vnd.ms-opentype",
                                        "application/octet-stream",
                                    ),
                                )

                                else -> viewModel.setFontChoice(selected)
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (customColorDialogOpen) {
        ColorPickerSheet(
            initialHex = if (palette == AccentPalette.CUSTOM) customHex else palette.hex.orEmpty(),
            onDismiss = { customColorDialogOpen = false },
            onApply = { value ->
                val applied = viewModel.setCustomAccentHex(value)
                if (applied) customColorDialogOpen = false
                applied
            },
        )
    }
}

@Composable
private fun ModeSegmentedControl(
    mode: ThemeMode,
    accent: Color,
    onAccent: Color,
    inactiveTextColor: Color,
    onModeChange: (ThemeMode) -> Unit,
) {
    val shape = RoundedCornerShape(13.dp)
    val trackColor by animateColorAsState(
        targetValue = DownloaderColors.AppearanceControl,
        animationSpec = tween(220),
        label = "appearanceTrack",
    )
    val borderColor by animateColorAsState(
        targetValue = DownloaderColors.AppearanceBorder,
        animationSpec = tween(220),
        label = "appearanceTrackBorder",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .clip(shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(49.dp)
                .background(trackColor)
                .selectableGroup(),
        ) {
            ModeSegment(
                label = DownloaderApp.str(R.string.appearance_mode_dark),
                selected = mode == ThemeMode.DARK,
                accent = accent,
                onAccent = onAccent,
                inactiveTextColor = inactiveTextColor,
                shape = RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp),
                onClick = { onModeChange(ThemeMode.DARK) },
                modifier = Modifier.weight(1f),
            )
            ModeSegment(
                label = DownloaderApp.str(R.string.appearance_mode_light),
                selected = mode == ThemeMode.LIGHT,
                accent = accent,
                onAccent = onAccent,
                inactiveTextColor = inactiveTextColor,
                shape = RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp),
                onClick = { onModeChange(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f),
            )
        }
        // Border digambar di atas segmen agar garis tepi track tidak tertutup
        // oleh warna segmen aktif.
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, borderColor, shape),
        )
    }
}

@Composable
private fun ModeSegment(
    label: String,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    inactiveTextColor: Color,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) accent else Color.Transparent,
        animationSpec = tween(220),
        label = "appearanceSegmentBackground",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) onAccent else inactiveTextColor,
        animationSpec = tween(220),
        label = "appearanceSegmentText",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(background)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = DownloaderText.button,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppearanceField(
    description: String,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val fieldColor by animateColorAsState(
        targetValue = DownloaderColors.AppearanceField,
        animationSpec = tween(220),
        label = "appearanceField",
    )
    val borderColor by animateColorAsState(
        targetValue = DownloaderColors.AppearanceBorder,
        animationSpec = tween(220),
        label = "appearanceFieldBorder",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(fieldColor)
            .border(1.dp, borderColor, RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = description,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun PaletteMenu(
    expanded: Boolean,
    palette: AccentPalette,
    customHex: String,
    onDismiss: () -> Unit,
    onSelect: (AccentPalette) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = DownloaderColors.AppearanceField,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, DownloaderColors.AppearanceBorder),
    ) {
        AccentPalette.entries.forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = paletteLabel(option),
                        style = DownloaderText.button,
                        fontWeight = FontWeight.Medium,
                        color = DownloaderColors.AppearanceText,
                    )
                },
                onClick = { onSelect(option) },
                leadingIcon = {
                    PaletteSwatch(
                        palette = option,
                        customHex = customHex,
                        size = 18.dp,
                    )
                },
                trailingIcon = {
                    if (option == palette) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = DownloaderColors.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FontMenu(
    expanded: Boolean,
    selected: FontChoice,
    onDismiss: () -> Unit,
    onSelect: (FontChoice) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(230.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = DownloaderColors.AppearanceField,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, DownloaderColors.AppearanceBorder),
    ) {
        FontMenuItem(
            choice = FontChoice.DEFAULT,
            selected = selected,
            enabled = true,
            onSelect = onSelect,
        )
        FontMenuItem(
            choice = FontChoice.SYSTEM,
            selected = selected,
            enabled = true,
            onSelect = onSelect,
        )
        FontMenuItem(
            choice = FontChoice.ARKUN_SANS,
            selected = selected,
            enabled = ArkunSansAvailable,
            onSelect = onSelect,
        )
        FontMenuItem(
            choice = FontChoice.CUSTOM,
            selected = selected,
            enabled = true,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun FontMenuItem(
    choice: FontChoice,
    selected: FontChoice,
    enabled: Boolean,
    onSelect: (FontChoice) -> Unit,
) {
    val label = fontLabel(choice)
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = label,
                    style = DownloaderText.button,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        DownloaderColors.AppearanceText
                    } else {
                        DownloaderColors.AppearanceTextSecondary
                    },
                )
                if (choice == FontChoice.ARKUN_SANS && !ArkunSansAvailable) {
                    Text(
                        text = DownloaderApp.str(R.string.appearance_font_unavailable),
                        style = DownloaderText.metaSm,
                        color = DownloaderColors.AppearanceTextSecondary,
                    )
                }
            }
        },
        onClick = { onSelect(choice) },
        enabled = enabled,
        trailingIcon = {
            if (choice == selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = DownloaderColors.Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

@Composable
private fun PaletteSwatch(
    palette: AccentPalette,
    customHex: String,
    size: Dp,
) {
    val color = when (palette) {
        AccentPalette.CUSTOM -> parseHexColor(customHex) ?: DownloaderColors.Primary
        else -> parseHexColor(palette.hex.orEmpty()) ?: DownloaderColors.Primary
    }
    val brush = if (palette == AccentPalette.CUSTOM) {
        Brush.sweepGradient(
            listOf(
                Color(0xFFF63B3B),
                Color(0xFFFBBF24),
                Color(0xFF4BF63B),
                Color(0xFF38BDF8),
                Color(0xFF3B82F6),
                Color(0xFFF63B3B),
            ),
        )
    } else {
        null
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (brush != null) {
                    Modifier.background(brush)
                } else {
                    Modifier.background(color)
                },
            )
            .border(1.dp, Color.Black.copy(alpha = 0.18f), CircleShape),
    )
}

internal fun paletteLabel(palette: AccentPalette): String = when (palette) {
    AccentPalette.BLUE -> DownloaderApp.str(R.string.appearance_palette_blue)
    AccentPalette.RED -> DownloaderApp.str(R.string.appearance_palette_red)
    AccentPalette.GREEN -> DownloaderApp.str(R.string.appearance_palette_green)
    AccentPalette.CUSTOM -> DownloaderApp.str(R.string.appearance_palette_custom)
}

internal fun fontLabel(choice: FontChoice): String = when (choice) {
    FontChoice.DEFAULT -> DownloaderApp.str(R.string.appearance_font_default)
    FontChoice.SYSTEM -> DownloaderApp.str(R.string.appearance_font_system)
    FontChoice.ARKUN_SANS -> DownloaderApp.str(R.string.appearance_font_arkun)
    FontChoice.CUSTOM -> DownloaderApp.str(R.string.appearance_font_custom)
}

internal fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.DARK -> DownloaderApp.str(R.string.appearance_mode_dark)
    ThemeMode.LIGHT -> DownloaderApp.str(R.string.appearance_mode_light)
}
