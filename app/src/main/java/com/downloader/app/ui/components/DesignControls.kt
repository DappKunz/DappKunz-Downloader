package com.downloader.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderDimens
import com.downloader.app.ui.theme.DownloaderRadius
import com.downloader.app.ui.theme.DownloaderText

/**
 * Chip filter desain (tinggi 31, radius pill).
 *
 * Aktif: border `#3B82F6` + fill Soft blue + teks `#D8E8FF`.
 * Non-aktif: fill Surface + border `#2B3242` + teks sekunder.
 */
@Composable
fun DownloaderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fill by animateColorAsState(
        targetValue = if (selected) DownloaderColors.PrimarySoft else DownloaderColors.Surface,
        animationSpec = tween(220),
        label = "chipFill",
    )
    val stroke by animateColorAsState(
        targetValue = if (selected) DownloaderColors.Primary else DownloaderColors.Border,
        animationSpec = tween(220),
        label = "chipStroke",
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            !enabled -> DownloaderColors.TextMuted
            selected -> DownloaderColors.TextOnSoft
            else -> DownloaderColors.TextSecondary
        },
        animationSpec = tween(200),
        label = "chipLabel",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(DownloaderRadius.pill),
        color = fill,
        border = BorderStroke(width = 1.dp, color = stroke),
        modifier = modifier
            .height(DownloaderDimens.chipHeight)
            .pressScale(scaleDown = 0.93f),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = DownloaderText.chip,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Kolom pencarian desain: tinggi 42, radius 14, fill Background `#080A10`,
 * border `#2B3242`, ikon 19 dan placeholder 11·400.
 */
@Composable
fun DownloaderSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(DownloaderRadius.field)
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) DownloaderColors.Primary else DownloaderColors.Border,
        animationSpec = tween(220),
        label = "searchBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 1.5.dp else 1.dp,
        animationSpec = tween(220),
        label = "searchBorderWidth",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DownloaderDimens.searchHeight)
            .clip(shape)
            .background(DownloaderColors.Background)
            .border(borderWidth, borderColor, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = DownloaderColors.TextSecondary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = DownloaderText.rowTitleSm.copy(color = DownloaderColors.TextPrimary),
            cursorBrush = SolidColor(DownloaderColors.Primary),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = DownloaderText.rowTitleSm,
                            color = DownloaderColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(DownloaderRadius.pill))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = DownloaderColors.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
