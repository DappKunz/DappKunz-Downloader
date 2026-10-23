package com.downloader.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.util.LocaleHelper
import com.downloader.app.ui.components.AnimatedBottomSheet
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderText

/**
 * Bottom sheet pilihan bahasa — mengikuti frame
 * `Overlay / Custom Language Picker` di Figma.
 */
@Composable
fun LanguageSheet(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var pending by remember { mutableStateOf(currentLanguage) }
    var applyOnClose by remember { mutableStateOf<String?>(null) }

    AnimatedBottomSheet(
        title = DownloaderApp.str(R.string.language_sheet_title),
        onDismiss = {
            val next = applyOnClose
            if (next != null && next != currentLanguage) {
                onApply(next)
            } else {
                onDismiss()
            }
        },
        sheetHeight = 320.dp,
        footer = { close ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            ) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (pending == currentLanguage) {
                            close()
                        } else {
                            applyOnClose = pending
                            close()
                        }
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
                        text = DownloaderApp.str(R.string.language_sheet_apply),
                        style = DownloaderText.button,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        },
    ) { _ ->
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

            Text(
                text = DownloaderApp.str(R.string.language_sheet_title),
                style = DownloaderText.sheetTitle,
                color = DownloaderColors.AppearanceText,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = DownloaderApp.str(R.string.language_sheet_desc),
                style = DownloaderText.body,
                color = DownloaderColors.AppearanceTextSecondary,
            )

            Spacer(Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOption(
                    label = DownloaderApp.str(R.string.language_indonesian),
                    selected = pending == LocaleHelper.LANG_INDONESIAN,
                    onClick = { pending = LocaleHelper.LANG_INDONESIAN },
                )
                LanguageOption(
                    label = DownloaderApp.str(R.string.language_english),
                    selected = pending == LocaleHelper.LANG_ENGLISH,
                    onClick = { pending = LocaleHelper.LANG_ENGLISH },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) DownloaderColors.PrimarySoft else DownloaderColors.AppearanceField,
        animationSpec = tween(180),
        label = "languageOptionBackground",
    )
    val border by animateColorAsState(
        targetValue = if (selected) DownloaderColors.Primary else DownloaderColors.AppearanceBorder,
        animationSpec = tween(180),
        label = "languageOptionBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = DownloaderColors.Primary,
                unselectedColor = DownloaderColors.AppearanceTextSecondary,
            ),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = DownloaderText.button,
            fontWeight = FontWeight.Medium,
            color = DownloaderColors.AppearanceText,
        )
        Spacer(Modifier.weight(1f))
    }
}
