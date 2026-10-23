package com.downloader.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.util.Formatters
import com.downloader.app.ui.components.MotionDialog
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderText

@Composable
fun UpdateScreen(
    onClose: () -> Unit,
    viewModel: UpdateViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.maybeAutoCheck()
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
                text = DownloaderApp.str(R.string.upd_title),
                style = DownloaderText.screenTitleSm,
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
                text = DownloaderApp.str(R.string.upd_subtitle),
                style = DownloaderText.body,
                color = DownloaderColors.TextSecondary,
                modifier = Modifier.entrance(0, offsetY = 10),
            )

            CurrentVersionCard(
                versionName = state.installedVersionName,
                lastCheckedAt = state.lastCheckedAt,
                modifier = Modifier.entrance(1, offsetY = 12),
            )

            Text(
                text = DownloaderApp.str(R.string.upd_section_check),
                style = DownloaderText.sectionTitle,
                color = DownloaderColors.TextPrimary,
                modifier = Modifier.entrance(2, offsetY = 8),
            )

            StatusCard(
                state = state,
                modifier = Modifier.entrance(2, offsetY = 12),
            )

            ButtonActions(
                state = state,
                onCheck = { viewModel.checkForUpdates(manual = true) },
                onDownload = viewModel::downloadUpdate,
                onCancel = viewModel::cancelDownload,
                onInstall = viewModel::installDownloaded,
                modifier = Modifier.entrance(3, offsetY = 10),
            )

            if (state.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = DownloaderColors.DangerSoft,
                    border = BorderStroke(1.dp, DownloaderColors.Danger.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(4, offsetY = 10),
                ) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        style = DownloaderText.body,
                        color = DownloaderColors.Danger,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            if (state.releaseNotes != null) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = DownloaderColors.Surface,
                    border = BorderStroke(1.dp, DownloaderColors.Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .entrance(4, offsetY = 10),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = DownloaderApp.str(R.string.upd_notes),
                            style = DownloaderText.groupTitle,
                            color = DownloaderColors.TextMuted,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.releaseNotes.orEmpty(),
                            style = DownloaderText.body,
                            color = DownloaderColors.TextSecondary,
                        )
                    }
                }
            }

            TextButton(
                onClick = viewModel::openReleasePage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .entrance(5, offsetY = 8),
            ) {
                Text(DownloaderApp.str(R.string.upd_open_release))
            }
        }
    }

    MotionDialog(
        visible = state.needsInstallPermission,
        onDismiss = viewModel::dismissInstallPermission,
    ) {
        Text(
            text = DownloaderApp.str(R.string.upd_permission_title),
            style = DownloaderText.dialogTitle,
            color = DownloaderColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = DownloaderApp.str(R.string.upd_permission_desc),
            style = DownloaderText.body,
            color = DownloaderColors.TextSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = viewModel::dismissInstallPermission) {
                Text(DownloaderApp.str(R.string.action_cancel))
            }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = viewModel::openInstallPermissionSettings) {
                Text(
                    text = DownloaderApp.str(R.string.upd_permission_action),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Figma's 353×152 current-version card. */
@Composable
private fun CurrentVersionCard(
    versionName: String,
    lastCheckedAt: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DownloaderColors.PrimarySoft,
        border = BorderStroke(1.dp, DownloaderColors.BorderSoftBlue),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DownloaderColors.Primary.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = DownloaderColors.TextOnSoft,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DownloaderApp.str(R.string.upd_version_label),
                        style = DownloaderText.metaSm,
                        color = DownloaderColors.TextSecondary,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = versionName,
                        style = DownloaderText.screenTitle,
                        color = DownloaderColors.TextPrimary,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (lastCheckedAt > 0) {
                    DownloaderApp.str(
                        R.string.upd_last_checked,
                        Formatters.dateTime(lastCheckedAt),
                    )
                } else {
                    DownloaderApp.str(R.string.upd_version_note)
                },
                style = DownloaderText.metaSm,
                color = DownloaderColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StatusCard(
    state: UpdateUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor(state.status)),
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnimatedContent(
                    targetState = state.status,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 4 }) togetherWith
                            (fadeOut(tween(150)) + slideOutVertically(tween(190)) { -it / 5 })
                    },
                    label = "updateStatus",
                ) { status ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = statusTitle(status),
                            style = DownloaderText.rowTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = DownloaderColors.TextPrimary,
                        )
                        when (status) {
                            UpdateStatus.CHECKING -> Text(
                                text = DownloaderApp.str(R.string.upd_checking),
                                style = DownloaderText.metaSm,
                                color = DownloaderColors.TextSecondary,
                            )

                            UpdateStatus.IDLE -> Text(
                                text = DownloaderApp.str(R.string.upd_idle),
                                style = DownloaderText.metaSm,
                                color = DownloaderColors.TextSecondary,
                            )

                            UpdateStatus.NO_RELEASE -> Text(
                                text = DownloaderApp.str(R.string.upd_no_release),
                                style = DownloaderText.metaSm,
                                color = DownloaderColors.TextSecondary,
                            )

                            UpdateStatus.UP_TO_DATE -> Text(
                                text = DownloaderApp.str(R.string.upd_up_to_date),
                                style = DownloaderText.metaSm,
                                color = DownloaderColors.TextSecondary,
                            )

                            UpdateStatus.UNCOMPARABLE -> {
                                state.latestVersion?.let {
                                    Text(
                                        text = DownloaderApp.str(R.string.upd_latest, it),
                                        style = DownloaderText.metaSm,
                                        color = DownloaderColors.TextSecondary,
                                    )
                                }
                                Text(
                                    text = DownloaderApp.str(R.string.upd_uncomparable),
                                    style = DownloaderText.metaSm,
                                    color = DownloaderColors.TextSecondary,
                                )
                            }

                            UpdateStatus.ERROR -> Text(
                                text = DownloaderApp.str(R.string.upd_error_retry_hint),
                                style = DownloaderText.metaSm,
                                color = DownloaderColors.TextSecondary,
                            )

                            UpdateStatus.UPDATE_AVAILABLE -> {
                                state.latestVersion?.let {
                                    Text(
                                        text = DownloaderApp.str(R.string.upd_latest, it),
                                        style = DownloaderText.metaSm,
                                        color = DownloaderColors.TextSecondary,
                                    )
                                }
                                state.releaseDate?.let {
                                    Text(
                                        text = DownloaderApp.str(R.string.upd_release_date, it),
                                        style = DownloaderText.metaSm,
                                        color = DownloaderColors.TextSecondary,
                                    )
                                }
                                if (state.asset == null) {
                                    Text(
                                        text = DownloaderApp.str(R.string.upd_no_apk),
                                        style = DownloaderText.metaSm,
                                        color = DownloaderColors.Danger,
                                    )
                                }
                            }

                            UpdateStatus.DOWNLOADING -> {
                                Text(
                                    text = if (state.totalBytes > 0) {
                                        DownloaderApp.str(
                                            R.string.upd_downloading,
                                            ((state.downloadedBytes * 100) / state.totalBytes).toInt(),
                                        )
                                    } else {
                                        DownloaderApp.str(
                                            R.string.upd_downloading_unknown,
                                            Formatters.bytes(state.downloadedBytes),
                                        )
                                    },
                                    style = DownloaderText.metaSm,
                                    color = DownloaderColors.TextSecondary,
                                )
                                if (state.totalBytes > 0) {
                                    LinearProgressIndicator(
                                        progress = {
                                            (state.downloadedBytes.toFloat() / state.totalBytes.toFloat())
                                                .coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }

                            UpdateStatus.DOWNLOADED -> Text(
                                text = DownloaderApp.str(R.string.upd_downloaded_ready),
                                style = DownloaderText.metaSm,
                                color = DownloaderColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun statusTitle(status: UpdateStatus): String = DownloaderApp.str(
    when (status) {
        UpdateStatus.CHECKING -> R.string.upd_status_checking
        UpdateStatus.IDLE -> R.string.upd_status_idle
        UpdateStatus.NO_RELEASE -> R.string.upd_status_no_release
        UpdateStatus.UP_TO_DATE -> R.string.upd_status_latest
        UpdateStatus.UNCOMPARABLE -> R.string.upd_status_uncomparable
        UpdateStatus.ERROR -> R.string.upd_status_error
        UpdateStatus.UPDATE_AVAILABLE -> R.string.upd_available
        UpdateStatus.DOWNLOADING -> R.string.upd_status_downloading
        UpdateStatus.DOWNLOADED -> R.string.upd_status_downloaded
    },
)

private fun statusColor(status: UpdateStatus): Color = when (status) {
    UpdateStatus.ERROR -> DownloaderColors.Danger
    UpdateStatus.UPDATE_AVAILABLE,
    UpdateStatus.UP_TO_DATE,
    UpdateStatus.DOWNLOADED,
    -> DownloaderColors.Success
    UpdateStatus.CHECKING,
    UpdateStatus.DOWNLOADING,
    -> DownloaderColors.Primary
    else -> DownloaderColors.TextMuted
}

@Composable
private fun ButtonActions(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state.status) {
            UpdateStatus.CHECKING -> Unit

            UpdateStatus.DOWNLOADING -> {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pressScale(scaleDown = 0.97f),
                ) {
                    Text(DownloaderApp.str(R.string.upd_download_cancel))
                }
            }

            UpdateStatus.DOWNLOADED -> {
                Button(
                    onClick = onInstall,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pressScale(scaleDown = 0.97f),
                ) {
                    Text(DownloaderApp.str(R.string.upd_install))
                }
            }

            UpdateStatus.UPDATE_AVAILABLE -> {
                val asset = state.asset
                if (asset != null) {
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(15.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .pressScale(scaleDown = 0.97f),
                    ) {
                        Text(
                            if (asset.size > 0) {
                                DownloaderApp.str(R.string.upd_download, Formatters.bytes(asset.size))
                            } else {
                                DownloaderApp.str(R.string.upd_download_simple)
                            },
                        )
                    }
                }
            }

            else -> Unit
        }

        if (state.status != UpdateStatus.CHECKING && state.status != UpdateStatus.DOWNLOADING) {
            Button(
                onClick = onCheck,
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pressScale(scaleDown = 0.97f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.status == UpdateStatus.ERROR) {
                        DownloaderApp.str(R.string.upd_retry)
                    } else {
                        DownloaderApp.str(R.string.upd_check)
                    },
                )
            }
        }
    }
}
