package com.downloader.app.ui.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
import com.downloader.app.ui.components.MotionDialog
import com.downloader.app.ui.components.PlaylistSection
import com.downloader.app.ui.components.PrimaryDownloadButton
import com.downloader.app.ui.components.QualitySection
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale

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
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .entrance(0, offsetY = 10),
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
                modifier = Modifier.entrance(0, offsetY = 10),
            )

            UrlField(
                value = analyze.url,
                onValueChange = viewModel::onUrlChange,
                loading = analyze.loading,
                ready = analyze.meta != null,
                onPaste = {
                    val text = clipboard.getText()?.text
                    if (text.isNullOrBlank()) {
                        viewModel.onClipboardEmpty()
                    } else {
                        viewModel.onUrlChange(text.trim())
                    }
                },
                onClear = viewModel::onUrlClear,
                onAnalyze = viewModel::runAnalyze,
                modifier = Modifier.entrance(1, offsetY = 12),
            )

            AnimatedVisibility(
                visible = analyze.meta == null && !analyze.loading,
                enter = fadeIn(tween(200)) + expandVertically(tween(240)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(190)),
            ) {
                Button(
                    onClick = viewModel::runAnalyze,
                    enabled = analyze.url.isNotBlank() && !analyze.loading,
                    shape = RoundedCornerShape(DownloaderRadius.card),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .pressScale(scaleDown = 0.97f),
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        DownloaderApp.str(R.string.action_analyze),
                        style = DownloaderText.button,
                    )
                }
            }

            Text(
                text = DownloaderApp.str(R.string.supported_platforms),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.entrance(2, offsetY = 8),
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
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.entrance(3, offsetY = 12),
                    ) {
                        MediaPreview(meta = meta)
                    }

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
                            modifier = Modifier.entrance(4, offsetY = 10),
                        )
                    }

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.entrance(5, offsetY = 10),
                    ) {
                        FormatPills(
                            meta = meta,
                            selected = viewModel.selectedFormat,
                            onSelect = viewModel::selectFormat,
                        )
                    }

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.entrance(6, offsetY = 10),
                    ) {
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
                    }

                    val playlist = meta.playlist
                    if (playlist != null) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.entrance(7, offsetY = 10),
                        ) {
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
                        }
                    } else {
                        val estimate = viewModel.singleEstimateBytes
                        if (estimate != null && estimate > 0) {
                            Text(
                                text = DownloaderApp.str(R.string.estimate_size, Formatters.bytes(estimate)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.entrance(7, offsetY = 8),
                            )
                        }
                    }

                    val needsSelection = playlist != null && playlist.items.isNotEmpty()
                    val hint = if (needsSelection && viewModel.selectedItems.isEmpty()) {
                        DownloaderApp.str(R.string.playlist_need_selection)
                    } else {
                        null
                    }

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.entrance(8, offsetY = 12),
                    ) {
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
    }

    MotionDialog(
        visible = showStorageConfirm,
        onDismiss = { showStorageConfirm = false },
    ) {
        Text(
            text = DownloaderApp.str(R.string.download_preset_storage_confirm_title),
            style = DownloaderText.dialogTitle,
            color = DownloaderColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = DownloaderApp.str(R.string.download_preset_storage_confirm_desc),
            style = DownloaderText.body,
            color = DownloaderColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { showStorageConfirm = false }) {
                Text(DownloaderApp.str(R.string.action_cancel))
            }
            Spacer(Modifier.width(4.dp))
            TextButton(
                onClick = {
                    showStorageConfirm = false
                    viewModel.applyPreset(DownloadPreset.STORAGE)
                },
            ) {
                Text(
                    text = DownloaderApp.str(R.string.download_preset_storage_confirm_action),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Figma's 353×66 link-preview card, adapted into an editable field: leading
 * link glyph, URL, status line, paste/clear actions and an inline re-analyze
 * action that remains available after a result is shown.
 */
@Composable
private fun UrlField(
    value: String,
    onValueChange: (String) -> Unit,
    loading: Boolean,
    ready: Boolean,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(15.dp)
    val border by animateColorAsState(
        targetValue = if (focused) DownloaderColors.Primary else DownloaderColors.Border,
        animationSpec = tween(220),
        label = "urlBorder",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(shape)
            .background(DownloaderColors.Surface)
            .border(1.dp, border, shape)
            .padding(start = 13.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Link,
            contentDescription = null,
            tint = DownloaderColors.Primary,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = DownloaderText.rowTitle.copy(color = DownloaderColors.TextPrimary),
                cursorBrush = SolidColor(DownloaderColors.Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onAnalyze() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    androidx.compose.foundation.layout.Box {
                        if (value.isEmpty()) {
                            Text(
                                text = DownloaderApp.str(R.string.url_hint),
                                style = DownloaderText.rowTitle,
                                color = DownloaderColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    loading -> DownloaderApp.str(R.string.new_download_analyzing)
                    ready -> DownloaderApp.str(R.string.url_status_ready)
                    else -> DownloaderApp.str(R.string.url_status_hint)
                },
                style = DownloaderText.metaSm,
                color = if (ready) DownloaderColors.Success else DownloaderColors.TextSecondary,
                maxLines = 1,
            )
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(18.dp),
                strokeWidth = 2.dp,
                color = DownloaderColors.Primary,
            )
        } else {
            IconButton(
                onClick = onAnalyze,
                enabled = value.isNotBlank(),
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = DownloaderApp.str(R.string.action_analyze),
                    tint = if (value.isNotBlank()) DownloaderColors.Primary else DownloaderColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (value.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = DownloaderApp.str(R.string.action_clear_link),
                    tint = DownloaderColors.TextMuted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        IconButton(
            onClick = onPaste,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentPaste,
                contentDescription = DownloaderApp.str(R.string.action_paste),
                tint = DownloaderColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PresetRow(
    selected: DownloadPreset?,
    onSelect: (DownloadPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
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
