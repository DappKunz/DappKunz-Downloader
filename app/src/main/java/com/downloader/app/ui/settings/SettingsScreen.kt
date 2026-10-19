package com.downloader.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.storage.OutputPublisher
import com.downloader.app.core.util.Formatters
import com.downloader.app.core.util.LocaleHelper
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderText

@Composable
fun SettingsScreen(
    onAbout: () -> Unit,
    onUpdate: () -> Unit,
    onHistory: () -> Unit,
    onLog: () -> Unit,
    onDebug: () -> Unit,
    onDonate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsStateWithLifecycle()
    val folderUri by viewModel.folderUri.collectAsStateWithLifecycle()
    val turbo by viewModel.turbo.collectAsStateWithLifecycle()

    var languageDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 118.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = DownloaderApp.str(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        SettingsGroup(title = DownloaderApp.str(R.string.settings_download_section)) {
            SettingSwitchRow(
                icon = Icons.Rounded.Rocket,
                title = DownloaderApp.str(R.string.settings_turbo_title),
                value = DownloaderApp.str(R.string.settings_turbo_desc),
                checked = turbo,
                onCheckedChange = viewModel::setTurbo,
            )
        }

        SettingsGroup(title = DownloaderApp.str(R.string.settings_storage_section)) {
            SettingRow(
                icon = Icons.Rounded.Folder,
                title = DownloaderApp.str(R.string.settings_folder_title),
                value = folderUri?.let {
                    OutputPublisher.describeTreeLocation(context, Uri.parse(it))
                } ?: DownloaderApp.str(R.string.settings_folder_default),
                actionLabel = DownloaderApp.str(R.string.settings_folder_pick),
                onAction = { pickFolder.launch(null) },
            )
            if (folderUri != null) {
                SettingRow(
                    icon = Icons.Rounded.SettingsBackupRestore,
                    title = DownloaderApp.str(R.string.settings_folder_reset_title),
                    value = DownloaderApp.str(R.string.settings_folder_reset_desc),
                    actionLabel = DownloaderApp.str(R.string.settings_folder_reset_action),
                    onAction = {
                        viewModel.setFolderUri(null)
                        Toast.makeText(
                            context,
                            DownloaderApp.str(R.string.settings_folder_reset_done),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }
        }

        SettingsGroup(title = DownloaderApp.str(R.string.settings_history_section)) {
            SettingRow(
                icon = Icons.Rounded.History,
                title = DownloaderApp.str(R.string.settings_history_title),
                value = DownloaderApp.str(R.string.settings_history_desc),
                actionLabel = null,
                onAction = onHistory,
                wholeRowClickable = true,
            )
            SettingRow(
                icon = Icons.Rounded.Description,
                title = DownloaderApp.str(R.string.settings_log_title),
                value = DownloaderApp.str(R.string.settings_log_desc),
                actionLabel = null,
                onAction = onLog,
                wholeRowClickable = true,
            )
        }

        SettingsGroup(title = DownloaderApp.str(R.string.settings_language_section)) {
            SettingRow(
                icon = Icons.Rounded.Language,
                title = DownloaderApp.str(R.string.settings_language_title),
                value = languageLabel(language),
                actionLabel = DownloaderApp.str(R.string.action_change),
                onAction = { languageDialog = true },
            )
        }

        SettingsGroup(title = DownloaderApp.str(R.string.settings_cache_section)) {
            SettingRow(
                icon = Icons.Rounded.CleaningServices,
                title = DownloaderApp.str(R.string.settings_cache_title),
                value = when {
                    viewModel.cacheBusy -> DownloaderApp.str(R.string.settings_cache_calculating)
                    viewModel.cacheSizeBytes != null ->
                        DownloaderApp.str(R.string.settings_cache_size, Formatters.bytes(viewModel.cacheSizeBytes ?: 0))

                    else -> DownloaderApp.str(R.string.settings_cache_desc)
                },
                actionLabel = DownloaderApp.str(R.string.settings_cache_confirm_action),
                onAction = { clearDialog = true },
            )
        }

        SettingsGroup(title = DownloaderApp.str(R.string.settings_update_section)) {
            SettingRow(
                icon = Icons.Rounded.SystemUpdate,
                title = DownloaderApp.str(R.string.settings_update_title),
                value = DownloaderApp.str(R.string.settings_update_desc),
                actionLabel = null,
                onAction = onUpdate,
                wholeRowClickable = true,
            )
        }

        SettingsGroup(title = DownloaderApp.str(R.string.settings_donation_section)) {
            SettingRow(
                icon = Icons.Rounded.Favorite,
                title = DownloaderApp.str(R.string.settings_donation_title),
                value = DownloaderApp.str(R.string.settings_donation_desc),
                actionLabel = null,
                onAction = onDonate,
                wholeRowClickable = true,
            )
        }

        SettingsGroup(title = DownloaderApp.str(R.string.settings_about_section)) {
            SettingRow(
                icon = Icons.Rounded.Info,
                title = DownloaderApp.str(R.string.settings_about_title),
                value = appVersionLabel(context),
                actionLabel = null,
                onAction = onAbout,
                wholeRowClickable = true,
            )
            SettingRow(
                icon = Icons.Rounded.BugReport,
                title = DownloaderApp.str(R.string.settings_debug_title),
                value = DownloaderApp.str(R.string.settings_debug_desc),
                actionLabel = null,
                onAction = onDebug,
                wholeRowClickable = true,
            )
        }
    }

    if (languageDialog) {
        AlertDialog(
            onDismissRequest = { languageDialog = false },
            title = { Text(DownloaderApp.str(R.string.settings_language_dialog_title)) },
            text = {
                Column {
                    LanguageOption(
                        label = DownloaderApp.str(R.string.language_indonesian),
                        selected = language == LocaleHelper.LANG_INDONESIAN,
                        onClick = {
                            languageDialog = false
                            applyLanguage(context, viewModel, LocaleHelper.LANG_INDONESIAN)
                        },
                    )
                    LanguageOption(
                        label = DownloaderApp.str(R.string.language_english),
                        selected = language == LocaleHelper.LANG_ENGLISH,
                        onClick = {
                            languageDialog = false
                            applyLanguage(context, viewModel, LocaleHelper.LANG_ENGLISH)
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { languageDialog = false }) {
                    Text(DownloaderApp.str(R.string.action_close))
                }
            },
        )
    }

    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text(DownloaderApp.str(R.string.settings_cache_confirm_title)) },
            text = { Text(DownloaderApp.str(R.string.settings_cache_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearDialog = false
                        val jobsActive = com.downloader.app.core.util.CacheManager.activeJobIds().isNotEmpty()
                        viewModel.clearCache { freed ->
                            val message = when {
                                jobsActive -> DownloaderApp.str(R.string.settings_cache_busy)
                                freed > 0 -> DownloaderApp.str(R.string.settings_cache_freed, Formatters.bytes(freed))
                                else -> DownloaderApp.str(R.string.settings_cache_nothing)
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(DownloaderApp.str(R.string.settings_cache_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearDialog = false }) {
                    Text(DownloaderApp.str(R.string.action_cancel))
                }
            },
        )
    }
}

private fun applyLanguage(context: android.content.Context, viewModel: SettingsViewModel, tag: String) {
    if (viewModel.language.value == tag) return
    viewModel.setLanguage(tag)
    (context as? Activity)?.recreate()
}

private fun languageLabel(tag: String): String = when (tag) {
    LocaleHelper.LANG_ENGLISH -> DownloaderApp.str(R.string.language_english)
    else -> DownloaderApp.str(R.string.language_indonesian)
}

private fun appVersionLabel(context: android.content.Context): String = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    DownloaderApp.str(R.string.about_version, info.versionName ?: "-", code.toString())
} catch (_: PackageManager.NameNotFoundException) {
    "-"
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = DownloaderText.sectionTitleSm,
            color = DownloaderColors.TextPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Card(
            shape = RoundedCornerShape(DownloaderRadius.md),
            colors = CardDefaults.cardColors(containerColor = DownloaderColors.Surface),
            border = BorderStroke(1.dp, DownloaderColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    actionLabel: String?,
    onAction: () -> Unit,
    wholeRowClickable: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (wholeRowClickable) {
                    Modifier.clickable(onClick = onAction)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DownloaderDimens.iconTileMd)
                .clip(RoundedCornerShape(DownloaderRadius.tile))
                .background(DownloaderColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DownloaderColors.Cyan,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DownloaderText.rowTitleSm,
                color = DownloaderColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
            )
        }
        if (actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        } else {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = DownloaderColors.TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .padding(5.dp)
                    .size(18.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DownloaderDimens.iconTileMd)
                .clip(RoundedCornerShape(DownloaderRadius.tile))
                .background(DownloaderColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DownloaderColors.Cyan,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DownloaderText.rowTitleSm,
                color = DownloaderColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
