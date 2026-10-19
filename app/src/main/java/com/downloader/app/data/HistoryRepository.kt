package com.downloader.app.data

import android.content.Context
import com.downloader.app.core.model.DownloadRequest
import com.downloader.app.core.model.DownloadResult
import com.downloader.app.core.util.Formatters
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object HistoryStatus {
    const val COMPLETED = "completed"
    const val FAILED = "failed"
    const val CANCELLED = "cancelled"
}

@Serializable
data class HistoryEntry(
    val id: String,
    val title: String,
    val platform: String = "unknown",
    val url: String = "",
    val format: String = "mp4",
    val qualityLabel: String = "",
    val fileName: String = "",
    val uri: String = "",
    val location: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val isZip: Boolean = false,
    val status: String = HistoryStatus.COMPLETED,
    val errorMessage: String? = null,
    val thumbnail: String? = null,
    val createdAt: Long,
    val bitrate: Int = 320,
    val resolution: String = "auto",
    val hd: Boolean = true,
    val zipArchive: Boolean = false,
    val downloadMs: Long = 0,
    val processMs: Long = 0,
    val processMode: String = "",
)

object HistoryRepository {

    private const val MAX_ENTRIES = 300

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    private var historyFile: File? = null

    @Synchronized
    fun init(context: Context) {
        if (historyFile != null) return
        val file = File(context.applicationContext.filesDir, "history.json")
        historyFile = file
        _entries.value = try {
            if (file.exists()) json.decodeFromString<List<HistoryEntry>>(file.readText()) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun addCompleted(context: Context, request: DownloadRequest, result: DownloadResult) {
        add(
            context,
            buildEntry(
                request = request,
                title = request.meta?.media?.title?.takeIf { it.isNotBlank() } ?: result.fileName,
                status = HistoryStatus.COMPLETED,
                fileName = result.fileName,
                uri = result.uri,
                location = result.location,
                mimeType = result.mimeType,
                sizeBytes = result.sizeBytes,
                isZip = result.isZip,
                errorMessage = null,
                downloadMs = result.downloadMs,
                processMs = result.processMs,
                processMode = result.processMode,
            ),
        )
    }

    @Synchronized
    fun addFailed(context: Context, request: DownloadRequest, message: String?) {
        add(
            context,
            buildEntry(
                request = request,
                title = request.meta?.media?.title?.takeIf { it.isNotBlank() } ?: request.url,
                status = HistoryStatus.FAILED,
                errorMessage = message,
            ),
        )
    }

    @Synchronized
    fun addCancelled(context: Context, request: DownloadRequest) {
        add(
            context,
            buildEntry(
                request = request,
                title = request.meta?.media?.title?.takeIf { it.isNotBlank() } ?: request.url,
                status = HistoryStatus.CANCELLED,
            ),
        )
    }

    @Synchronized
    fun remove(id: String) {
        _entries.value = _entries.value.filterNot { it.id == id }
        persist()
    }

    @Synchronized
    fun clear() {
        _entries.value = emptyList()
        persist()
    }

    private fun buildEntry(
        request: DownloadRequest,
        title: String,
        status: String,
        fileName: String = "",
        uri: String = "",
        location: String = "",
        mimeType: String = "",
        sizeBytes: Long = 0,
        isZip: Boolean = false,
        errorMessage: String? = null,
        downloadMs: Long = 0,
        processMs: Long = 0,
        processMode: String = "",
    ): HistoryEntry = HistoryEntry(
        id = UUID.randomUUID().toString().replace("-", "").take(12),
        title = title,
        platform = request.meta?.source?.platform ?: "unknown",
        url = request.url,
        format = request.format,
        qualityLabel = Formatters.qualityLabel(request.format, request.bitrate, request.resolution, request.hd),
        fileName = fileName,
        uri = uri,
        location = location,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        isZip = isZip,
        status = status,
        errorMessage = errorMessage,
        thumbnail = request.meta?.media?.thumbnail,
        createdAt = System.currentTimeMillis(),
        bitrate = request.bitrate,
        resolution = request.resolution,
        hd = request.hd,
        zipArchive = request.zipArchive,
        downloadMs = downloadMs,
        processMs = processMs,
        processMode = processMode,
    )

    private fun add(context: Context, entry: HistoryEntry) {
        if (historyFile == null) init(context)
        _entries.value = (listOf(entry) + _entries.value).take(MAX_ENTRIES)
        persist()
    }

    private fun persist() {
        try {
            historyFile?.writeText(json.encodeToString(_entries.value))
        } catch (_: Exception) {
            // riwayat bersifat best-effort
        }
    }
}
