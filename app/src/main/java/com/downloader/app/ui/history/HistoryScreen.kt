package com.downloader.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.data.HistoryEntry
import com.downloader.app.data.HistoryRepository
import com.downloader.app.data.HistoryStatus
import com.downloader.app.ui.components.DownloaderChip
import com.downloader.app.ui.components.DownloaderSearchField
import com.downloader.app.ui.components.EmptyStateBox
import com.downloader.app.ui.components.HistoryItem
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderSpacing
import com.downloader.app.ui.theme.DownloaderText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class HistoryFilter { ALL, VIDEO, AUDIO, PHOTO, FAILED, PLAYLIST }

private fun categoryOf(entry: HistoryEntry): HistoryFilter {
    if (entry.status == HistoryStatus.FAILED) return HistoryFilter.FAILED
    val url = entry.url.lowercase()
    if (entry.zipArchive || "list=" in url || "/playlist" in url || "/sets/" in url) {
        return HistoryFilter.PLAYLIST
    }
    return when {
        entry.format == "photo" || entry.mimeType.startsWith("image/") -> HistoryFilter.PHOTO
        entry.format == "mp3" || entry.format == "m4a" || entry.mimeType.startsWith("audio/") ->
            HistoryFilter.AUDIO

        else -> HistoryFilter.VIDEO
    }
}

private fun filterLabel(filter: HistoryFilter): String = when (filter) {
    HistoryFilter.ALL -> DownloaderApp.str(R.string.history_filter_all)
    HistoryFilter.VIDEO -> DownloaderApp.str(R.string.history_filter_video)
    HistoryFilter.AUDIO -> DownloaderApp.str(R.string.history_filter_audio)
    HistoryFilter.PHOTO -> DownloaderApp.str(R.string.history_filter_photo)
    HistoryFilter.FAILED -> DownloaderApp.str(R.string.history_filter_failed)
    HistoryFilter.PLAYLIST -> DownloaderApp.str(R.string.history_filter_playlist)
}

/** Label grup hari: "Hari ini", "Kemarin", atau tanggal penuh. */
private fun dayLabel(millis: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    if (sameYear && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
        return DownloaderApp.str(R.string.history_group_today)
    }
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (sameYear && yesterday.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
        return DownloaderApp.str(R.string.history_group_yesterday)
    }
    return SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(millis))
}

/**
 * Riwayat Download — mengikuti frame desain "Screen / 07 — Riwayat Download"
 * (14:268) beserta overlay menu (15:2) dan dialog konfirmasi (15:23).
 */
@Composable
fun HistoryScreen(
    onClose: () -> Unit,
    onRetry: (HistoryEntry) -> Unit,
) {
    val entries by HistoryRepository.entries.collectAsStateWithLifecycle()
    var clearDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL.name) }
    val activeFilter = HistoryFilter.values().firstOrNull { it.name == filter } ?: HistoryFilter.ALL

    val filtered = remember(entries, query, activeFilter) {
        val q = query.trim()
        entries.filter { entry ->
            (activeFilter == HistoryFilter.ALL || categoryOf(entry) == activeFilter) &&
                (q.isEmpty() || entry.title.contains(q, true) || entry.fileName.contains(q, true))
        }
    }

    val grouped = remember(filtered) {
        val map = LinkedHashMap<String, MutableList<HistoryEntry>>()
        filtered.forEach { entry ->
            map.getOrPut(dayLabel(entry.createdAt)) { mutableListOf() }.add(entry)
        }
        map.entries.toList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = DownloaderApp.str(R.string.action_back),
                )
            }
            Text(
                text = DownloaderApp.str(R.string.history_title),
                style = DownloaderText.screenTitleSm,
                color = DownloaderColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
                IconButton(onClick = { clearDialog = true }) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = DownloaderApp.str(R.string.history_clear_all),
                        tint = DownloaderColors.TextMuted,
                    )
                }
            }
        }

        Text(
            text = DownloaderApp.str(R.string.history_subtitle),
            style = DownloaderText.body,
            color = DownloaderColors.TextSecondary,
            modifier = Modifier.padding(horizontal = DownloaderSpacing.screen),
        )

        Spacer(Modifier.height(16.dp))

        if (entries.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = DownloaderSpacing.screen)) {
                DownloaderSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = DownloaderApp.str(R.string.history_search_hint),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DownloaderSpacing.sm),
                ) {
                    HistoryFilter.values().forEach { option ->
                        DownloaderChip(
                            label = filterLabel(option),
                            selected = activeFilter == option,
                            onClick = { filter = option.name },
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            entries.isEmpty() -> EmptyStateBox(
                icon = Icons.Rounded.History,
                title = DownloaderApp.str(R.string.history_empty_title),
                description = DownloaderApp.str(R.string.history_empty_desc),
                modifier = Modifier.fillMaxWidth(),
            )

            filtered.isEmpty() -> EmptyStateBox(
                icon = Icons.Rounded.SearchOff,
                title = DownloaderApp.str(R.string.history_no_results_title),
                description = DownloaderApp.str(R.string.history_no_results_desc),
                modifier = Modifier.fillMaxWidth(),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                grouped.forEach { (label, dayEntries) ->
                    item(key = "day_$label") {
                        Text(
                            text = label,
                            style = DownloaderText.sectionTitleSm,
                            color = DownloaderColors.TextPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(dayEntries, key = { it.id }) { entry ->
                        HistoryItem(
                            entry = entry,
                            onRetry = onRetry,
                            onDelete = { pendingDelete = it },
                        )
                    }
                }
            }
        }
    }

    if (clearDialog) {
        ConfirmDialog(
            title = DownloaderApp.str(R.string.history_clear_confirm_title),
            body = DownloaderApp.str(R.string.history_clear_confirm_desc),
            confirmLabel = DownloaderApp.str(R.string.history_clear_all),
            onConfirm = {
                clearDialog = false
                HistoryRepository.clear()
            },
            onDismiss = { clearDialog = false },
        )
    }

    val deleteTarget = pendingDelete
    if (deleteTarget != null) {
        ConfirmDialog(
            title = DownloaderApp.str(R.string.history_delete_confirm_title),
            body = DownloaderApp.str(R.string.history_delete_confirm_desc),
            confirmLabel = DownloaderApp.str(R.string.action_delete),
            onConfirm = {
                HistoryRepository.remove(deleteTarget.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/**
 * Dialog konfirmasi desain (345×222, radius 22, fill Elevated):
 * tile peringatan 44, judul 19·700, isi 11·400, tombol 34 (batal abu, aksi merah).
 */
@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(DownloaderRadius.dialog),
            color = DownloaderColors.SurfaceElevated,
            border = BorderStroke(1.dp, DownloaderColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = DownloaderSpacing.xl,
                    end = DownloaderSpacing.xl,
                    top = DownloaderSpacing.xl,
                    bottom = 15.dp,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(DownloaderRadius.row))
                        .background(DownloaderColors.DangerSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = DownloaderColors.Danger,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = title,
                    style = DownloaderText.dialogTitle,
                    color = DownloaderColors.TextPrimary,
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    text = body,
                    style = DownloaderText.body,
                    color = DownloaderColors.TextSecondary,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DialogButton(
                        label = DownloaderApp.str(R.string.action_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(104f),
                    )
                    Spacer(Modifier.width(DownloaderSpacing.sm))
                    DialogButton(
                        label = confirmLabel,
                        onClick = onConfirm,
                        danger = true,
                        modifier = Modifier.weight(193f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DownloaderRadius.tile),
        color = if (danger) DownloaderColors.Danger else DownloaderColors.Surface,
        border = if (danger) null else BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier.height(34.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = if (danger) {
                    DownloaderText.rowTitleSm.copy(fontWeight = FontWeight.Bold)
                } else {
                    DownloaderText.rowTitleSm
                },
                color = if (danger) Color.White else DownloaderColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}
