package com.downloader.app.ui.download

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.AppException
import com.downloader.app.core.ErrorMapper
import com.downloader.app.core.model.DownloadRequest
import com.downloader.app.core.model.NormalizedMetadata
import com.downloader.app.core.pipeline.MediaAnalyzer
import com.downloader.app.core.service.DownloadService
import com.downloader.app.core.service.JobController
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.util.SizeEstimator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class DownloadPreset { BEST, AUDIO, STORAGE, BALANCED }

data class DownloadPrefill(
    val url: String,
    val format: String? = null,
    val bitrate: Int? = null,
    val resolution: String? = null,
    val hd: Boolean? = null,
    val zip: Boolean? = null,
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private var analyzeJob: Job? = null

    /**
     * Penanda analisis terbaru. Hasil analisis hanya boleh ditulis ke UI bila
     * penandanya masih yang terakhir, supaya analisis yang sudah dibatalkan
     * tidak mengisi ulang kolom tautan yang baru saja dikosongkan.
     */
    private var analyzeGeneration = 0

    data class AnalyzeUi(
        val url: String = "",
        val loading: Boolean = false,
        val meta: NormalizedMetadata? = null,
        val error: String? = null,
    )

    var analyze by mutableStateOf(AnalyzeUi())
        private set

    var selectedFormat by mutableStateOf("mp4")
        private set

    var bitrate by mutableStateOf(320)
        private set

    var resolution by mutableStateOf("auto")
        private set

    var hd by mutableStateOf(true)
        private set

    var zipMode by mutableStateOf(false)
        private set

    var preset by mutableStateOf<DownloadPreset?>(null)
        private set

    var selectedItems by mutableStateOf<Set<Int>>(emptySet())
        private set

    var starting by mutableStateOf(false)
        private set

    var userMessage by mutableStateOf<String?>(null)
        private set

    fun onUrlChange(value: String) {
        analyze = analyze.copy(url = value, error = null)
    }

    fun onClipboardEmpty() {
        userMessage = DownloaderApp.str(R.string.snackbar_clipboard_empty)
    }

    /**
     * Kosongkan kolom tautan sekali sentuh, termasuk hasil analisisnya.
     *
     * Tanpa ini tautan unduhan sebelumnya tetap tertinggal di kolom, sehingga
     * pengguna harus menghapusnya manual lewat keyboard sebelum menempel
     * tautan baru.
     */
    fun onUrlClear() {
        analyzeGeneration++
        // Analisis yang masih berjalan dihentikan agar hasilnya tidak muncul
        // kembali setelah kolom bersih.
        analyzeJob?.cancel()
        analyzeJob = null
        analyze = AnalyzeUi()
        preset = null
        selectedItems = emptySet()
    }

    /**
     * Preferensi format dari aksi cepat Beranda ("Unduh berdasarkan jenis").
     * Dipakai setelah analisis selesai bila tautan mendukung jenis tersebut;
     * pilihan manual pengguna ([selectFormat]) menghapus preferensi ini.
     */
    private var preferredFormat: String? = null

    fun applyPrefill(prefill: DownloadPrefill) {
        prefill.format?.let {
            preferredFormat = it
            selectedFormat = it
        }
        prefill.bitrate?.let { bitrate = it }
        prefill.resolution?.let { resolution = it }
        prefill.hd?.let { hd = it }
        prefill.zip?.let { zipMode = it }
        if (prefill.url.isBlank()) return
        analyze = AnalyzeUi(url = prefill.url.trim())
        runAnalyze()
    }

    fun runAnalyze() {
        val url = analyze.url.trim()
        if (url.isEmpty() || analyze.loading) return
        val generation = ++analyzeGeneration
        analyze = analyze.copy(loading = true, error = null, meta = null)
        userMessage = null

        analyzeJob = viewModelScope.launch {
            try {
                val meta = MediaAnalyzer.analyze(url)
                if (generation == analyzeGeneration) applyMeta(meta)
            } catch (e: AppException) {
                if (generation == analyzeGeneration) {
                    analyze = analyze.copy(loading = false, error = e.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation == analyzeGeneration) {
                    analyze = analyze.copy(loading = false, error = ErrorMapper.friendly(e.message))
                }
            }
        }
    }

    private fun applyMeta(meta: NormalizedMetadata) {
        analyze = analyze.copy(loading = false, meta = meta, error = null)
        val preferred = preferredFormat
        val supported = when (preferred) {
            "photo" -> meta.capabilities.photo
            "mp3", "m4a" -> meta.capabilities.audio
            "mp4" -> meta.capabilities.video
            else -> false
        }
        selectedFormat = when {
            preferred != null && supported -> preferred
            meta.media.type == "photo" && meta.capabilities.photo -> "photo"
            meta.capabilities.video -> "mp4"
            meta.capabilities.audio -> "mp3"
            meta.capabilities.photo -> "photo"
            else -> "mp4"
        }
        preferredFormat = null
        resolution = "auto"
        hd = true
        selectedItems = meta.playlist?.items?.map { it.index }?.toSet() ?: emptySet()
        zipMode = false
        preset = when {
            selectedFormat == "photo" -> null
            selectedFormat == "mp3" || selectedFormat == "m4a" -> {
                if (meta.capabilities.audio) DownloadPreset.AUDIO else null
            }

            meta.capabilities.video -> DownloadPreset.BEST
            meta.capabilities.audio -> DownloadPreset.AUDIO
            else -> null
        }
    }

    val singleEstimateBytes: Long?
        get() {
            val meta = analyze.meta ?: return null
            if (meta.playlist != null) return null
            return SizeEstimator.estimateBytes(selectedFormat, bitrate, resolution, hd, meta.media.duration)
        }

    val playlistEstimateBytes: Long?
        get() {
            val playlist = analyze.meta?.playlist ?: return null
            val items = playlist.items.filter { it.index in selectedItems }
            if (items.isEmpty()) return null
            var total = 0L
            var any = false
            items.forEach { item ->
                val estimate = SizeEstimator.estimateBytes(
                    format = selectedFormat,
                    bitrate = bitrate,
                    resolution = resolution,
                    hd = hd,
                    durationSeconds = item.duration,
                )
                if (estimate != null) {
                    total += estimate
                    any = true
                }
            }
            return if (any) total else null
        }

    fun selectFormat(format: String) {
        preferredFormat = null
        selectedFormat = format
        preset = null
    }

    fun updateBitrate(value: Int) {
        bitrate = value
        preset = null
    }

    fun updateResolution(value: String) {
        resolution = value
        preset = null
    }

    fun updateHd(value: Boolean) {
        hd = value
        preset = null
    }

    fun applyPreset(value: DownloadPreset) {
        val meta = analyze.meta
        val videoCapable = meta?.capabilities?.video == true
        when (value) {
            DownloadPreset.BEST -> {
                selectedFormat = if (videoCapable) "mp4" else "mp3"
                resolution = "auto"
                hd = true
                if (selectedFormat == "mp3") bitrate = 320
            }

            DownloadPreset.AUDIO -> {
                selectedFormat = "mp3"
                bitrate = 320
            }

            DownloadPreset.STORAGE -> {
                if (videoCapable) {
                    selectedFormat = "mp4"
                    resolution = "480"
                    hd = false
                } else {
                    selectedFormat = "mp3"
                    bitrate = 128
                }
            }

            DownloadPreset.BALANCED -> {
                PrefsRepository.setTurbo(false)
                userMessage = DownloaderApp.str(R.string.download_preset_balanced_note)
            }
        }
        preset = value
    }

    fun updateZipMode(value: Boolean) {
        zipMode = value
    }

    fun toggleItem(index: Int) {
        selectedItems = if (index in selectedItems) selectedItems - index else selectedItems + index
    }

    fun selectAllItems() {
        selectedItems = analyze.meta?.playlist?.items?.map { it.index }?.toSet() ?: emptySet()
    }

    fun unselectAllItems() {
        selectedItems = emptySet()
    }

    fun dismissError() {
        analyze = analyze.copy(error = null)
    }

    fun dismissUserMessage() {
        userMessage = null
    }

    fun startDownload(onStarted: () -> Unit, onDuplicate: () -> Unit) {
        val meta = analyze.meta ?: return
        if (starting) return

        if (meta.playlist != null && selectedItems.isEmpty()) {
            userMessage = DownloaderApp.str(R.string.playlist_need_selection)
            return
        }

        val app = getApplication<Application>()
        val request = DownloadRequest(
            url = analyze.url.trim(),
            format = selectedFormat,
            bitrate = bitrate,
            resolution = resolution,
            hd = hd,
            items = meta.playlist?.let { selectedItems.sorted() },
            zipArchive = zipMode,
            meta = meta,
        )

        starting = true
        val jobId = JobController.enqueue(app, request)
        if (jobId == null) {
            starting = false
            onDuplicate()
            return
        }

        ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
        starting = false
        onStarted()
    }
}
