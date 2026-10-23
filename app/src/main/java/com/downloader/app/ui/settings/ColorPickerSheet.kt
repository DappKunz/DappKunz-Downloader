package com.downloader.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.ui.components.AnimatedBottomSheet
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderText
import com.downloader.app.ui.theme.parseHexColor
import java.util.Locale

/**
 * Color picker HSV tanpa dependency baru. Pengguna menggeser area
 * saturasi/kecerahan dan slider hue; kode HEX hanya ditampilkan sebagai hasil.
 */
@Composable
fun ColorPickerSheet(
    initialHex: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    val initialColor = parseHexColor(initialHex) ?: Color(0xFF3B82F6)
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }

    var hue by rememberSaveable { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(initialHsv[2]) }
    var applyOnClose by remember { mutableStateOf(false) }

    val selectedColor = Color(
        android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)),
    )
    val selectedHex = remember(selectedColor) {
        String.format(Locale.US, "#%06X", selectedColor.toArgb() and 0xFFFFFF)
    }

    fun setColor(color: Color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
    }

    AnimatedBottomSheet(
        title = DownloaderApp.str(R.string.appearance_custom_color_title),
        onDismiss = {
            if (applyOnClose) onApply(selectedHex) else onDismiss()
        },
        sheetHeight = 600.dp,
        footer = { close ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            ) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        applyOnClose = true
                        close()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(49.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DownloaderColors.Primary,
                        contentColor = DownloaderColors.OnPrimary,
                    ),
                ) {
                    Text(
                        text = DownloaderApp.str(R.string.appearance_custom_color_apply),
                        style = DownloaderText.button,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        },
    ) { close ->
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
                    .background(DownloaderColors.AppearanceHandle),
            )
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DownloaderApp.str(R.string.appearance_custom_color_title),
                    style = DownloaderText.sheetTitle,
                    color = DownloaderColors.AppearanceText,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = close) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = DownloaderApp.str(R.string.action_close),
                        tint = DownloaderColors.AppearanceTextSecondary,
                    )
                }
            }

            Text(
                text = DownloaderApp.str(R.string.appearance_custom_color_desc),
                style = DownloaderText.body,
                color = DownloaderColors.AppearanceTextSecondary,
            )
            Spacer(Modifier.height(16.dp))

            SaturationValuePicker(
                hue = hue,
                saturation = saturation,
                brightness = brightness,
                onColorChange = { nextSaturation, nextBrightness ->
                    saturation = nextSaturation
                    brightness = nextBrightness
                },
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = DownloaderApp.str(R.string.appearance_color_picker_hue),
                style = DownloaderText.meta,
                fontWeight = FontWeight.Bold,
                color = DownloaderColors.AppearanceTextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            HueSlider(hue = hue, onHueChange = { hue = it })

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(selectedColor)
                        .border(1.dp, DownloaderColors.AppearanceBorder, RoundedCornerShape(12.dp)),
                )
                Column {
                    Text(
                        text = DownloaderApp.str(R.string.appearance_color_picker_hex),
                        style = DownloaderText.meta,
                        color = DownloaderColors.AppearanceTextSecondary,
                    )
                    Text(
                        text = selectedHex,
                        style = DownloaderText.button,
                        fontWeight = FontWeight.Bold,
                        color = DownloaderColors.AppearanceText,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = DownloaderApp.str(R.string.appearance_color_picker_presets),
                style = DownloaderText.meta,
                fontWeight = FontWeight.Bold,
                color = DownloaderColors.AppearanceTextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PalettePresetColors.forEach { preset ->
                    val presetColor = Color(preset)
                    val presetHex = String.format(Locale.US, "#%06X", preset and 0xFFFFFF)
                    val selected = selectedHex.equals(presetHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(presetColor)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) {
                                    DownloaderColors.AppearanceText
                                } else {
                                    DownloaderColors.AppearanceBorder
                                },
                                shape = CircleShape,
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { setColor(presetColor) },
                            )
                            .semantics { contentDescription = presetHex },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SaturationValuePicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onColorChange: (Float, Float) -> Unit,
) {
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    val update by rememberUpdatedState(onColorChange)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DownloaderColors.AppearanceBorder, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    update(
                        (down.position.x / size.width).coerceIn(0f, 1f),
                        (1f - down.position.y / size.height).coerceIn(0f, 1f),
                    )
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        update(
                            (change.position.x / size.width).coerceIn(0f, 1f),
                            (1f - change.position.y / size.height).coerceIn(0f, 1f),
                        )
                        change.consume()
                        if (!change.pressed) break
                    }
                }
            }
            .semantics {
                contentDescription = DownloaderApp.str(R.string.appearance_color_picker_sv)
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        val radius = 9.dp.toPx()
        val travelX = (size.width - 2f * radius).coerceAtLeast(1f)
        val travelY = (size.height - 2f * radius).coerceAtLeast(1f)
        val center = Offset(
            x = radius + saturation.coerceIn(0f, 1f) * travelX,
            y = radius + (1f - brightness.coerceIn(0f, 1f)) * travelY,
        )
        drawCircle(Color.Black.copy(alpha = 0.35f), radius + 1.dp.toPx(), center)
        drawCircle(Color.White, radius, center, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
) {
    val update by rememberUpdatedState(onHueChange)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .border(1.dp, DownloaderColors.AppearanceBorder, RoundedCornerShape(11.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    update((down.position.x / size.width).coerceIn(0f, 1f) * 360f)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        update((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                        change.consume()
                        if (!change.pressed) break
                    }
                }
            }
            .semantics {
                contentDescription = DownloaderApp.str(R.string.appearance_color_picker_hue)
            },
    ) {
        drawRect(
            Brush.horizontalGradient(
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red,
                ),
            ),
        )
        val radius = 8.dp.toPx()
        val travel = (size.width - 2f * radius).coerceAtLeast(1f)
        val x = radius + (hue.coerceIn(0f, 360f) / 360f) * travel
        drawCircle(
            color = Color.White,
            radius = radius,
            center = Offset(x, size.height / 2f),
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = radius + 1.dp.toPx(),
            center = Offset(x, size.height / 2f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private val PalettePresetColors = intArrayOf(
    0xFF3B82F6.toInt(),
    0xFFF63B3B.toInt(),
    0xFF4BF63B.toInt(),
    0xFFFBBF24.toInt(),
    0xFFA855F7.toInt(),
    0xFFF97316.toInt(),
    0xFFEC4899.toInt(),
    0xFF06B6D4.toInt(),
)
