package com.downloader.app.core.pipeline

import android.content.Context
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.AppException
import com.downloader.app.core.ErrorMapper
import com.downloader.app.core.engine.FfmpegEngine
import com.downloader.app.core.engine.ThumbnailFetcher
import com.downloader.app.core.engine.YtDlpEngine
import com.downloader.app.core.log.AppLog
import com.downloader.app.core.model.DownloadRequest
import com.downloader.app.core.model.DownloadResult
import com.downloader.app.core.model.ItemCanceledException
import com.downloader.app.core.model.ItemState
import com.downloader.app.core.model.ItemStatus
import com.downloader.app.core.model.JobReporter
import com.downloader.app.core.model.JobStage
import com.downloader.app.core.model.MediaInfo
import com.downloader.app.core.model.NormalizedMetadata
import com.downloader.app.core.model.PlaylistItem
import com.downloader.app.core.model.SourceInfo
import com.downloader.app.core.storage.OutputPublisher
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.storage.PublishResult
import com.downloader.app.core.util.FfmpegProgress
import com.downloader.app.core.util.FileNames
import com.downloader.app.core.util.FileSignature
import com.downloader.app.core.util.Formatters
import com.downloader.app.core.util.MediaProbe
import com.downloader.app.core.util.SizeEstimator
import com.downloader.app.core.util.SpeedMeter
import com.downloader.app.core.util.StorageGuard
import com.downloader.app.core.util.intOrNull
import com.downloader.app.core.util.lastThumbnailUrl
import com.downloader.app.core.util.stringOrNull
import com.downloader.app.core.util.thumbnailCandidates
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class DownloadPipeline(
    private val context: Context,
    private val jobId: String,
    private val reporter: JobReporter,
    private val resume: Boolean = false,
    private val keepWorkDir: () -> Boolean = { false },
    private val isItemCanceled: (Int) -> Boolean = { false },
) {

    private val workDir = File(context.filesDir, "jobs/$jobId")
    private val itemJobs = ConcurrentHashMap<Int, Job>()

    /**
     * Batalkan satu item playlist yang sedang berjalan. Coroutine item
     * dibatalkan sehingga proses yt-dlp/FFmpeg miliknya ikut dimatikan
     * (engine sudah menangani kill proses saat coroutine dibatalkan).
     */
    fun cancelItem(index: Int) {
        itemJobs[index]?.cancel(ItemCanceledException())
    }
    private val sourceDir = File(workDir, "source")
    private val processingDir = File(workDir, "processing")

    private data class ItemStat(
        val label: String,
        val mode: String,
        val downloadMs: Long,
        val processMs: Long,
        val bytes: Long,
        val attempts: Int,
        val detail: String? = null,
        /** Bagian dari [downloadMs] yang dipakai yt-dlp untuk merge, -1 bila tidak ada. */
        val mergeMs: Long = -1,
    )

    private val stats = Collections.synchronizedList(mutableListOf<ItemStat>())

    private fun recordStat(
        label: String,
        mode: String,
        downloadMs: Long,
        processMs: Long,
        bytes: Long,
        attempts: Int,
        detail: String? = null,
        mergeMs: Long = -1,
    ) {
        stats.add(ItemStat(label, mode, downloadMs, processMs, bytes, attempts, detail, mergeMs))
        val attemptText = if (attempts > 1) " percobaan=$attempts" else ""
        val detailText = if (!detail.isNullOrBlank()) " $detail" else ""
        // Merge dijalankan di dalam proses yt-dlp, jadi waktunya dikeluarkan dari
        // angka unduh agar "unduh" dan "merge" bisa dibandingkan terpisah.
        val pureDownloadMs = if (mergeMs >= 0) (downloadMs - mergeMs).coerceAtLeast(0) else downloadMs
        val mergeText = if (mergeMs >= 0) " merge=${fmtSec(mergeMs)}s" else ""
        AppLog.d(
            "Pipeline",
            "#$label mode=$mode unduh=${fmtSec(pureDownloadMs)}s$mergeText proses=${fmtSec(processMs)}s " +
                "ukuran=${fmtMb(bytes)}MB$attemptText$detailText",
        )
    }

    private fun logPlaylistSummary(title: String, ok: Int, failed: Int) {
        val snapshot = stats.toList()
        if (snapshot.isEmpty()) return
        val mergeMs = snapshot.sumOf { if (it.mergeMs >= 0) it.mergeMs else 0L }
        val downloadMs = snapshot.sumOf { it.downloadMs } - mergeMs
        val processMs = snapshot.sumOf { it.processMs }
        val nativeCount = snapshot.count { it.mode == "native" }
        val fastCount = snapshot.count { it.mode == "copy" || it.mode == "remux" }
        val encodeCount = snapshot.count { it.mode == "encode" }
        val skippedFfmpeg = snapshot.count { it.mode == "native" }
        AppLog.d(
            "Pipeline",
            "Playlist '$title' ringkasan: sukses=$ok gagal=$failed " +
                "native=$nativeCount remux=$fastCount encode=$encodeCount " +
                "tanpa_ffmpeg=$skippedFfmpeg unduh=${fmtSec(downloadMs)}s " +
                "merge=${fmtSec(mergeMs)}s proses=${fmtSec(processMs)}s",
        )
    }

    private fun fmtSec(ms: Long): String = "%.1f".format(Locale.US, ms / 1000.0)

    private fun fmtMb(bytes: Long): String = "%.1f".format(Locale.US, bytes / 1048576.0)

    /**
     * Validasi cepat isi file (magic bytes) sebelum dianggap selesai.
     * Mencegah "sekadar ganti ekstensi" menyajikan file yang isinya beda container.
     */
    /**
     * Cek ruang sebelum menulis. Estimasi null tetap memakai batas minimum
     * agar disk yang hampir penuh gagal lebih awal dengan pesan yang jelas.
     */
    private fun ensureFreeSpace(estimateBytes: Long?) {
        val space = StorageGuard.check(workDir, estimateBytes) ?: return
        if (space.isEnough) return
        throw AppException(
            "STORAGE_LOW",
            DownloaderApp.str(
                R.string.err_storage_low,
                Formatters.bytes(space.freeBytes),
                Formatters.bytes(space.requiredBytes),
            ),
        )
    }

    /**
     * Validasi bertingkat: signature dulu, lalu ffprobe untuk memastikan
     * kontainer punya stream dan durasi wajar. Bila ffprobe tidak bisa jalan
     * di perangkat, validasi signature tetap dipakai (perilaku lama) dan
     * kegagalannya dicatat ke log.
     *
     * Hasil [Validation.Ok] membawa [MediaProbe.Info] supaya pemanggil bisa
     * memakai ulang data codec tanpa menjalankan ffprobe/FFmpeg sekali lagi.
     */
    private sealed interface Validation {
        /** Lolos. [info] null berarti ffprobe tidak tersedia (hanya signature diperiksa). */
        data class Ok(val info: MediaProbe.Info?) : Validation

        data object Rejected : Validation
    }

    private suspend fun validateOutput(
        file: File,
        format: String,
        expect: MediaProbe.Expect,
    ): Validation {
        if (!FileSignature.hasValidMagic(file, format)) return Validation.Rejected
        return when (val probe = FfmpegEngine.probe(context, file, "probe_${jobId}_${System.nanoTime()}")) {
            is FfmpegEngine.ProbeResult.Output -> {
                val info = MediaProbe.parse(probe.json)
                if (MediaProbe.isValid(info, expect)) {
                    Validation.Ok(info)
                } else {
                    AppLog.d("Pipeline", "#$jobId probe menolak ${file.name}")
                    Validation.Rejected
                }
            }

            FfmpegEngine.ProbeResult.Failed -> Validation.Rejected

            FfmpegEngine.ProbeResult.Unavailable -> {
                AppLog.d("Pipeline", "ffprobe tidak tersedia; memakai validasi signature")
                Validation.Ok(null)
            }
        }
    }

    /**
     * Validasi target MP4: selain stream + durasi, codec hasil `-c copy` juga
     * harus bisa diputar dari dalam MP4 (Opus/Vorbis ditolak). Tanpa pemeriksaan
     * ini `copy` WebM -> MP4 "berhasil" padahal berkasnya tidak berguna, dan
     * tangga cadangan [ConversionPlanner.escalation] tidak pernah terpakai.
     */
    private suspend fun validateMp4(file: File): Validation {
        val validation = validateOutput(file, "mp4", MediaProbe.Expect.VIDEO)
        val info = (validation as? Validation.Ok)?.info ?: return validation
        if (!ConversionPlanner.isMp4Compatible(info.videoCodec(), info.audioCodec())) {
            AppLog.d(
                "Pipeline",
                "#$jobId codec tidak kompatibel MP4 (video=${info.videoCodec()} audio=${info.audioCodec()})",
            )
            return Validation.Rejected
        }
        return validation
    }

    /** Probe berkas sumber untuk membaca codec sebelum memilih operasi FFmpeg. */
    private suspend fun probeSource(file: File): MediaProbe.Info? {
        val result = FfmpegEngine.probe(context, file, "srcprobe_${jobId}_${System.nanoTime()}")
        return (result as? FfmpegEngine.ProbeResult.Output)?.let { MediaProbe.parse(it.json) }
    }

    /**
     * Baca `<nama-output>.info.json` yang ditulis yt-dlp karena
     * `--write-info-json` pada proses unduhan yang baru saja berjalan.
     *
     * Ini metadata milik **item itu sendiri** (artist/album/uploader), jadi jauh
     * lebih tepat daripada metadata playlist. Karena yt-dlp sudah mengambilnya
     * untuk unduhan tersebut, tidak ada proses ekstraksi tambahan — berbeda
     * dengan `dumpInfo` yang menjalankan yt-dlp kedua kali.
     *
     * Berkasnya selalu dihapus agar tidak ikut terpublikasi.
     */
    private fun readDownloadInfo(dir: File, baseName: String): JsonObject? {
        val file = dir.listFiles()?.firstOrNull { it.name == "$baseName.info.json" } ?: return null
        return try {
            Json.parseToJsonElement(file.readText()).jsonObject
        } catch (_: Exception) {
            null
        } finally {
            file.delete()
        }
    }

    suspend fun run(request: DownloadRequest): DownloadResult {
        workDir.mkdirs()
        sourceDir.mkdirs()
        processingDir.mkdirs()
        return try {
            val result = runInternal(request)
            if (!keepWorkDir()) workDir.deleteRecursively()
            result
        } catch (e: CancellationException) {
            if (!keepWorkDir()) workDir.deleteRecursively()
            throw e
        }
        // Kegagalan biasa: workdir SENGAJA dipertahankan agar tombol
        // "Coba lagi" bisa lanjut tanpa mengunduh ulang item yang selesai.
        // Dibersihkan saat kartu job ditutup (JobController.remove) atau cache dibersihkan.
    }

    private suspend fun runInternal(request: DownloadRequest): DownloadResult {
        reporter.stage(JobStage.DOWNLOADING, DownloaderApp.str(R.string.pl_preparing))
        reporter.progress(4)

        val meta = run {
            val analyzeStart = System.currentTimeMillis()
            val resolved = request.meta ?: MediaAnalyzer.analyze(request.url)
            AppLog.d(
                "Pipeline",
                "#$jobId analisis=${fmtSec(System.currentTimeMillis() - analyzeStart)}s " +
                    "sumber=${if (request.meta != null) "pra-analisis" else "extractor"} " +
                    "tipe=${resolved.media.type} platform=${resolved.source.platform}",
            )
            resolved
        }

        val lowerUrl = request.url.lowercase()
        val isPotentialPlaylist = request.items != null ||
            "list=" in lowerUrl ||
            "playlist" in lowerUrl ||
            meta.media.isPlaylist

        var entries: List<PlaylistItem>? = null
        if (isPotentialPlaylist) {
            entries = meta.playlist?.items?.takeIf { it.isNotEmpty() }
            if (entries.isNullOrEmpty()) {
                val enumStart = System.currentTimeMillis()
                entries = try {
                    enumeratePlaylist(request.url)
                } catch (_: Exception) {
                    null
                }
                AppLog.d(
                    "Pipeline",
                    "#$jobId enumerasi playlist=${fmtSec(System.currentTimeMillis() - enumStart)}s " +
                        "item=${entries?.size ?: 0}",
                )
            }
        }

        if (request.format == "photo" && meta.photoImages.isNotEmpty()) {
            return processSingle(request, meta)
        }

        val playlistFlow = entries != null && (entries.size > 1 || request.items != null)
        return if (playlistFlow) {
            processPlaylist(request, meta, entries!!)
        } else {
            processSingle(request, meta)
        }
    }

    private suspend fun processSingle(request: DownloadRequest, meta: NormalizedMetadata): DownloadResult {
        val mapper = ProgressMapper(reporter, 4, 88)
        val statsBefore = stats.size
        ensureFreeSpace(
            SizeEstimator.estimateBytes(
                format = request.format,
                bitrate = request.bitrate,
                resolution = request.resolution,
                hd = request.hd,
                durationSeconds = meta.media.duration,
            ),
        )
        val out = when (request.format) {
            "mp3", "m4a" -> processAudio(request.url, request.format, request.bitrate, processingDir, meta, mapper, "single")
            "mp4" -> processVideo(request.url, request.resolution, request.hd, processingDir, meta, mapper, "single")
            "photo" -> {
                val images = meta.photoImages
                if (images.isNotEmpty()) {
                    processPhotoPost(request.url, meta, images, processingDir, mapper)
                } else {
                    processPhoto(request.url, processingDir, mapper)
                }
            }
            else -> throw AppException(
                "PROCESSING_FAILED",
                DownloaderApp.str(R.string.err_format_unsupported, request.format),
            )
        }

        reporter.stage(JobStage.PROCESSING, DownloaderApp.str(R.string.pl_saving))
        reporter.progress(96)
        val size = out.length()
        val mime = FileNames.mimeForFile(out)
        val published = OutputPublisher.publish(context, out, out.name, mime)
        reporter.progress(100)
        val stat = if (stats.size > statsBefore) stats.lastOrNull() else null
        return DownloadResult(
            fileName = out.name,
            uri = published.uri,
            location = published.location,
            sizeBytes = size,
            mimeType = mime,
            isZip = false,
            downloadMs = stat?.downloadMs ?: 0,
            processMs = stat?.processMs ?: 0,
            processMode = stat?.mode ?: "",
        )
    }

    private suspend fun processPlaylist(
        request: DownloadRequest,
        meta: NormalizedMetadata,
        entries: List<PlaylistItem>,
    ): DownloadResult {
        val rawTitle = meta.playlist?.title
            ?: meta.media.title.takeIf { it.isNotBlank() && it != "Untitled Media" }
            ?: DownloaderApp.str(R.string.playlist_default_title)
        val playlistTitle = FileNames.sanitizeFilename(rawTitle)

        val selected = if (request.items != null) {
            val set = request.items.toSet()
            entries.filter { it.index in set }
        } else {
            entries
        }
        if (selected.isEmpty()) {
            throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.err_playlist_empty_selection))
        }

        val cap = when (request.format) {
            "photo" -> 3000
            "mp3", "m4a" -> if (request.bitrate > 192) 1600 else 2100
            else -> 500
        }
        val limited = selected.take(cap)
        val capNotice = if (limited.size < selected.size) {
            DownloaderApp.str(R.string.pl_playlist_cap, limited.size, selected.size)
        } else {
            ""
        }

        return if (request.zipArchive) {
            processPlaylistZip(request, meta, limited, playlistTitle, capNotice)
        } else {
            processPlaylistLoose(request, meta, limited, playlistTitle, capNotice)
        }
    }

    private suspend fun processPlaylistZip(
        request: DownloadRequest,
        meta: NormalizedMetadata,
        items: List<PlaylistItem>,
        playlistTitle: String,
        capNotice: String,
    ): DownloadResult {
        val playlistFolder = File(processingDir, playlistTitle)
        playlistFolder.mkdirs()

        runCatching {
            meta.media.thumbnail?.takeIf { it.startsWith("http") }?.let { thumbUrl ->
                ThumbnailFetcher.fetchBytes(thumbUrl)?.takeIf { it.size > 512 }?.let { bytes ->
                    File(playlistFolder, "cover.jpg").writeBytes(bytes)
                }
            }
        }

        val completed = Collections.synchronizedList(mutableListOf<File>())
        val failed = Collections.synchronizedList(mutableListOf<Pair<Int, String>>())
        val total = items.size
        val progress = PlaylistProgressTracker(reporter, total)
        val canceled = AtomicInteger(0)

        reporter.title(playlistTitle)
        reporter.stage(
            JobStage.DOWNLOADING,
            DownloaderApp.str(R.string.pl_playlist_downloading, total, capNotice),
        )
        reporter.items(0, total)
        reporter.itemsInit(
            items.map {
                ItemState(
                    index = it.index,
                    title = it.title,
                    status = if (isItemCanceled(it.index)) ItemStatus.CANCELED else ItemStatus.WAITING,
                )
            },
        )

        val singleSink: JobReporter = ProgressMapper(reporter, 5, 80)
        suspend fun reportCanceled(item: PlaylistItem) {
            canceled.incrementAndGet()
            reporter.item(item.index, ItemStatus.CANCELED)
            progress.onItemFinished(item.index)
        }

        runParallel(
            items,
            itemsConcurrency(request.format),
            onCanceled = { item -> reportCanceled(item) },
        ) { item ->
            if (isItemCanceled(item.index)) {
                reportCanceled(item)
                return@runParallel
            }
            val itemJob = coroutineContext[Job]
            if (itemJob != null) itemJobs[item.index] = itemJob
            try {
                val sink: JobReporter = if (total == 1) {
                    singleSink
                } else {
                    ItemSink(reporter, item.index) { pct -> progress.onItemPercent(item.index, pct) }
                }
                reporter.item(item.index, ItemStatus.RUNNING)
                val out = processPlaylistItem(request, meta, item, playlistFolder, sink)
                completed.add(out)
                reporter.item(item.index, ItemStatus.COMPLETED)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed.add(item.index to (e.message ?: DownloaderApp.str(R.string.pl_item_failed)))
                reporter.item(
                    item.index,
                    ItemStatus.FAILED,
                    e.message ?: DownloaderApp.str(R.string.pl_item_failed),
                )
            } finally {
                itemJobs.remove(item.index)
            }
            progress.onItemFinished(item.index)
        }

        if (completed.isEmpty()) {
            if (failed.isEmpty() && canceled.get() > 0) {
                throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.pl_all_canceled))
            }
            val detail = failed.take(3).joinToString("; ") { "#${it.first}: ${it.second}" }
            throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.err_playlist_all_failed, detail))
        }

        logPlaylistSummary(playlistTitle, completed.size, failed.size)

        reporter.title(playlistTitle)
        reporter.stage(JobStage.PACKAGING, DownloaderApp.str(R.string.pl_zip))
        reporter.progress(92)
        val zip = File(processingDir, "$playlistTitle.zip")
        zipFolder(playlistFolder, zip)
        val size = zip.length()
        val published = OutputPublisher.publish(context, zip, zip.name, "application/zip")
        reporter.progress(100)
        return DownloadResult(
            fileName = zip.name,
            uri = published.uri,
            location = published.location,
            sizeBytes = size,
            mimeType = "application/zip",
            isZip = true,
            completedCount = completed.size,
            failedCount = failed.size,
            downloadMs = stats.sumOf { it.downloadMs },
            processMs = stats.sumOf { it.processMs },
            processMode = "mixed",
        )
    }

    private suspend fun processPlaylistLoose(
        request: DownloadRequest,
        meta: NormalizedMetadata,
        items: List<PlaylistItem>,
        playlistTitle: String,
        capNotice: String,
    ): DownloadResult {
        val total = items.size
        val marksDir = File(workDir, "marks").apply { mkdirs() }

        reporter.title(playlistTitle)
        reporter.stage(
            JobStage.DOWNLOADING,
            DownloaderApp.str(R.string.pl_playlist_downloading, total, capNotice),
        )
        reporter.items(0, total)

        runCatching {
            meta.media.thumbnail?.takeIf { it.startsWith("http") }?.let { thumbUrl ->
                val bytes = ThumbnailFetcher.fetchBytes(thumbUrl)?.takeIf { it.size > 512 }
                if (bytes != null) {
                    val coverFile = File(workDir, "cover.jpg")
                    coverFile.writeBytes(bytes)
                    OutputPublisher.publish(context, coverFile, "cover.jpg", "image/jpeg", subFolder = playlistTitle)
                    coverFile.delete()
                }
            }
        }

        val failed = Collections.synchronizedList(mutableListOf<Pair<Int, String>>())
        val publishedRefs = Collections.synchronizedList(mutableListOf<Pair<PublishResult, String>>())
        val success = AtomicInteger(0)
        val totalBytesOut = AtomicLong(0)
        val progress = PlaylistProgressTracker(reporter, total)
        val canceled = AtomicInteger(0)

        reporter.itemsInit(
            items.map {
                ItemState(
                    index = it.index,
                    title = it.title,
                    status = if (isItemCanceled(it.index)) ItemStatus.CANCELED else ItemStatus.WAITING,
                )
            },
        )

        val singleSink: JobReporter = ProgressMapper(reporter, 5, 80)
        suspend fun reportCanceled(item: PlaylistItem) {
            canceled.incrementAndGet()
            reporter.item(item.index, ItemStatus.CANCELED)
            progress.onItemFinished(item.index)
        }

        runParallel(
            items,
            itemsConcurrency(request.format),
            onCanceled = { item -> reportCanceled(item) },
        ) { item ->
            val mark = File(marksDir, "done_${item.index}")
            if (isItemCanceled(item.index) && !mark.exists()) {
                reportCanceled(item)
                return@runParallel
            }
            if (mark.exists()) {
                success.incrementAndGet()
                reporter.item(item.index, ItemStatus.COMPLETED)
            } else {
                val itemJob = coroutineContext[Job]
                if (itemJob != null) itemJobs[item.index] = itemJob
                val itemDir = File(workDir, "item_${item.index}").apply { mkdirs() }
                try {
                    val sink: JobReporter = if (total == 1) {
                        singleSink
                    } else {
                        ItemSink(reporter, item.index) { pct -> progress.onItemPercent(item.index, pct) }
                    }
                    reporter.item(item.index, ItemStatus.RUNNING)
                    val out = processPlaylistItem(request, meta, item, itemDir, sink)
                    val mime = FileNames.mimeForFile(out)
                    val published = OutputPublisher.publish(context, out, out.name, mime, subFolder = playlistTitle)
                    publishedRefs.add(published to mime)
                    totalBytesOut.addAndGet(out.length())
                    success.incrementAndGet()
                    reporter.item(item.index, ItemStatus.COMPLETED)
                    out.delete()
                    runCatching { itemDir.deleteRecursively() }
                    runCatching { mark.createNewFile() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed.add(item.index to (e.message ?: DownloaderApp.str(R.string.pl_item_failed)))
                    reporter.item(
                        item.index,
                        ItemStatus.FAILED,
                        e.message ?: DownloaderApp.str(R.string.pl_item_failed),
                    )
                } finally {
                    itemJobs.remove(item.index)
                }
            }
            progress.onItemFinished(item.index)
        }

        val successCount = success.get()
        if (successCount == 0) {
            if (failed.isEmpty() && canceled.get() > 0) {
                throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.pl_all_canceled))
            }
            val detail = failed.take(3).joinToString("; ") { "#${it.first}: ${it.second}" }
            throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.err_playlist_all_failed, detail))
        }

        logPlaylistSummary(playlistTitle, successCount, failed.size)

        val first = publishedRefs.firstOrNull()
        reporter.title(playlistTitle)
        reporter.stage(JobStage.PROCESSING, DownloaderApp.str(R.string.pl_saving))
        reporter.progress(100)
        return DownloadResult(
            fileName = playlistTitle,
            uri = first?.first?.uri ?: "",
            location = first?.first?.location ?: playlistTitle,
            sizeBytes = totalBytesOut.get(),
            mimeType = first?.second ?: "",
            isZip = false,
            completedCount = successCount,
            failedCount = failed.size,
            downloadMs = stats.sumOf { it.downloadMs },
            processMs = stats.sumOf { it.processMs },
            processMode = "mixed",
        )
    }

    private suspend fun processAudio(
        url: String,
        fmt: String,
        bitrate: Int,
        outDir: File,
        metaHint: NormalizedMetadata?,
        rep: JobReporter,
        sourceToken: String,
    ): File {
        rep.stage(JobStage.DOWNLOADING, DownloaderApp.str(R.string.pl_fetch_metadata))
        rep.progress(6)

        val needsInfo = metaHint == null || metaHint.media.thumbnail == null
        val info: JsonObject? = if (needsInfo) {
            try {
                YtDlpEngine.dumpInfo(url, flatPlaylist = false)
            } catch (e: AppException) {
                if (metaHint == null) throw e
                null
            }
        } else {
            null
        }

        val title = metaHint?.media?.title?.takeIf { it.isNotBlank() && it != "Untitled Media" }
            ?: info?.stringOrNull("title")
            ?: DownloaderApp.str(R.string.media_type_audio)
        val safeTitle = FileNames.sanitizeFilename(title)
        rep.title(title)

        val expected = File(outDir, "$safeTitle.$fmt")
        if (resume && expected.exists() && expected.length() > 0 &&
            validateOutput(expected, fmt, MediaProbe.Expect.AUDIO) is Validation.Ok
        ) {
            rep.progress(78)
            return expected
        }

        ensureFreeSpace(SizeEstimator.audioBytes(bitrate, metaHint?.media?.duration))
        rep.stage(JobStage.DOWNLOADING, DownloaderApp.str(R.string.pl_download_audio))
        rep.progress(12)

        val formatSelector = when (fmt) {
            "m4a" -> "bestaudio[ext=m4a]/bestaudio[acodec^=mp4a]/bestaudio/best"
            else -> "bestaudio[acodec^=mp3]/bestaudio/best"
        }

        val processId = "${jobId}_audio_${System.nanoTime()}"
        val request = YoutubeDLRequest(url)
            .addOption("-f", formatSelector)
            .addOption("-o", File(sourceDir, "source_audio_${sourceToken}.%(ext)s").absolutePath)
            .addOption("--newline")
            .addOption("--no-warnings")
            .addOption("--no-check-certificates")
            .addOption("--concurrent-fragments", fragmentCount())
            .addOption("--no-playlist")
            .addOption("--continue")
            // Metadata lagu (artist/album) ditulis yt-dlp sebagai berkas
            // `.info.json` di samping hasil unduhan. Datanya sudah diambil untuk
            // unduhan ini, jadi tidak perlu proses ekstraksi kedua — dan yang
            // dipakai adalah artis lagunya, bukan nama pembuat playlist.
            .addOption("--write-info-json")
            .addDownloadResilience()
            .addProgressTemplate()

        val downloadStart = System.currentTimeMillis()
        val mergeMs = executeDownload(request, processId, rep, DownloaderApp.str(R.string.err_download_generic))
        val downloadMs = System.currentTimeMillis() - downloadStart

        val downloadInfo = readDownloadInfo(sourceDir, "source_audio_${sourceToken}")

        // Artis diambil dari metadata unduhan ini sendiri, BUKAN dari playlist.
        // Sebelumnya item playlist mewarisi artist/creator milik playlist, jadi
        // "Justin Bieber - Baby" bisa tertulis artis "alanzobros" (pembuat
        // playlist). Urutan: artis lagu -> kanal pengunggah -> hint per item ->
        // metadata induk (jalur unduhan tunggal) -> "Unknown Artist".
        val artist = downloadInfo?.stringOrNull("artist")
            ?: downloadInfo?.stringOrNull("uploader")
            ?: downloadInfo?.stringOrNull("channel")
            ?: metaHint?.media?.artist
            ?: metaHint?.media?.creator
            ?: info?.stringOrNull("artist")
            ?: info?.stringOrNull("uploader")
            ?: info?.stringOrNull("channel")
            ?: "Unknown Artist"
        val album = downloadInfo?.stringOrNull("album")
            ?: metaHint?.media?.album
            ?: info?.stringOrNull("album")

        val sourceAudio = sourceDir.listFiles()
            ?.firstOrNull {
                it.name.startsWith("source_audio_${sourceToken}.") &&
                    !it.name.endsWith(".part") &&
                    !it.name.endsWith(".info.json")
            }
            ?: throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.err_audio_missing))

        val coverCandidates = buildList {
            // Thumbnail dari item itu sendiri lebih tepat daripada thumbnail
            // playlist, jadi diprioritaskan.
            downloadInfo?.let { addAll(thumbnailCandidates(it)) }
            metaHint?.media?.thumbnail?.takeIf { it.startsWith("http") }?.let { add(it) }
            info?.let { addAll(thumbnailCandidates(it)) }
        }.distinct()

        val sourceExt = sourceAudio.extension.lowercase()
        val alreadyNative = ConversionPlanner.isAudioNative(fmt, sourceExt)
        if (alreadyNative) {
            val validateStart = System.currentTimeMillis()
            val validation = validateOutput(sourceAudio, fmt, MediaProbe.Expect.AUDIO)
            val validateMs = System.currentTimeMillis() - validateStart
            if (validation is Validation.Ok) {
                val target = FileNames.uniqueFile(outDir, "${safeTitle}.${fmt}")
                val moved = sourceAudio.renameTo(target)
                if (!moved) sourceAudio.copyTo(target, overwrite = true)
                // Semua keputusan diambil dari hasil ffprobe yang sudah ada,
                // jadi tidak ada proses tambahan hanya untuk memeriksa.
                val probeInfo = validation.info
                val hasCover = probeInfo?.hasAttachedPicture() == true
                val needsCover = !hasCover && coverCandidates.isNotEmpty()
                val needsTags = !probeInfo?.tag("artist").equals(artist.trim(), ignoreCase = true)
                var rewriteMs = 0L
                if (!needsCover && !needsTags) {
                    AppLog.d("Pipeline", "#$sourceToken tag & cover sudah benar, FFmpeg dilewati")
                } else {
                    rep.stage(JobStage.PROCESSING, DownloaderApp.str(R.string.pl_embed_cover))
                    val rewriteStart = System.currentTimeMillis()
                    applyAudioTagsBestEffort(
                        audio = target,
                        coverCandidates = if (needsCover) coverCandidates else emptyList(),
                        token = sourceToken,
                        metadataArgs = if (needsTags) audioMetadataArgs(title, artist, album) else emptyList(),
                    )
                    rewriteMs = System.currentTimeMillis() - rewriteStart
                    AppLog.d(
                        "Pipeline",
                        "#$sourceToken tag=${if (needsTags) "ditulis" else "sudah ada"} " +
                            "cover=${if (needsCover) "dipasang" else "sudah ada"}",
                    )
                }
                val detail = buildString {
                    append("valid=${fmtSec(validateMs)}s")
                    if (needsCover || needsTags) {
                        append(" tulis=${fmtSec(rewriteMs)}s")
                        append(" tag=${if (needsTags) "tulis" else "ada"}")
                        append(" cover=${if (needsCover) "pasang" else "ada"}")
                    } else {
                        append(" ffmpeg=dilewati")
                    }
                }
                recordStat(
                    sourceToken,
                    ConversionPlanner.modeLabel(ConversionPlanner.Operation.NATIVE),
                    downloadMs,
                    System.currentTimeMillis() - downloadStart - downloadMs,
                    target.length(),
                    0,
                    detail,
                    mergeMs,
                )
                rep.progress(92)
                return target
            }
            AppLog.d("Pipeline", "#$sourceToken sumber .$sourceExt tidak valid, lanjut konversi FFmpeg")
        }

        val cover = ThumbnailFetcher.downloadCover(context, coverCandidates, sourceDir, sourceToken)

        rep.stage(JobStage.PROCESSING, DownloaderApp.str(R.string.pl_convert_audio))
        rep.progress(82)
        rep.speed(0, null)

        val target = FileNames.uniqueFile(outDir, "${safeTitle}.${fmt}")
        val tmpTarget = File(outDir, "${target.name}.part")
        val outputFormat = if (fmt == "mp3") "mp3" else "mp4"
        val coverFile = listOfNotNull(cover.jpg, cover.raw)
            .firstOrNull { it.exists() && it.length() > 512 }
        val codec = if (fmt == "mp3") "libmp3lame" else "aac"

        // Tag ditulis dari metadata item itu sendiri (lihat `artist`/`album` di
        // atas), bukan dari nama pembuat playlist.
        val metadataArgs = audioMetadataArgs(title, artist, album)

        val progressHandler = FfmpegProgressHandler(rep, 82, 9)
        val baseArgs = mutableListOf("-y", "-progress", "pipe:1", "-threads", "0", "-i", sourceAudio.absolutePath)
        if (coverFile != null) {
            baseArgs += listOf(
                "-i", coverFile.absolutePath,
                "-map", "0:a",
                "-map", "1:v",
                "-vf", COVER_CROP_FILTER,
                "-c:v", "mjpeg",
                "-q:v", "3",
                "-disposition:v", "attached_pic",
            )
        } else {
            baseArgs += listOf("-map", "0:a")
        }
        baseArgs += listOf("-c:a", codec, "-b:a", "${bitrate}k", "-threads", "0")
        if (fmt == "mp3") baseArgs += listOf("-id3v2_version", "3")
        baseArgs += metadataArgs
        baseArgs += listOf("-f", outputFormat, tmpTarget.absolutePath)

        var attempts = 0
        val enc1Start = System.currentTimeMillis()
        var code = FfmpegEngine.run(context, baseArgs, "${jobId}_enc_1", progressHandler::onLine)
        val enc1Ms = System.currentTimeMillis() - enc1Start
        attempts++
        var enc2Ms = -1L
        if ((code != 0 || !tmpTarget.exists() || tmpTarget.length() <= 0) && coverFile != null) {
            tmpTarget.delete()
            val fallbackArgs = mutableListOf(
                "-y", "-progress", "pipe:1", "-threads", "0", "-i", sourceAudio.absolutePath,
                "-map", "0:a",
                "-c:a", codec, "-b:a", "${bitrate}k", "-threads", "0",
            )
            if (fmt == "mp3") fallbackArgs += listOf("-id3v2_version", "3")
            fallbackArgs += metadataArgs
            fallbackArgs += listOf("-f", outputFormat, tmpTarget.absolutePath)
            val enc2Start = System.currentTimeMillis()
            code = FfmpegEngine.run(context, fallbackArgs, "${jobId}_enc_2", progressHandler::onLine)
            enc2Ms = System.currentTimeMillis() - enc2Start
            attempts++
        }
        val validateStart = System.currentTimeMillis()
        val validOut = code == 0 && tmpTarget.exists() && tmpTarget.length() > 0 &&
            validateOutput(tmpTarget, fmt, MediaProbe.Expect.AUDIO) is Validation.Ok
        val validateMs = System.currentTimeMillis() - validateStart
        if (!validOut) {
            tmpTarget.delete()
            throw AppException("PROCESSING_FAILED", DownloaderApp.str(R.string.err_ffmpeg_audio))
        }
        val detail = buildString {
            append("enc1=${fmtSec(enc1Ms)}s")
            if (enc2Ms >= 0) append(" enc2=${fmtSec(enc2Ms)}s")
            append(" valid=${fmtSec(validateMs)}s")
        }
        recordStat(
            sourceToken,
            ConversionPlanner.modeLabel(ConversionPlanner.Operation.TRANSCODE),
            downloadMs,
            System.currentTimeMillis() - downloadStart - downloadMs,
            tmpTarget.length(),
            attempts,
            detail,
            mergeMs,
        )
        val moved = tmpTarget.renameTo(target)
        if (!moved) {
            tmpTarget.copyTo(target, overwrite = true)
            tmpTarget.delete()
        }
        rep.progress(92)
        return target
    }

    /**
     * Tulis tag (judul/artis/album) dan/atau pasang cover pada berkas audio
     * jalur native TANPA re-encode audio (stream copy).
     *
     * Jalur native berarti audio sudah sesuai format, jadi FFmpeg di sini hanya
     * menulis ulang pembungkusnya. Karena itu fungsi ini **dilewati sama sekali**
     * bila tidak ada yang perlu diubah (tag sudah benar dan cover sudah ada).
     * Best-effort: kalau gagal, berkas aslinya tetap dipakai.
     */
    private suspend fun applyAudioTagsBestEffort(
        audio: File,
        coverCandidates: List<String>,
        token: String,
        metadataArgs: List<String>,
    ) {
        val coverFile = if (coverCandidates.isEmpty()) {
            null
        } else {
            val cover = ThumbnailFetcher.downloadCover(context, coverCandidates, sourceDir, "${token}_embed")
            listOfNotNull(cover.jpg, cover.raw).firstOrNull { it.exists() && it.length() > 512 }
        }
        if (coverFile == null && metadataArgs.isEmpty()) return

        val fmt = audio.extension.lowercase()
        val tmp = File(audio.parentFile ?: return, "${audio.name}.part")
        val args = mutableListOf("-y", "-threads", "0", "-i", audio.absolutePath)
        if (coverFile != null) {
            args += listOf(
                "-i", coverFile.absolutePath,
                "-map", "0:a",
                "-map", "1:v",
                "-vf", COVER_CROP_FILTER,
                "-c:a", "copy",
                "-c:v", "mjpeg",
                "-q:v", "3",
                "-disposition:v", "attached_pic",
            )
        } else {
            args += listOf("-map", "0:a", "-c:a", "copy")
        }
        if (fmt == "mp3") args += listOf("-id3v2_version", "3")
        args += metadataArgs
        args += listOf("-f", if (fmt == "mp3") "mp3" else "mp4", tmp.absolutePath)

        val code = runCatching { FfmpegEngine.run(context, args, "${jobId}_tags_$token") }.getOrDefault(-1)
        if (code == 0 && tmp.exists() && tmp.length() > 0 && FileSignature.hasValidMagic(tmp, fmt)) {
            val moved = tmp.renameTo(audio)
            if (!moved) runCatching { tmp.copyTo(audio, overwrite = true) }
        }
        tmp.delete()
    }

    /**
     * Tag yang ditulis ke berkas audio. Dipakai jalur native (lewat
     * [applyAudioTagsBestEffort]) maupun jalur konversi.
     */
    private fun audioMetadataArgs(title: String, artist: String, album: String?): List<String> =
        buildList {
            add("-metadata")
            add("title=$title")
            add("-metadata")
            add("artist=$artist")
            if (!album.isNullOrBlank()) {
                add("-metadata")
                add("album=$album")
            }
        }

    private suspend fun processVideo(
        url: String,
        resolution: String,
        hd: Boolean,
        outDir: File,
        metaHint: NormalizedMetadata?,
        rep: JobReporter,
        sourceToken: String,
    ): File {
        rep.stage(JobStage.DOWNLOADING, DownloaderApp.str(R.string.pl_fetch_metadata))
        rep.progress(6)

        val info: JsonObject? = if (metaHint == null) {
            try {
                YtDlpEngine.dumpInfo(url, flatPlaylist = false)
            } catch (_: AppException) {
                null
            }
        } else {
            null
        }

        val title = metaHint?.media?.title?.takeIf { it.isNotBlank() && it != "Untitled Media" }
            ?: info?.stringOrNull("title")
            ?: DownloaderApp.str(R.string.media_type_video)
        val safeTitle = FileNames.sanitizeFilename(title)
        rep.title(title)

        val expected = File(outDir, "$safeTitle.mp4")
        if (resume && expected.exists() && expected.length() > 0 &&
            validateMp4(expected) is Validation.Ok
        ) {
            rep.progress(78)
            return expected
        }

        val formatSelector = buildVideoFormatSelector(resolution, hd)

        ensureFreeSpace(SizeEstimator.videoBytes(resolution, hd, metaHint?.media?.duration))
        rep.stage(JobStage.DOWNLOADING, DownloaderApp.str(R.string.pl_download_video))
        rep.progress(12)

        val processId = "${jobId}_video_${System.nanoTime()}"
        val request = YoutubeDLRequest(url)
            .addOption("-f", formatSelector)
            .addOption("--merge-output-format", "mp4")
            .addOption("-o", File(sourceDir, "source_video_${sourceToken}.%(ext)s").absolutePath)
            .addOption("--newline")
            .addOption("--no-warnings")
            .addOption("--no-check-certificates")
            .addOption("--concurrent-fragments", fragmentCount())
            .addOption("--no-playlist")
            .addOption("--continue")
            .addDownloadResilience()
            .addProgressTemplate()

        val downloadStart = System.currentTimeMillis()
        val mergeMs = executeDownload(request, processId, rep, DownloaderApp.str(R.string.err_download_generic))
        val downloadMs = System.currentTimeMillis() - downloadStart

        val sourceVideo = sourceDir.listFiles()
            ?.firstOrNull { it.name.startsWith("source_video_${sourceToken}.") && !it.name.endsWith(".part") }
            ?: throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.err_video_missing))

        rep.stage(JobStage.PROCESSING, DownloaderApp.str(R.string.pl_process_video))
        rep.progress(82)
        rep.speed(0, null)

        val target = FileNames.uniqueFile(outDir, "${safeTitle}.mp4")

        if (sourceVideo.name.lowercase().endsWith(".mp4")) {
            val nativeValidateStart = System.currentTimeMillis()
            val validNative = validateMp4(sourceVideo) is Validation.Ok
            val nativeValidateMs = System.currentTimeMillis() - nativeValidateStart
            if (validNative) {
                val moved = sourceVideo.renameTo(target)
                if (!moved) sourceVideo.copyTo(target, overwrite = true)
                recordStat(
                    sourceToken,
                    ConversionPlanner.modeLabel(ConversionPlanner.Operation.NATIVE),
                    downloadMs,
                    System.currentTimeMillis() - downloadStart - downloadMs,
                    sourceVideo.length(),
                    0,
                    "valid=${fmtSec(nativeValidateMs)}s",
                    mergeMs,
                )
                rep.progress(92)
                return target
            }
            AppLog.d("Pipeline", "#$sourceToken output .mp4 tidak valid, lanjut proses FFmpeg")
        }

        val tmpTarget = File(outDir, "${target.name}.part")

        // Keputusan diambil dari codec sumber, bukan dari ekstensi saja. Satu
        // probe sumber jauh lebih murah daripada percobaan `-c copy` yang
        // menyisipkan Opus ke MP4 lalu harus diulang dengan operasi lain.
        val sourceInfo = probeSource(sourceVideo)
        val plan = ConversionPlanner.planVideo(
            videoCodec = sourceInfo?.videoCodec(),
            audioCodec = sourceInfo?.audioCodec(),
        )
        AppLog.d(
            "Pipeline",
            "#$sourceToken rencana=${ConversionPlanner.modeLabel(plan)} " +
                "kontainer=.${sourceVideo.extension.lowercase()} " +
                "video=${sourceInfo?.videoCodec() ?: "?"} audio=${sourceInfo?.audioCodec() ?: "?"}",
        )

        val progressHandler = FfmpegProgressHandler(rep, 82, 9)
        val operationMs = linkedMapOf<String, Long>()
        var attempts = 0
        var mode = ConversionPlanner.modeLabel(plan)
        var validateMs = 0L
        var valid = false

        for (operation in ConversionPlanner.attempts(plan)) {
            val label = ConversionPlanner.modeLabel(operation)
            val run = runAttempt(
                videoArgs(operation, sourceVideo, tmpTarget),
                "${jobId}_$label",
                progressHandler::onLine,
            )
            attempts++
            operationMs[label] = run.second
            val validateStart = System.currentTimeMillis()
            valid = run.first == 0 && validateMp4(tmpTarget) is Validation.Ok
            validateMs += System.currentTimeMillis() - validateStart
            if (valid) {
                mode = label
                break
            }
            AppLog.d("Pipeline", "#$sourceToken operasi $label ditolak, naik ke cadangan")
            tmpTarget.delete()
        }
        if (!valid) {
            tmpTarget.delete()
            throw AppException("PROCESSING_FAILED", DownloaderApp.str(R.string.err_ffmpeg_video))
        }
        val detail = buildString {
            operationMs.forEach { (label, ms) -> append(" $label=${fmtSec(ms)}s") }
            append(" valid=${fmtSec(validateMs)}s")
        }
        recordStat(
            sourceToken, mode, downloadMs,
            System.currentTimeMillis() - downloadStart - downloadMs,
            tmpTarget.length(), attempts, detail, mergeMs,
        )
        val moved = tmpTarget.renameTo(target)
        if (!moved) {
            tmpTarget.copyTo(target, overwrite = true)
            tmpTarget.delete()
        }
        rep.progress(92)
        return target
    }

    private suspend fun runAttempt(
        args: List<String>,
        processId: String,
        onLine: (String) -> Unit,
    ): Pair<Int, Long> {
        val start = System.currentTimeMillis()
        val code = FfmpegEngine.run(context, args, processId, onLine)
        return code to (System.currentTimeMillis() - start)
    }

    /**
     * Argumen FFmpeg untuk satu operasi video.
     *
     * Sengaja identik dengan rangkaian lama (`copy` -> `remux` -> `encode`) agar
     * kualitas keluaran tidak berubah; yang berubah hanya keputusan operasi mana
     * yang perlu dijalankan lebih dulu.
     */
    private fun videoArgs(
        operation: ConversionPlanner.Operation,
        source: File,
        tmp: File,
    ): List<String> {
        val head = listOf("-y", "-progress", "pipe:1", "-threads", "0", "-i", source.absolutePath)
        val tail = listOf("-movflags", "+faststart", "-f", "mp4", tmp.absolutePath)
        return when (operation) {
            ConversionPlanner.Operation.REMUX -> head + listOf("-c", "copy") + tail
            ConversionPlanner.Operation.REMUX_AUDIO -> head + listOf("-c:v", "copy", "-c:a", "aac") + tail
            else -> head + listOf(
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-crf", "22",
                "-threads", "0",
                "-c:a", "aac",
            ) + tail
        }
    }

    private suspend fun processPhotoPost(
        pageUrl: String,
        meta: NormalizedMetadata,
        images: List<String>,
        outDir: File,
        rep: JobReporter,
    ): File {
        val limited = images.take(100)
        val total = limited.size
        val baseTitle = FileNames.sanitizeFilename(
            meta.media.title.takeIf { it.isNotBlank() && it != "Untitled Media" }
                ?: DownloaderApp.str(R.string.playlist_default_title),
            40,
        )
        rep.stage(JobStage.DOWNLOADING, DownloaderApp.str(R.string.pl_photopost, total))
        rep.items(0, total)

        if (resume) {
            val expectedZip = File(outDir, "$baseTitle.zip")
            if (total > 1 && expectedZip.exists() && expectedZip.length() > 0) {
                rep.progress(90)
                return expectedZip
            }
            if (total == 1) {
                val single = outDir.listFiles()
                    ?.firstOrNull {
                        it.name.startsWith("${baseTitle}_01") && it.length() > 0 &&
                            FileSignature.hasValidImage(it)
                    }
                if (single != null) {
                    rep.progress(90)
                    return single
                }
            }
        }

        ensureFreeSpace(null)
        val postDir = File(outDir, baseTitle).apply { mkdirs() }
        val saved = mutableListOf<File>()
        limited.forEachIndexed { i, imageUrl ->
            try {
                val rawExt = FileNames.extensionOf(imageUrl).removePrefix(".")
                val ext = rawExt.takeIf { it == "jpg" || it == "jpeg" || it == "png" || it == "webp" } ?: "jpg"
                val name = "${(i + 1).toString().padStart(2, '0')}_${baseTitle}.$ext"
                val target = FileNames.uniqueFile(postDir, name)
                val bytes = ThumbnailFetcher.fetchBytes(imageUrl)
                if (bytes != null && bytes.isNotEmpty()) {
                    target.writeBytes(bytes)
                    if (FileSignature.hasValidImage(target)) {
                        saved.add(normalizeImageExtension(target))
                    } else {
                        target.delete()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            rep.items(i + 1, total)
            rep.progress((5 + 85.0 * (i + 1) / total).toInt())
        }
        if (saved.isEmpty()) {
            throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.err_photo_empty))
        }
        if (saved.size == 1) {
            val single = saved.first()
            val moved = File(outDir, single.name)
            val final = if (single.renameTo(moved)) {
                moved
            } else {
                single.copyTo(moved, overwrite = true).also { single.delete() }
            }
            runCatching { postDir.deleteRecursively() }
            rep.progress(92)
            return final
        }
        rep.stage(JobStage.PACKAGING, DownloaderApp.str(R.string.pl_zip))
        rep.progress(92)
        val zip = File(outDir, "$baseTitle.zip")
        zipFolder(postDir, zip)
        runCatching { postDir.deleteRecursively() }
        return zip
    }

    private suspend fun processPhoto(
        url: String,
        outDir: File,
        rep: JobReporter,
    ): File {
        if (!FileNames.isDirectImage(url)) {
            throw AppException("UNSUPPORTED_SOURCE", DownloaderApp.str(R.string.err_photo_not_direct))
        }
        rep.stage(JobStage.DOWNLOADING, DownloaderApp.str(R.string.pl_download_photo))
        rep.progress(12)

        val baseName = FileNames.sanitizeFilename(
            url.substringBefore('?').substringBefore('#').substringAfterLast('/').ifBlank { "photo.jpg" },
        )

        val expected = File(outDir, baseName)
        if (resume && expected.exists() && expected.length() > 0 && FileSignature.hasValidImage(expected)) {
            rep.progress(78)
            return expected
        }

        ensureFreeSpace(null)
        val target = FileNames.uniqueFile(outDir, baseName)
        val meter = SpeedMeter()
        val bytes = ThumbnailFetcher.fetchBytesProgress(url) { downloaded, total ->
            val speed = meter.update(downloaded)
            rep.bytes(downloaded, total)
            if (speed > 0) rep.speed(speed, null)
            if (total > 0) {
                rep.progress((12 + 60.0 * downloaded / total).toInt().coerceIn(12, 72))
            }
        } ?: throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.err_photo_download))
        if (bytes.isEmpty()) {
            throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.err_photo_empty))
        }
        target.writeBytes(bytes)
        if (!FileSignature.hasValidImage(target)) {
            target.delete()
            throw AppException("PROCESSING_FAILED", DownloaderApp.str(R.string.err_photo_invalid))
        }
        val normalized = normalizeImageExtension(target)
        rep.bytes(bytes.size.toLong(), bytes.size.toLong())
        rep.speed(0, null)
        rep.progress(78)
        return normalized
    }

    /**
     * URL sering tidak menyebut ekstensi asli (mis. tautan CDN tanpa ekstensi
     * atau .jpg padahal isinya PNG). Setelah signature dibaca, nama file
     * disesuaikan dengan isi agar pemutar/galeri mengenali formatnya.
     */
    private fun normalizeImageExtension(file: File): File {
        val kind = FileSignature.imageExtension(file) ?: return file
        val current = file.extension.lowercase(Locale.ROOT)
        if (current == kind || (kind == "jpg" && current == "jpeg")) return file
        val parent = file.parentFile ?: return file
        val renamed = FileNames.uniqueFile(parent, "${file.nameWithoutExtension}.$kind")
        return if (file.renameTo(renamed)) renamed else file
    }

    private fun YoutubeDLRequest.addDownloadResilience(): YoutubeDLRequest = apply {
        addOption("--retries", 10)
        addOption("--fragment-retries", 10)
        addOption("--file-access-retries", 3)
        addOption("--socket-timeout", 30)
    }

    private fun YoutubeDLRequest.addProgressTemplate(): YoutubeDLRequest =
        addOption("--progress-template", PROGRESS_TEMPLATE)

    /**
     * Jalankan unduhan yt-dlp sambil mengukur berapa lama bagian merge-nya.
     *
     * Merge dan post-processing lain dijalankan FFmpeg **di dalam** proses
     * yt-dlp, jadi tanpa penanda keluaran `[Merger]` waktunya tidak akan pernah
     * terpisah dari waktu unduh.
     *
     * @return durasi merge dalam milidetik, atau `-1` bila yt-dlp tidak merge.
     */
    private suspend fun executeDownload(
        request: YoutubeDLRequest,
        processId: String,
        rep: JobReporter,
        defaultMessage: String,
    ): Long {
        val meter = SpeedMeter()
        var mergeStartMs = -1L
        val onLine: (Float, Long, String) -> Unit = { _, _, line ->
            if (mergeStartMs < 0 && line.contains("[Merger]")) {
                mergeStartMs = System.currentTimeMillis()
                AppLog.d("Pipeline", "#$processId mulai merge")
            }
            val sample = parseProgressLine(line)
            if (sample != null) {
                val measured = meter.update(sample.downloadedBytes)
                val speed = if (measured > 0) measured else sample.speedBps
                rep.bytes(sample.downloadedBytes, sample.totalBytes)
                if (speed > 0 || sample.etaSeconds > 0) {
                    rep.speed(speed, sample.etaSeconds.takeIf { it > 0 })
                }
                if (sample.totalBytes > 0) {
                    val percent = (12 + 60.0 * sample.downloadedBytes / sample.totalBytes).toInt()
                    rep.progress(percent.coerceIn(12, 72))
                }
            }
        }

        try {
            YtDlpEngine.execute(request, processId, onLine)
            rep.speed(0, null)
            rep.progress(75)
        } catch (e: YoutubeDLException) {
            val raw = e.message ?: ""
            if ("403" in raw) {
                request.addOption("--force-ipv4")
                try {
                    YtDlpEngine.execute(request, "$processId-v4", onLine)
                    rep.speed(0, null)
                    rep.progress(75)
                } catch (retry: YoutubeDLException) {
                    throw AppException(
                        "DOWNLOAD_FAILED",
                        ErrorMapper.friendly(retry.message ?: defaultMessage),
                    )
                }
            } else {
                throw AppException(
                    "DOWNLOAD_FAILED",
                    ErrorMapper.friendly(raw.ifBlank { defaultMessage }),
                )
            }
        }
        return if (mergeStartMs >= 0) {
            (System.currentTimeMillis() - mergeStartMs).coerceAtLeast(0)
        } else {
            -1L
        }
    }

    private fun parseProgressLine(line: String): ProgressSample? {
        if (!line.startsWith(PROGRESS_PREFIX)) return null
        val parts = line.removePrefix(PROGRESS_PREFIX).split("|")
        if (parts.size < 5) return null
        val downloaded = parts[0].trim().toDoubleOrNull()?.toLong() ?: -1L
        if (downloaded < 0) return null
        val total = parts[1].trim().toDoubleOrNull()?.toLong()
            ?: parts[2].trim().toDoubleOrNull()?.toLong()
            ?: -1L
        val speed = parts[3].trim().toDoubleOrNull()?.toLong() ?: -1L
        val eta = parts[4].trim().toDoubleOrNull()?.toLong() ?: -1L
        return ProgressSample(
            downloadedBytes = downloaded,
            totalBytes = total,
            speedBps = speed.coerceAtLeast(0),
            etaSeconds = eta,
        )
    }

    private fun buildVideoFormatSelector(resolution: String, hd: Boolean): String {
        val height = resolution.toIntOrNull()
        return when {
            resolution != "auto" && height != null ->
                "bestvideo[height<=$height][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=$height]+bestaudio/best[height<=$height]/best"

            hd ->
                "bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best"

            else ->
                "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=720]+bestaudio/best[height<=720]/best"
        }
    }

    private suspend fun enumeratePlaylist(url: String): List<PlaylistItem>? {
        val info = YtDlpEngine.dumpInfo(url, flatPlaylist = true)
        val arr = info["entries"] as? JsonArray ?: return null
        val items = arr.mapNotNull { it as? JsonObject }.mapIndexed { idx, entry ->
            PlaylistItem(
                index = idx + 1,
                id = entry.stringOrNull("id") ?: "item_${idx + 1}",
                title = entry.stringOrNull("title") ?: "Item ${idx + 1}",
                duration = entry.intOrNull("duration"),
                thumbnail = entry.stringOrNull("thumbnail") ?: lastThumbnailUrl(entry),
                url = entry.stringOrNull("url") ?: entry.stringOrNull("webpage_url"),
                // Artis milik lagu ini, bukan pemilik playlist (lihat PlaylistItem).
                artist = entry.stringOrNull("artist")
                    ?: entry.stringOrNull("uploader")
                    ?: entry.stringOrNull("channel"),
            )
        }
        return items.ifEmpty { null }
    }

    private fun resolveItemUrl(item: PlaylistItem): String? {
        val raw = item.url ?: item.id
        if (raw.isBlank()) return null
        if (raw.startsWith("http")) return raw
        return if (Regex("^[A-Za-z0-9_-]{6,}$").matches(raw)) {
            "https://www.youtube.com/watch?v=$raw"
        } else {
            null
        }
    }

    private suspend fun processPlaylistItem(
        request: DownloadRequest,
        meta: NormalizedMetadata,
        item: PlaylistItem,
        targetDir: File,
        rep: JobReporter,
    ): File {
        val itemUrl = resolveItemUrl(item)
            ?: throw AppException("DOWNLOAD_FAILED", DownloaderApp.str(R.string.pl_item_url_missing))
        val hint = itemMetaHint(item, meta, request)
        val token = "item_${item.index}"
        return when (request.format) {
            "mp3", "m4a" -> processAudio(itemUrl, request.format, request.bitrate, targetDir, hint, rep, token)
            "mp4" -> processVideo(itemUrl, request.resolution, request.hd, targetDir, hint, rep, token)
            "photo" -> processPhoto(itemUrl, targetDir, rep)
            else -> throw AppException(
                "PROCESSING_FAILED",
                DownloaderApp.str(R.string.err_format_unsupported, request.format),
            )
        }
    }

    private fun itemsConcurrency(format: String): Int {
        val turbo = PrefsRepository.turboValue()
        return when (format) {
            "mp4" -> if (turbo) 5 else 3
            "mp3" -> if (turbo) 8 else 4
            else -> if (turbo) 12 else 6
        }
    }

    /**
     * Jumlah fragmen paralel per unduhan. Dikurangi dari 8/16 menjadi 4/8 agar
     * playlist besar tidak membuka puluhan koneksi serentak (mis. 8 item audio
     * × 8 fragmen = 64 koneksi). Kecepatan per item sedikit turun, tetapi
     * stabilitas jaringan dan suhu perangkat jauh lebih baik.
     */
    private fun fragmentCount(): Int = if (PrefsRepository.turboValue()) 8 else 4

    private suspend fun <T> runParallel(
        items: List<T>,
        concurrency: Int,
        onCanceled: (suspend (T) -> Unit)? = null,
        block: suspend (T) -> Unit,
    ) = coroutineScope {
        val gate = Semaphore(concurrency.coerceAtLeast(1))
        items.map { item ->
            item to async { gate.withPermit { block(item) } }
        }.forEach { (item, deferred) ->
            try {
                deferred.await()
            } catch (e: ItemCanceledException) {
                onCanceled?.invoke(item)
            }
        }
    }

    private fun itemMetaHint(
        item: PlaylistItem,
        parent: NormalizedMetadata,
        request: DownloadRequest,
    ): NormalizedMetadata = NormalizedMetadata(
        source = SourceInfo(url = parent.source.url, platform = parent.source.platform),
        media = MediaInfo(
            type = if (request.format == "photo") "photo" else parent.media.type,
            isPlaylist = false,
            title = item.title,
            // SENGAJA tidak mewarisi artist/creator dari playlist: pada playlist,
            // keduanya milik PEMBUAT playlist (mis. "alanzobros"), bukan artis
            // lagunya. Nilai asli per item diambil dari metadata unduhan itu
            // sendiri; kalau tidak ada, lebih baik kosong daripada salah nama.
            creator = item.artist,
            artist = item.artist,
            album = null,
            thumbnail = item.thumbnail ?: parent.media.thumbnail,
            duration = item.duration,
        ),
        capabilities = parent.capabilities,
        formats = parent.formats,
        playlist = null,
    )

    private fun zipFolder(folder: File, zipFile: File) {
        val basePath = folder.parentFile?.toPath() ?: folder.toPath()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            folder.walkTopDown().filter { it.isFile }.forEach { file ->
                val entryName = basePath.relativize(file.toPath()).toString().replace('\\', '/')
                val entry = ZipEntry(entryName).apply {
                    method = ZipEntry.STORED
                    size = file.length()
                    compressedSize = file.length()
                    crc = crc32(file)
                }
                zos.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    private fun crc32(file: File): Long {
        val crc = CRC32()
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                crc.update(buffer, 0, read)
            }
        }
        return crc.value
    }

    private data class ProgressSample(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBps: Long,
        val etaSeconds: Long,
    )

    companion object {
        private const val PROGRESS_PREFIX = "dlprog:"
        private const val PROGRESS_TEMPLATE =
            "download:dlprog:%(progress.downloaded_bytes)s|%(progress.total_bytes)s|%(progress.total_bytes_estimate)s|%(progress.speed)s|%(progress.eta)s"

        /**
         * Sampul album ditanam sebagai kotak 1:1 (center-crop) — tanpa ini
         * thumbnail platform (YouTube dll) yang aslinya 16:9 membuat album art
         * terlihat melebar di aplikasi berkas/pemutar lain. Konsisten dengan
         * tampilan persegi di dalam aplikasi.
         */
        private const val COVER_CROP_FILTER = "crop=min(iw\\,ih):min(iw\\,ih)"
    }
}

private class ProgressMapper(
    private val delegate: JobReporter,
    private val base: Int,
    private val span: Int,
) : JobReporter {

    override fun stage(stage: JobStage, message: String?) = delegate.stage(stage, message)

    override fun progress(percent: Int) {
        val mapped = base + (span * (percent.coerceIn(0, 100) / 100f)).toInt()
        delegate.progress(mapped)
    }

    override fun bytes(downloaded: Long, total: Long) = delegate.bytes(downloaded, total)

    override fun speed(bytesPerSecond: Long, etaSeconds: Long?) = delegate.speed(bytesPerSecond, etaSeconds)

    override fun items(current: Int, total: Int) = delegate.items(current, total)

    override fun title(title: String) = delegate.title(title)
}

/**
 * Menerjemahkan output `-progress pipe:1` + banner FFmpeg menjadi persen
 * konversi nyata (bukan persen statis). Diam saja bila durasi sumber tidak
 * terdeteksi (mis. stream live) agar tidak menampilkan angka palsu.
 */
private class FfmpegProgressHandler(
    private val rep: JobReporter,
    private val base: Int,
    private val span: Int,
) {

    private var durationUs: Long = -1L
    private var lastPercent: Int = -1

    fun onLine(line: String) {
        if (durationUs <= 0) {
            FfmpegProgress.parseDurationUs(line)?.let { durationUs = it }
            return
        }
        val outUs = FfmpegProgress.parseOutTimeUs(line) ?: return
        val ratio = (outUs.toDouble() / durationUs).coerceIn(0.0, 1.0)
        val percent = base + (span * ratio).toInt()
        if (percent > lastPercent) {
            lastPercent = percent
            rep.progress(percent)
        }
    }
}

/**
 * Sink progress untuk satu item playlist paralel.
 *
 * Persen unduhan item diteruskan sebagai status per-item, **dan** dilaporkan
 * ke [PlaylistProgressTracker] lewat [onPercent] supaya bar tingkat job ikut
 * bergerak selagi item masih berjalan (bukan hanya saat item selesai).
 */
private class ItemSink(
    private val delegate: JobReporter,
    private val index: Int,
    private val onPercent: (Int) -> Unit = {},
) : JobReporter {

    private var lastPercent = -1
    private var processing = false

    override fun stage(stage: JobStage, message: String?) {
        // Unduhan item selesai -> tandai Processing supaya pengguna bisa
        // membedakan "masih mengunduh" dari "sedang merge/konversi".
        if (!processing && (stage == JobStage.PROCESSING || stage == JobStage.PACKAGING)) {
            processing = true
            delegate.item(index, ItemStatus.PROCESSING)
        }
        delegate.stage(stage, message)
    }

    override fun progress(percent: Int) = onPercent(percent.coerceIn(0, 100))

    override fun bytes(downloaded: Long, total: Long) {
        if (total <= 0) return
        val percent = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
        if (percent != lastPercent) {
            lastPercent = percent
            delegate.item(index, ItemStatus.RUNNING, percent = percent)
            onPercent(percent)
        }
    }

    override fun speed(bytesPerSecond: Long, etaSeconds: Long?) = delegate.speed(bytesPerSecond, etaSeconds)

    override fun items(current: Int, total: Int) = delegate.items(current, total)

    override fun title(title: String) = Unit

    override fun itemsInit(items: List<ItemState>) = Unit

    override fun item(index: Int, status: ItemStatus, message: String?, percent: Int) = Unit
}

/**
 * Penghitung progres tingkat job untuk playlist.
 *
 * Menyimpan jumlah item yang sudah tuntas dan persen unduhan item yang masih
 * berjalan, lalu menerbitkan persen job memakai rumus murni [PlaylistProgress]
 * (diuji terpisah). Sebelumnya persen hanya naik saat item selesai, sehingga
 * bar mandek selama item-item pertama belum rampung.
 *
 * Penerbitan ditahan bila persen bulatnya tidak berubah, supaya beberapa
 * coroutine item tidak membanjiri UI dengan pembaruan yang sama.
 */
private class PlaylistProgressTracker(
    private val reporter: JobReporter,
    private val total: Int,
) {

    private val done = AtomicInteger(0)
    private val inFlight = ConcurrentHashMap<Int, Int>()
    private val lastPublished = AtomicInteger(-1)

    init {
        // Terbitkan titik awal supaya bar tidak tertinggal di persen tahap
        // analisis selama item pertama belum melaporkan apa pun.
        publish()
    }

    /** Persen unduhan item [index] berubah; segera terbitkan persen job baru. */
    fun onItemPercent(index: Int, percent: Int) {
        inFlight[index] = percent.coerceIn(0, 100)
        publish()
    }

    /** Item [index] tuntas (berhasil, gagal, atau dibatalkan). */
    fun onItemFinished(index: Int): Int {
        inFlight.remove(index)
        val finished = done.incrementAndGet()
        reporter.items(finished, total)
        publish()
        return finished
    }

    private fun publish() {
        val percent = PlaylistProgress.percent(done.get(), inFlight.values, total)
        if (percent != lastPublished.getAndSet(percent)) reporter.progress(percent)
    }
}
