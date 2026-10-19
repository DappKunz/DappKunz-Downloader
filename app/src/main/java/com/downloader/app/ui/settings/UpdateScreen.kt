package com.downloader.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.util.Formatters
import com.downloader.app.ui.theme.DownloaderColors

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
                text = DownloaderApp.str(R.string.upd_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
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
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(DownloaderColors.Primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = DownloaderColors.Primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = DownloaderApp.str(R.string.upd_installed, state.installedVersionName),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.lastCheckedAt > 0) {
                            DownloaderApp.str(
                                R.string.upd_last_checked,
                                Formatters.dateTime(state.lastCheckedAt),
                            )
                        } else {
                            DownloaderApp.str(R.string.upd_never_checked)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            StatusCard(state)

            ButtonActions(
                state = state,
                onCheck = { viewModel.checkForUpdates(manual = true) },
                onDownload = viewModel::downloadUpdate,
                onCancel = viewModel::cancelDownload,
                onInstall = viewModel::installDownloaded,
            )

            if (state.errorMessage != null) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            if (state.releaseNotes != null) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = DownloaderColors.SurfaceElevated),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = DownloaderApp.str(R.string.upd_notes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.releaseNotes.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            TextButton(
                onClick = viewModel::openReleasePage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(DownloaderApp.str(R.string.upd_open_release))
            }
        }
    }

    if (state.needsInstallPermission) {
        AlertDialog(
            onDismissRequest = viewModel::dismissInstallPermission,
            title = { Text(DownloaderApp.str(R.string.upd_permission_title)) },
            text = { Text(DownloaderApp.str(R.string.upd_permission_desc)) },
            confirmButton = {
                TextButton(onClick = viewModel::openInstallPermissionSettings) {
                    Text(DownloaderApp.str(R.string.upd_permission_action))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissInstallPermission) {
                    Text(DownloaderApp.str(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun StatusCard(state: UpdateUiState) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = DownloaderColors.SurfaceElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (state.status) {
                UpdateStatus.CHECKING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(DownloaderApp.str(R.string.upd_checking))
                    }
                }

                UpdateStatus.IDLE -> {
                    Text(
                        text = DownloaderApp.str(R.string.upd_idle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                UpdateStatus.NO_RELEASE -> {
                    Text(
                        text = DownloaderApp.str(R.string.upd_no_release),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                UpdateStatus.UP_TO_DATE -> {
                    Text(
                        text = DownloaderApp.str(R.string.upd_up_to_date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }

                UpdateStatus.UNCOMPARABLE -> {
                    state.latestVersion?.let {
                        Text(
                            text = DownloaderApp.str(R.string.upd_latest, it),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Text(
                        text = DownloaderApp.str(R.string.upd_uncomparable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                UpdateStatus.ERROR -> {
                    Text(
                        text = DownloaderApp.str(R.string.upd_error_retry_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                UpdateStatus.UPDATE_AVAILABLE -> {
                    Text(
                        text = DownloaderApp.str(R.string.upd_available),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.latestVersion?.let {
                        Text(
                            text = DownloaderApp.str(R.string.upd_latest, it),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    state.releaseDate?.let {
                        Text(
                            text = DownloaderApp.str(R.string.upd_release_date, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.asset == null) {
                        Text(
                            text = DownloaderApp.str(R.string.upd_no_apk),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
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
                        style = MaterialTheme.typography.bodyMedium,
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

                UpdateStatus.DOWNLOADED -> {
                    Text(
                        text = DownloaderApp.str(R.string.upd_downloaded_ready),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonActions(
    state: UpdateUiState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state.status) {
            UpdateStatus.CHECKING -> Unit

            UpdateStatus.DOWNLOADING -> {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(DownloaderApp.str(R.string.upd_download_cancel))
                }
            }

            UpdateStatus.DOWNLOADED -> {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(DownloaderApp.str(R.string.upd_install))
                }
            }

            UpdateStatus.UPDATE_AVAILABLE -> {
                val asset = state.asset
                if (asset != null) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
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
            OutlinedButton(
                onClick = onCheck,
                modifier = Modifier.weight(1f),
            ) {
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
