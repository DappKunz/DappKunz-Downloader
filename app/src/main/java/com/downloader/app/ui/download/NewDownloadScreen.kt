package com.downloader.app.ui.download

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.util.Formatters
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderText
import com.downloader.app.ui.components.AnalyzeSkeleton
import com.downloader.app.ui.components.AnimatedErrorBanner
import com.downloader.app.ui.components.FormatPills
import com.downloader.app.ui.components.MediaPreview
import com.downloader.app.ui.components.PlaylistSection
import com.downloader.app.ui.components.PrimaryDownloadButton
import com.downloader.app.ui.components.QualitySection

@Composable
fun NewDownloadScreen(
    prefill: DownloadPrefill?,
    onPrefillConsumed: () -> Unit,
    onClose: () -> Unit,
    onStarted: () -> Unit,
    onDuplicate: () -> Unit,
    viewModel: DownloadViewModel = viewModel(),
) {
    val clipboard = LocalClipboardManager.current
    val analyze = viewModel.analyze
    var showStorageConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(prefill) {
        if (prefill != null) {
            viewModel.applyPrefill(prefill)
            onPrefillConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = DownloaderApp.str(R.string.action_back),
                )
            }
            Text(
                text = DownloaderApp.str(R.string.new_download_title),
                style = DownloaderText.screenTitle,
                color = DownloaderColors.TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = DownloaderApp.str(R.string.new_download_subtitle),
                style = DownloaderText.body,
                color = DownloaderColors.TextSecondary,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = analyze.url,
                    onValueChange = viewModel::onUrlChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    placeholder = { Text(DownloaderApp.str(R.string.url_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(DownloaderRadius.card),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DownloaderColors.Surface,
                        unfocusedContainerColor = DownloaderColors.Surface,
                        focusedBorderColor = DownloaderColors.Primary,
                        unfocusedBorderColor = DownloaderColors.Border,
                        cursorColor = DownloaderColors.Primary,
                        focusedTextColor = DownloaderColors.TextPrimary,
                        unfocusedTextColor = DownloaderColors.TextPrimary,
                        focusedPlaceholderColor = DownloaderColors.TextSecondary,
                        unfocusedPlaceholderColor = DownloaderColors.TextSecondary,
                        focusedTrailingIconColor = DownloaderColors.TextMuted,
                        unfocusedTrailingIconColor = DownloaderColors.TextMuted,
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Tombol X: kosongkan tautan lama sekali sentuh tanpa
                            // harus menghapus manual lewat keyboard.
                            if (analyze.url.isNotEmpty()) {
                                IconButton(onClick = viewModel::onUrlClear) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = DownloaderApp.str(R.string.action_clear_link),
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val text = clipboard.getText()?.text
                                    if (text.isNullOrBlank()) {
                                        viewModel.onClipboardEmpty()
                                    } else {
                                        viewModel.onUrlChange(text.trim())
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Rounded.ContentPaste,
                                    contentDescription = DownloaderApp.str(R.string.action_paste),
                                )
                            }
                        }
                    },
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = viewModel::runAnalyze,
                    enabled = analyze.url.isNotBlank() && !analyze.loading,
                    shape = RoundedCornerShape(DownloaderRadius.card),
                    modifier = Modifier.height(56.dp),
                ) {
                    if (analyze.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            DownloaderApp.str(R.string.action_analyze),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Text(
                text = DownloaderApp.str(R.string.supported_platforms),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedErrorBanner(
                message = analyze.error,
                onDismiss = viewModel::dismissError,
            )
            AnimatedErrorBanner(
                message = viewModel.userMessage,
                onDismiss = viewModel::dismissUserMessage,
            )

            when {
                analyze.loading -> AnalyzeSkeleton()

                analyze.meta != null -> {
                    val meta = analyze.meta
                    MediaPreview(meta = meta)

                    if (meta.media.type != "photo" && (meta.capabilities.video || meta.capabilities.audio)) {
                        PresetRow(
                            selected = viewModel.preset,
                            onSelect = { preset ->
                                if (preset == DownloadPreset.STORAGE) {
                                    showStorageConfirm = true
                                } else {
                                    viewModel.applyPreset(preset)
                                }
                            },
                        )
                    }

                    FormatPills(
                        meta = meta,
                        selected = viewModel.selectedFormat,
                        onSelect = viewModel::selectFormat,
                    )

                    QualitySection(
                        meta = meta,
                        format = viewModel.selectedFormat,
                        bitrate = viewModel.bitrate,
                        resolution = viewModel.resolution,
                        hd = viewModel.hd,
                        onBitrate = viewModel::updateBitrate,
                        onResolution = viewModel::updateResolution,
                        onHd = viewModel::updateHd,
                    )

                    val playlist = meta.playlist
                    if (playlist != null) {
                        PlaylistSection(
                            playlist = playlist,
                            selected = viewModel.selectedItems,
                            zipMode = viewModel.zipMode,
                            format = viewModel.selectedFormat,
                            bitrate = viewModel.bitrate,
                            resolution = viewModel.resolution,
                            hd = viewModel.hd,
                            totalEstimateBytes = viewModel.playlistEstimateBytes,
                            onToggle = viewModel::toggleItem,
                            onSelectAll = viewModel::selectAllItems,
                            onUnselectAll = viewModel::unselectAllItems,
                            onZipChange = viewModel::updateZipMode,
                        )
                    } else {
                        val estimate = viewModel.singleEstimateBytes
                        if (estimate != null && estimate > 0) {
                            Text(
                                text = DownloaderApp.str(R.string.estimate_size, Formatters.bytes(estimate)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    val needsSelection = playlist != null && playlist.items.isNotEmpty()
                    val hint = if (needsSelection && viewModel.selectedItems.isEmpty()) {
                        DownloaderApp.str(R.string.playlist_need_selection)
                    } else {
                        null
                    }

                    PrimaryDownloadButton(
                        enabled = !needsSelection || viewModel.selectedItems.isNotEmpty(),
                        hint = hint,
                        loading = viewModel.starting,
                        onClick = {
                            viewModel.startDownload(
                                onStarted = onStarted,
                                onDuplicate = onDuplicate,
                            )
                        },
                    )
                }
            }
        }
    }

    if (showStorageConfirm) {
        AlertDialog(
            onDismissRequest = { showStorageConfirm = false },
            title = { Text(DownloaderApp.str(R.string.download_preset_storage_confirm_title)) },
            text = { Text(DownloaderApp.str(R.string.download_preset_storage_confirm_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStorageConfirm = false
                        viewModel.applyPreset(DownloadPreset.STORAGE)
                    },
                ) {
                    Text(DownloaderApp.str(R.string.download_preset_storage_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageConfirm = false }) {
                    Text(DownloaderApp.str(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PresetRow(selected: DownloadPreset?, onSelect: (DownloadPreset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = DownloaderApp.str(R.string.download_preset_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            DownloadPreset.values().forEach { preset ->
                FilterChip(
                    selected = selected == preset,
                    onClick = { onSelect(preset) },
                    label = { Text(presetLabel(preset)) },
                )
            }
        }
    }
}

private fun presetLabel(preset: DownloadPreset): String = when (preset) {
    DownloadPreset.BEST -> DownloaderApp.str(R.string.download_preset_best)
    DownloadPreset.AUDIO -> DownloaderApp.str(R.string.download_preset_audio)
    DownloadPreset.STORAGE -> DownloaderApp.str(R.string.download_preset_storage)
    DownloadPreset.BALANCED -> DownloaderApp.str(R.string.download_preset_balanced)
}
