package com.downloader.app.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downloader.app.DownloaderApp
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.util.CacheManager
import com.downloader.app.core.util.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val language = PrefsRepository.language
    val folderUri = PrefsRepository.folderUri
    val turbo = PrefsRepository.turbo

    fun setTurbo(value: Boolean) {
        PrefsRepository.setTurbo(value)
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
