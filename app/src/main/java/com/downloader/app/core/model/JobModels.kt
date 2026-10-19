package com.downloader.app.core.model

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

enum class ItemStatus {
    WAITING,

    /** Item sedang diunduh oleh yt-dlp. */
    RUNNING,

    /** Unduhan item selesai; berkas sedang diproses (merge/konversi/validasi). */
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED,
    ;
}

@Serializable
data class ItemState(
    val index: Int,
    val title: String = "",
    val status: ItemStatus = ItemStatus.WAITING,
    val message: String? = null,
    val percent: Int = -1,
)

class ItemCanceledException : CancellationException("item dibatalkan")

enum class JobStage {
    QUEUED,
    DOWNLOADING,
    PROCESSING,
    PACKAGING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED || this == EXPIRED

    val isActive: Boolean
        get() = !isTerminal

    val isRunning: Boolean
        get() = isActive && this != QUEUED && this != PAUSED
}

@Serializable
data class DownloadRequest(
    val url: String,
    val format: String,
    val bitrate: Int,
    val resolution: String,
    val hd: Boolean,
    val items: List<Int>?,
    val zipArchive: Boolean,
    val meta: NormalizedMetadata?,
)

data class DownloadResult(
    val fileName: String,
    val uri: String,
    val location: String,
    val sizeBytes: Long,
    val mimeType: String,
    val isZip: Boolean,
    val completedCount: Int = 1,
    val failedCount: Int = 0,
    val downloadMs: Long = 0,
    val processMs: Long = 0,
    val processMode: String = "",
)

interface JobReporter {
    fun stage(stage: JobStage, message: String? = null)
    fun progress(percent: Int)
    fun bytes(downloaded: Long, total: Long)
    fun speed(bytesPerSecond: Long, etaSeconds: Long?)
    fun items(current: Int, total: Int)
    fun title(title: String)

    fun itemsInit(items: List<ItemState>) {}

    fun item(index: Int, status: ItemStatus, message: String? = null, percent: Int = -1) {}
}
