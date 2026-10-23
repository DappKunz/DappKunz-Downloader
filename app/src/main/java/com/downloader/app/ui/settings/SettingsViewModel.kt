package com.downloader.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.storage.AccentPalette
import com.downloader.app.core.storage.CustomFontManager
import com.downloader.app.core.storage.FontChoice
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.storage.ThemeMode
import com.downloader.app.core.storage.normalizeHexColor
import com.downloader.app.core.util.CacheManager
import com.downloader.app.core.util.LocaleHelper
import com.downloader.app.ui.theme.ArkunSansAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val language = PrefsRepository.language
    val folderUri = PrefsRepository.folderUri
    val turbo = PrefsRepository.turbo
    val themeMode = PrefsRepository.themeMode
    val accentPalette = PrefsRepository.accentPalette
    val customAccentHex = PrefsRepository.customAccentHex
    val fontChoice = PrefsRepository.fontChoice
    val customFontPath = PrefsRepository.customFontPath

    var fontImportBusy by mutableStateOf(false)
        private set

    fun setTurbo(value: Boolean) {
        PrefsRepository.setTurbo(value)
    }

    fun setThemeMode(value: ThemeMode) {
        PrefsRepository.setThemeMode(value)
    }

    fun setAccentPalette(value: AccentPalette) {
        PrefsRepository.setAccentPalette(value)
    }

    /** Mengembalikan false bila HEX tidak valid; preferensi lama dipertahankan. */
    fun setCustomAccentHex(value: String): Boolean {
        val normalized = normalizeHexColor(value) ?: return false
        PrefsRepository.setCustomAccentHex(normalized)
        PrefsRepository.setAccentPalette(AccentPalette.CUSTOM)
        return true
    }

    fun setFontChoice(value: FontChoice) {
        if (value == FontChoice.ARKUN_SANS && !ArkunSansAvailable) return
        if (value == FontChoice.CUSTOM && PrefsRepository.customFontPath.value.isNullOrBlank()) return
        PrefsRepository.setFontChoice(value)
    }

    fun importCustomFont(uri: Uri, onResult: (String) -> Unit) {
        if (fontImportBusy) return
        fontImportBusy = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                CustomFontManager.importFont(getApplication(), uri)
            }
            val message = when (result) {
                is CustomFontManager.ImportResult.Success -> {
                    PrefsRepository.setCustomFontPath(result.file.absolutePath)
                    PrefsRepository.setFontChoice(FontChoice.CUSTOM)
                    DownloaderApp.str(R.string.appearance_font_applied, result.displayName)
                }

                is CustomFontManager.ImportResult.Error -> DownloaderApp.str(
                    when (result.reason) {
                        CustomFontManager.Failure.TOO_LARGE -> R.string.appearance_font_too_large
                        CustomFontManager.Failure.INVALID_FORMAT,
                        CustomFontManager.Failure.EMPTY -> R.string.appearance_font_invalid

                        CustomFontManager.Failure.UNREADABLE -> R.string.appearance_font_unreadable
                    },
                )
            }
            fontImportBusy = false
            onResult(message)
        }
    }

    var cacheSizeBytes by mutableStateOf<Long?>(null)
        private set

    var cacheBusy by mutableStateOf(false)
        private set

    fun refreshCacheSize() {
        viewModelScope.launch {
            cacheSizeBytes = withContext(Dispatchers.IO) { CacheManager.sizeBytes(getApplication()) }
        }
    }

    fun clearCache(onDone: (Long) -> Unit) {
        if (cacheBusy) return
        cacheBusy = true
        viewModelScope.launch {
            val freed = withContext(Dispatchers.IO) { CacheManager.clear(getApplication()) }
            cacheSizeBytes = withContext(Dispatchers.IO) { CacheManager.sizeBytes(getApplication()) }
            cacheBusy = false
            onDone(freed)
        }
    }

    fun setLanguage(tag: String) {
        PrefsRepository.setLanguage(tag)
        LocaleHelper.invalidate()
        DownloaderApp.refreshNotificationChannel()
    }

    fun setFolderUri(uri: String?) {
        PrefsRepository.setFolderUri(getApplication(), uri)
    }

    fun folderDefaultLabel(): String = DownloaderApp.str(com.downloader.app.R.string.settings_folder_default)
}
