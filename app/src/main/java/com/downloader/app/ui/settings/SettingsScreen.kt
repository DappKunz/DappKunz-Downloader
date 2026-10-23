package com.downloader.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.storage.AccentPalette
import com.downloader.app.core.storage.OutputPublisher
import com.downloader.app.core.storage.ThemeMode
import com.downloader.app.core.util.LocaleHelper
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderText

@Composable
fun SettingsScreen(
    onAbout: () -> Unit,
    onUpdate: () -> Unit,
    onDebug: () -> Unit,
    onDonate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val language by viewModel.language.collectAsStateWithLifecycle()
    val folderUri by viewModel.folderUri.collectAsStateWithLifecycle()
    val turbo by viewModel.turbo.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentPalette by viewModel.accentPalette.collectAsStateWithLifecycle()
    val customAccentHex by viewModel.customAccentHex.collectAsStateWithLifecycle()

    // Sengaja bukan rememberSaveable: overlay harus tertutup setelah recreate
    // (pergantian bahasa) dan tidak terbuka sendiri saat rotasi.
    var languageSheetOpen by remember { mutableStateOf(false) }
    var appearanceOpen by remember { mutableStateOf(false) }

    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) {
            // izin persisten tidak tersedia pada sebagian penyedia dokumen
        }
        val tree = DocumentFile.fromTreeUri(context, uri)
        if (tree == null || !tree.canWrite()) {
            Toast.makeText(
                context,
                DownloaderApp.str(R.string.settings_folder_no_write),
                Toast.LENGTH_LONG,
            ).show()
        } else {
            viewModel.setFolderUri(uri.toString())
            Toast.makeText(
                context,
                DownloaderApp.str(R.string.settings_folder_picked, tree.name ?: ""),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val resetFolder = {
        viewModel.setFolderUri(null)
        Toast.makeText(
            context,
            DownloaderApp.str(R.string.settings_folder_reset_done),
            Toast.LENGTH_SHORT,
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 118.dp),
    ) {
        Text(
            text = DownloaderApp.str(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = DownloaderColors.TextPrimary,
            modifier = Modifier.entrance(0),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = DownloaderApp.str(R.string.settings_subtitle),
            style = DownloaderText.meta,
            color = DownloaderColors.TextSecondary,
            modifier = Modifier.entrance(0, offsetY = 10),
        )
        Spacer(Modifier.height(4.dp))

        SettingsSection(
            title = DownloaderApp.str(R.string.settings_download_section),
            modifier = Modifier.entrance(1),
        ) {
            SettingSwitchRow(
                icon = Icons.Rounded.Rocket,
                title = DownloaderApp.str(R.string.settings_turbo_title),
                value = DownloaderApp.str(R.string.settings_turbo_desc),
                checked = turbo,
                onCheckedChange = viewModel::setTurbo,
            )
        }

        SettingsSection(
            title = DownloaderApp.str(R.string.settings_storage_section),
            modifier = Modifier.entrance(2),
        ) {
            SettingRow(
                icon = Icons.Rounded.Folder,
                title = DownloaderApp.str(R.string.settings_folder_title),
                value = folderUri?.let {
                    OutputPublisher.describeTreeLocation(context, Uri.parse(it))
                } ?: DownloaderApp.str(R.string.settings_folder_default),
                onClick = { pickFolder.launch(null) },
                trailing = if (folderUri != null) {
                    {
                        IconButton(
                            onClick = resetFolder,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SettingsBackupRestore,
                                contentDescription = DownloaderApp.str(R.string.settings_folder_reset_title),
                                tint = DownloaderColors.TextMuted,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                } else {
                    null
                },
            )
            SettingRow(
                icon = Icons.Rounded.Language,
                title = DownloaderApp.str(R.string.settings_language_title),
                value = languageLabel(language),
                onClick = { languageSheetOpen = true },
            )
            SettingRow(
                icon = Icons.Rounded.Palette,
                title = DownloaderApp.str(R.string.settings_appearance_title),
                value = appearanceSummary(themeMode, accentPalette, customAccentHex),
                onClick = { appearanceOpen = true },
            )
        }

        SettingsSection(
            title = DownloaderApp.str(R.string.settings_donation_section),
            modifier = Modifier.entrance(3),
        ) {
            SettingRow(
                icon = Icons.Rounded.Favorite,
                iconTint = DownloaderColors.Warning,
                title = DownloaderApp.str(R.string.settings_donation_title),
                value = DownloaderApp.str(R.string.settings_donation_desc),
                onClick = onDonate,
            )
        }

        SettingsSection(
            title = DownloaderApp.str(R.string.settings_about_section),
            modifier = Modifier.entrance(4),
        ) {
            SettingRow(
                icon = Icons.Rounded.SystemUpdate,
                title = DownloaderApp.str(R.string.settings_update_title),
                value = DownloaderApp.str(R.string.settings_update_desc),
                onClick = onUpdate,
            )
            SettingRow(
                icon = Icons.Rounded.Info,
                title = DownloaderApp.str(R.string.settings_about_title),
                value = DownloaderApp.str(R.string.settings_about_subtitle),
                onClick = onAbout,
            )
        }

        if (debuggable) {
            SettingsSection(
                title = DownloaderApp.str(R.string.settings_debug_title),
                modifier = Modifier.entrance(5),
            ) {
                SettingRow(
                    icon = Icons.Rounded.BugReport,
                    title = DownloaderApp.str(R.string.settings_debug_title),
                    value = DownloaderApp.str(R.string.settings_debug_desc),
                    onClick = onDebug,
                )
            }
        }
    }

    if (languageSheetOpen) {
        LanguageSheet(
            currentLanguage = language,
            onDismiss = { languageSheetOpen = false },
            onApply = { tag ->
                languageSheetOpen = false
                applyLanguage(context, viewModel, tag)
            },
        )
    }

    if (appearanceOpen) {
        AppearanceSheet(
            viewModel = viewModel,
            onDismiss = { appearanceOpen = false },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
    ) {
        Text(
            text = title,
            style = DownloaderText.groupTitle,
            color = DownloaderColors.TextMuted,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    iconTint: Color = DownloaderColors.Primary,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(DownloaderColors.Surface)
            .border(1.dp, DownloaderColors.Border, shape)
            .clickable(onClickLabel = title, onClick = onClick)
            .pressScale(scaleDown = 0.985f)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon = icon, tint = iconTint)
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = DownloaderText.rowTitle,
                fontWeight = FontWeight.Medium,
                color = DownloaderColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (fadeIn(tween(180)) + androidx.compose.animation.slideInVertically(tween(220)) { it / 3 }) togetherWith
                        (fadeOut(tween(120)) + androidx.compose.animation.slideOutVertically(tween(160)) { -it / 3 })
                },
                label = "settingValue",
            ) { current ->
                Text(
                    text = current,
                    style = DownloaderText.metaSm,
                    color = DownloaderColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = DownloaderColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(DownloaderColors.Surface)
            .border(1.dp, DownloaderColors.Border, shape)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon = icon, tint = DownloaderColors.Primary)
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = DownloaderText.rowTitle,
                fontWeight = FontWeight.Medium,
                color = DownloaderColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        DownloaderSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(DownloaderColors.PrimarySoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Figma's compact 39×22 toggle, with a spring-driven thumb. */
@Composable
private fun DownloaderSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val progress by animateDpAsState(
        targetValue = if (checked) 17.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 640f),
        label = "switchThumb",
    )
    val track by animateColorAsState(
        targetValue = if (checked) DownloaderColors.Primary else DownloaderColors.Border,
        animationSpec = tween(200),
        label = "switchTrack",
    )
    val thumb by animateColorAsState(
        targetValue = if (checked) Color.White else DownloaderColors.TextMuted,
        animationSpec = tween(200),
        label = "switchThumbColor",
    )
    Box(
        modifier = Modifier
            .size(width = 39.dp, height = 22.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(track)
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange,
            )
            .pressScale(scaleDown = 0.92f),
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .offset(x = progress)
                .size(18.dp)
                .clip(CircleShape)
                .background(thumb),
        )
    }
}

@Suppress("DEPRECATION")
private fun applyLanguage(context: android.content.Context, viewModel: SettingsViewModel, tag: String) {
    if (viewModel.language.value == tag) return
    viewModel.setLanguage(tag)
    (context as? Activity)?.let { activity ->
        activity.recreate()
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

private fun languageLabel(tag: String): String = when (tag) {
    LocaleHelper.LANG_ENGLISH -> DownloaderApp.str(R.string.language_english)
    else -> DownloaderApp.str(R.string.language_indonesian)
}

private fun appearanceSummary(
    mode: ThemeMode,
    palette: AccentPalette,
    customHex: String,
): String {
    val paletteText = if (palette == AccentPalette.CUSTOM) {
        DownloaderApp.str(R.string.appearance_palette_custom_value, customHex)
    } else {
        paletteLabel(palette)
    }
    return DownloaderApp.str(
        R.string.appearance_summary_short,
        themeModeLabel(mode),
        paletteText,
    )
}
