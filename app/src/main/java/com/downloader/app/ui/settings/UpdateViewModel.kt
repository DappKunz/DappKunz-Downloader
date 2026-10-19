package com.downloader.app.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.update.ApkValidator
import com.downloader.app.core.update.GithubAsset
import com.downloader.app.core.update.GithubRelease
import com.downloader.app.core.update.UpdateChecker
import com.downloader.app.core.update.UpdateDownloader
import com.downloader.app.core.update.UpdateStatusResolver
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UpdateStatus {
    IDLE, CHECKING, NO_RELEASE, UP_TO_DATE, UPDATE_AVAILABLE, UNCOMPARABLE, ERROR,
    DOWNLOADING, DOWNLOADED,
}

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val installedVersionName: String = "",
    val installedVersionCode: Long = 0,
    val latestVersion: String? = null,
    val releaseDate: String? = null,
    val releaseNotes: String? = null,
    val htmlUrl: String? = null,
    val release: GithubRelease? = null,
    val asset: GithubAsset? = null,
    val comparison: UpdateStatusResolver.Comparison = UpdateStatusResolver.Comparison.UNCOMPARABLE,
    val lastCheckedAt: Long = 0,
    val errorMessage: String? = null,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloadedFile: File? = null,
    val needsInstallPermission: Boolean = false,
)

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state

    private var checkJob: Job? = null
    private var downloadJob: Job? = null

    init {
        val (name, code) = installedVersion()
        val prefs = prefs()
        _state.update {
            it.copy(
                installedVersionName = name,
                installedVersionCode = code,
                lastCheckedAt = prefs.getLong(KEY_LAST_CHECK, 0),
            )
        }
    }

    fun maybeAutoCheck() {
        if (!shouldAutoCheck()) return
        checkForUpdates(manual = false)
    }

    fun checkForUpdates(manual: Boolean) {
        val current = _state.value
        if (current.status == UpdateStatus.CHECKING || current.status == UpdateStatus.DOWNLOADING) return
        if (!manual && !shouldAutoCheck()) return

        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _state.update { it.copy(status = UpdateStatus.CHECKING, errorMessage = null) }
            when (val result = UpdateChecker.fetchLatest()) {
                is UpdateChecker.Result.Release -> applyRelease(result.release, result.asset)
                UpdateChecker.Result.NoRelease -> {
                    saveLastCheck()
                    _state.update {
                        it.copy(status = UpdateStatus.NO_RELEASE, htmlUrl = UpdateChecker.RELEASES_PAGE)
                    }
                }

                is UpdateChecker.Result.Failure -> {
                    _state.update {
                        it.copy(status = UpdateStatus.ERROR, errorMessage = errorText(result.reason))
                    }
                }
            }
        }
    }

    private fun applyRelease(release: GithubRelease, asset: GithubAsset?) {
        saveLastCheck()
        val comparison = UpdateStatusResolver.resolve(
            _state.value.installedVersionCode,
            _state.value.installedVersionName,
            release,
        )
        val status = when (comparison) {
            UpdateStatusResolver.Comparison.NEWER -> UpdateStatus.UPDATE_AVAILABLE
            UpdateStatusResolver.Comparison.UP_TO_DATE -> UpdateStatus.UP_TO_DATE
            UpdateStatusResolver.Comparison.UNCOMPARABLE -> UpdateStatus.UNCOMPARABLE
        }
        _state.update {
            it.copy(
                status = status,
                latestVersion = release.tagName.ifBlank { release.name.orEmpty() },
                releaseDate = formatDate(release.publishedAt),
                releaseNotes = release.body?.trim()?.takeIf { notes -> notes.isNotEmpty() },
                htmlUrl = release.htmlUrl.ifBlank { UpdateChecker.RELEASES_PAGE },
                release = release,
                asset = asset,
                comparison = comparison,
                errorMessage = null,
                downloadedFile = null,
                downloadedBytes = 0,
                totalBytes = 0,
            )
        }
    }

    fun downloadUpdate() {
        val current = _state.value
        val asset = current.asset ?: return
        if (current.status == UpdateStatus.DOWNLOADING || current.status == UpdateStatus.CHECKING) return

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            val app = getApplication<Application>()
            UpdateDownloader.cleanup(app)
            _state.update {
                it.copy(
                    status = UpdateStatus.DOWNLOADING,
                    downloadedBytes = 0,
                    totalBytes = asset.size.coerceAtLeast(0),
                    errorMessage = null,
                    downloadedFile = null,
                )
            }
            try {
                val file = UpdateDownloader.download(app, asset.browserDownloadUrl, asset.name) { progress ->
                    _state.update {
                        it.copy(downloadedBytes = progress.downloaded, totalBytes = progress.total)
                    }
                }
                when (val verdict = ApkValidator.validate(app, file)) {
                    ApkValidator.Verdict.Valid -> _state.update {
                        it.copy(status = UpdateStatus.DOWNLOADED, downloadedFile = file)
                    }

                    is ApkValidator.Verdict.Invalid -> {
                        file.delete()
                        _state.update {
                            it.copy(
                                status = UpdateStatus.UPDATE_AVAILABLE,
                                downloadedFile = null,
                                errorMessage = validationText(verdict.reason),
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                UpdateDownloader.cleanup(app)
                _state.update {
                    it.copy(
                        status = UpdateStatus.UPDATE_AVAILABLE,
                        downloadedBytes = 0,
                        totalBytes = 0,
                        downloadedFile = null,
                    )
                }
                throw e
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        status = UpdateStatus.UPDATE_AVAILABLE,
                        downloadedBytes = 0,
                        totalBytes = 0,
                        downloadedFile = null,
                        errorMessage = DownloaderApp.str(R.string.upd_error_download),
                    )
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    fun installDownloaded() {
        val file = _state.value.downloadedFile ?: return
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !app.packageManager.canRequestPackageInstalls()
        ) {
            _state.update { it.copy(needsInstallPermission = true) }
            return
        }
        startInstaller(app, file)
    }

    fun openInstallPermissionSettings() {
        _state.update { it.copy(needsInstallPermission = false) }
        val app = getApplication<Application>()
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${app.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            app.startActivity(intent)
        } catch (_: Exception) {
            // sebagian perangkat tidak menyediakan halaman ini
        }
    }

    fun dismissInstallPermission() {
        _state.update { it.copy(needsInstallPermission = false) }
    }

    fun openReleasePage() {
        val url = _state.value.htmlUrl ?: UpdateChecker.RELEASES_PAGE
        val app = getApplication<Application>()
        try {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
            // tidak ada browser yang menangani
        }
    }

    private fun startInstaller(app: Application, file: File) {
        try {
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        } catch (_: Exception) {
            _state.update { it.copy(errorMessage = DownloaderApp.str(R.string.upd_error_install)) }
        }
    }

    private fun shouldAutoCheck(): Boolean {
        val last = prefs().getLong(KEY_LAST_CHECK, 0)
        if (last <= 0) return true
        return System.currentTimeMillis() - last >= AUTO_CHECK_INTERVAL_MS
    }

    private fun saveLastCheck() {
        val now = System.currentTimeMillis()
        prefs().edit().putLong(KEY_LAST_CHECK, now).apply()
        _state.update { it.copy(lastCheckedAt = now) }
    }

    private fun prefs() =
        getApplication<Application>().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    private fun installedVersion(): Pair<String, Long> = try {
        val app = getApplication<Application>()
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        (info.versionName ?: "-") to info.versionCode.toLong()
    } catch (_: PackageManager.NameNotFoundException) {
        "-" to 0L
    }

    private fun errorText(reason: UpdateChecker.Result.Reason): String = when (reason) {
        UpdateChecker.Result.Reason.NETWORK -> DownloaderApp.str(R.string.upd_error_network)
        UpdateChecker.Result.Reason.RATE_LIMITED -> DownloaderApp.str(R.string.upd_error_rate)
        UpdateChecker.Result.Reason.SERVER -> DownloaderApp.str(R.string.upd_error_server)
        UpdateChecker.Result.Reason.MALFORMED -> DownloaderApp.str(R.string.upd_error_malformed)
    }

    private fun validationText(reason: ApkValidator.Verdict.Reason): String = when (reason) {
        ApkValidator.Verdict.Reason.PACKAGE_MISMATCH ->
            DownloaderApp.str(R.string.upd_validation_package)

        ApkValidator.Verdict.Reason.SIGNATURE_MISMATCH ->
            DownloaderApp.str(R.string.upd_validation_signature)

        ApkValidator.Verdict.Reason.UNREADABLE ->
            DownloaderApp.str(R.string.upd_validation_unreadable)
    }

    private fun formatDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val parsed = parser.parse(raw) ?: return raw.take(10)
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(parsed.time))
        } catch (_: Exception) {
            raw.take(10)
        }
    }

    companion object {
        private const val PREFS = "downloader_update"
        private const val KEY_LAST_CHECK = "last_check_at"
        private const val AUTO_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
