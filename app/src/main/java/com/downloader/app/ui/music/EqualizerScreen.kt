package com.downloader.app.ui.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.music.EqPresets
import com.downloader.app.core.music.EqualizerController
import com.downloader.app.ui.components.DownloaderChip
import com.downloader.app.ui.components.entrance
import com.downloader.app.ui.components.pressScale
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderText
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(onClose: () -> Unit) {
    val state by EqualizerController.state.collectAsState()
    val controlsActive = state.available && state.enabled

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .entrance(0, offsetY = 8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = DownloaderApp.str(R.string.action_back),
                )
            }
            Text(
                text = DownloaderApp.str(R.string.eq_title),
                style = DownloaderText.screenTitle,
                color = DownloaderColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.enabled,
                onCheckedChange = EqualizerController::setEnabled,
                enabled = state.available,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.available) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = DownloaderColors.DangerSoft,
                    border = BorderStroke(1.dp, DownloaderColors.Danger.copy(alpha = 0.3f)),
                    modifier = Modifier.entrance(1),
                ) {
                    Text(
                        text = state.message ?: DownloaderApp.str(R.string.eq_unavailable),
                        style = DownloaderText.body,
                        color = DownloaderColors.Danger,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Text(
                text = DownloaderApp.str(R.string.eq_preset),
                style = DownloaderText.sectionTitle,
                color = DownloaderColors.TextPrimary,
                modifier = Modifier.entrance(1, offsetY = 8),
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .entrance(2, offsetY = 8),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EqPresets.BUILT_IN.forEach { preset ->
                    DownloaderChip(
                        label = preset,
                        selected = state.preset == preset,
                        onClick = { EqualizerController.applyPreset(preset) },
                    )
                }
                if (state.preset == EqPresets.CUSTOM) {
                    DownloaderChip(
                        label = EqPresets.CUSTOM,
                        selected = true,
                        onClick = { },
                    )
                }
            }

            if (state.bands.isEmpty()) {
                Text(
                    text = DownloaderApp.str(R.string.eq_no_bands),
                    style = DownloaderText.body,
                    color = DownloaderColors.TextSecondary,
                )
            } else {
                state.bands.forEachIndexed { index, band ->
                    val gain = state.gainsDb.getOrNull(band.index) ?: 0
                    BandRow(
                        centerHz = band.centerHz,
                        gainDb = gain,
                        minDb = band.minDb,
                        maxDb = band.maxDb,
                        enabled = controlsActive,
                        onGain = { EqualizerController.setGain(band.index, it) },
                        modifier = Modifier.entrance(index.coerceAtMost(6) + 3, offsetY = 8),
                    )
                }
            }

            OutlinedButton(
                onClick = EqualizerController::reset,
                enabled = state.available,
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .pressScale(scaleDown = 0.97f)
                    .entrance(8, offsetY = 8),
            ) {
                Text(DownloaderApp.str(R.string.eq_reset))
            }
        }
    }
}

@Composable
private fun BandRow(
    centerHz: Int,
    gainDb: Int,
    minDb: Int,
    maxDb: Int,
    enabled: Boolean,
    onGain: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = DownloaderColors.Surface,
        border = BorderStroke(1.dp, DownloaderColors.Border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatHz(centerHz),
                    style = DownloaderText.rowTitle,
                    color = DownloaderColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDb(gainDb),
                    style = DownloaderText.meta,
                    color = DownloaderColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(2.dp))
            Slider(
                value = gainDb.toFloat(),
                onValueChange = { onGain(it.toInt()) },
                valueRange = minDb.toFloat()..maxDb.toFloat(),
                steps = (maxDb - minDb - 1).coerceAtLeast(0),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = DownloaderColors.Primary,
                    activeTrackColor = DownloaderColors.Primary,
                    inactiveTrackColor = DownloaderColors.Border,
                    disabledThumbColor = DownloaderColors.TextMuted,
                    disabledActiveTrackColor = DownloaderColors.Border,
                    disabledInactiveTrackColor = DownloaderColors.Border,
                ),
            )
        }
    }
}

private fun formatHz(hz: Int): String = if (hz >= 1000) {
    if (hz % 1000 == 0) {
        "${hz / 1000} kHz"
    } else {
        String.format(Locale.getDefault(), "%.1f kHz", hz / 1000f)
    }
} else {
    "$hz Hz"
}

private fun formatDb(db: Int): String = when {
    db > 0 -> "+$db dB"
    db < 0 -> "$db dB"
    else -> "0 dB"
}
