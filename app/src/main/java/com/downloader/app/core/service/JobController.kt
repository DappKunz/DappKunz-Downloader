package com.downloader.app.core.service

import android.content.Context
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.AppException
import com.downloader.app.core.ErrorMapper
import com.downloader.app.core.engine.EngineManager
import com.downloader.app.core.log.AppLog
import com.downloader.app.core.model.DownloadRequest
import com.downloader.app.core.model.DownloadResult
import com.downloader.app.core.model.ItemState
import com.downloader.app.core.model.ItemStatus
import com.downloader.app.core.model.JobReporter
import com.downloader.app.core.model.JobStage
import com.downloader.app.core.pipeline.DownloadPipeline
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.util.Formatters
import com.downloader.app.core.util.Redact
import com.downloader.app.data.HistoryRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

object JobController {

    data class UiState(
        val jobId: String,
        val url: String,
        val title: String,
        val platform: String,
        val thumbnail: String?,
        val format: String,
        val qualityLabel: String,
        val stage: JobStage,
        val progress: Int = -1,
        val downloadedBytes: Long = 0,
        val totalBytes: Long = 0,
        val speedBps: Long = 0,
        val etaSeconds: Long = -1,
        val currentItem: Int = 0,
        val totalItems: Int = 0,
        val items: List<ItemState> = emptyList(),
        val message: String? = null,
        val result: DownloadResult? = null,
        val error: AppException? = null,
        val request: DownloadRequest,
        val createdAt: Long,
    ) {
        val isActive: Boolean get() = stage.isActive
        val isPaused: Boolean get() = stage == JobStage.PAUSED
        val progressPercent: Int? get() = progress.takeIf { it in 0..100 }
    }

    sealed interface JobEvent {
        data class Completed(val jobId: String, val title: String, val result: DownloadResult) : JobEvent
        data class Failed(val jobId: String, val title: String, val message: String) : JobEvent
        data class Cancelled(val jobId: String, val title: String) : JobEvent
    }

    private const val JOB_TIMEOUT_MS = 120 * 60 * 1000L

    private fun maxConcurrent(): Int = if (PrefsRepository.turboValue()) 3 else 2

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _jobs = MutableStateFlow<List<UiState>>(emptyList())
    val jobs: StateFlow<List<UiState>> = _jobs.asStateFlow()

    private val _events = MutableSharedFlow<JobEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<JobEvent> = _events.asSharedFlow()

    private val runners = ConcurrentHashMap<String, Job>()
    private val resumedIds = ConcurrentHashMap.newKeySet<String>()
    private val canceledItems = ConcurrentHashMap<String, MutableSet<Int>>()
    private val activePipelines = ConcurrentHashMap<String, DownloadPipeline>()

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var restoredQueue = false

    private val persistGuard = Any()

    @Synchronized
    fun enqueue(context: Context, request: DownloadRequest): String? {
        val url = request.url.trim()
        if (url.isBlank()) return null
        if (_jobs.value.any { it.isActive && it.url == url }) return null

        val appContext = context.applicationContext
        this.appContext = appContext
        val jobId = "job_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val entry = UiState(
            jobId = jobId,
            url = url,
            title = request.meta?.media?.title?.takeIf { it.isNotBlank() } ?: url,
            platform = request.meta?.source?.platform ?: "unknown",
            thumbnail = request.meta?.media?.thumbnail,
            format = request.format,
            qualityLabel = Formatters.qualityLabel(request.format, request.bitrate, request.resolution, request.hd),
            stage = JobStage.QUEUED,
            message = DownloaderApp.str(R.string.stage_queued),
            request = request,
            createdAt = System.currentTimeMillis(),
        )
        _jobs.update { listOf(entry) + it }
        AppLog.d("Job", "Antrean $jobId • ${request.format} • ${Redact.url(url).take(90)}")
        persistQueue()
        startNext(appContext)
        return jobId
    }

    /**
     * Pulihkan antrean dari disk (dipanggil sekali saat Application dibuat).
     * Job yang sedang berjalan saat proses mati dikembalikan sebagai PAUSED
     * agar bisa dilanjutkan tanpa mengunduh ulang item yang sudah selesai.
     */
    @Synchronized
    fun restore(context: Context) {
        appContext = context.applicationContext
        if (restoredQueue) return
        restoredQueue = true

        val saved = JobQueueStore.load(context)
        val existingIds = _jobs.value.map { it.jobId }.toSet()
        val pending = saved.filter { it.jobId !in existingIds }
        val restoredJobs = pending.map { p ->
            val stage = if (p.stage == JobStage.FAILED) JobStage.FAILED else JobStage.PAUSED
            UiState(
                jobId = p.jobId,
                url = p.url,
                title = p.title,
                platform = p.platform,
                thumbnail = p.thumbnail,
                format = p.format,
                qualityLabel = p.qualityLabel,
                stage = stage,
                message = if (stage == JobStage.PAUSED) DownloaderApp.str(R.string.job_restored_paused) else null,
                error = p.errorMessage?.let { AppException("RESTORED", it) },
                currentItem = p.currentItem,
                totalItems = p.totalItems,
                items = p.items.map { item ->
                    if (item.status == ItemStatus.RUNNING || item.status == ItemStatus.PROCESSING) {
                        item.copy(status = ItemStatus.WAITING, percent = -1, message = null)
                    } else {
                        item
                    }
                },
                request = p.request,
                createdAt = p.createdAt,
            )
        }
        if (restoredJobs.isNotEmpty()) {
            _jobs.update { (restoredJobs + it).sortedByDescending { job -> job.createdAt } }
            AppLog.d("Job", "Pulihkan antrean: ${restoredJobs.size} job")
        }
        cleanupWorkDirs(saved.map { it.jobId }.toSet() + existingIds)
        persistQueue()
    }

    private fun persistQueue() {
        val context = appContext ?: return
        scope.launch {
            val snapshot = _jobs.value
                .filter { JobQueueStore.shouldPersist(it.stage) }
                .map { it.toPersisted() }
            synchronized(persistGuard) {
                JobQueueStore.save(context, snapshot)
            }
        }
    }

    private fun UiState.toPersisted(): PersistedJob = PersistedJob(
        jobId = jobId,
        url = url,
        title = title,
        platform = platform,
        thumbnail = thumbnail,
        format = format,
        qualityLabel = qualityLabel,
        stage = stage,
        message = message,
        errorMessage = error?.message,
        currentItem = currentItem,
        totalItems = totalItems,
        items = items,
        request = request,
        createdAt = createdAt,
    )

    private fun cleanupWorkDirs(keepIds: Set<String>) {
        val context = appContext ?: return
        scope.launch {
            runCatching {
                File(context.filesDir, "jobs").listFiles()?.forEach { dir ->
                    if (dir.name !in keepIds) dir.deleteRecursively()
                }
                File(context.cacheDir, "jobs").deleteRecursively()
            }
        }
    }

    @Synchronized
    fun pause(context: Context, jobId: String) {
        val state = find(jobId) ?: return
        if (!state.isActive || state.stage == JobStage.PAUSED) return
        update(jobId) {
            it.copy(
                stage = JobStage.PAUSED,
                message = DownloaderApp.str(R.string.stage_paused),
                speedBps = 0,
                etaSeconds = -1,
            )
        }
        AppLog.d("Job", "Jeda $jobId")
        persistQueue()
        runners[jobId]?.cancel()
    }

    @Synchronized
    fun resume(context: Context, jobId: String) {
        val state = find(jobId) ?: return
        if (state.stage != JobStage.PAUSED) return
        resumedIds.add(jobId)
        update(jobId) {
            it.copy(
                stage = JobStage.QUEUED,
                message = DownloaderApp.str(R.string.pl_resume),
                progress = -1,
                speedBps = 0,
                etaSeconds = -1,
            )
        }
        AppLog.d("Job", "Lanjut $jobId")
        persistQueue()
        startNext(context.applicationContext)
    }

    /**
     * Ulangi job yang gagal memakai workdir yang dipertahankan (resume):
     * item yang sudah selesai di playlist tidak diunduh ulang.
     */
    @Synchronized
    fun retry(context: Context, jobId: String) {
        val state = find(jobId) ?: return
        if (state.stage != JobStage.FAILED) return
        resumedIds.add(jobId)
        update(jobId) {
            it.copy(
                stage = JobStage.QUEUED,
                message = DownloaderApp.str(R.string.pl_resume),
                progress = -1,
                downloadedBytes = 0,
                totalBytes = 0,
                speedBps = 0,
                etaSeconds = -1,
                currentItem = 0,
                error = null,
            )
        }
        AppLog.d("Job", "Coba lagi $jobId")
        persistQueue()
        startNext(context.applicationContext)
    }

    /**
     * Batalkan satu item playlist. Item yang belum mulai akan dilewati,
     * item yang sedang berjalan dihentikan (proses yt-dlp/FFmpeg dimatikan).
     * Pembatalan bertahan untuk resume/retry job yang sama.
     */
    @Synchronized
    fun cancelItem(context: Context, jobId: String, index: Int) {
        val state = find(jobId) ?: return
        if (!state.isActive) return
        canceledItems.computeIfAbsent(jobId) { ConcurrentHashMap.newKeySet() }.add(index)
        update(jobId) { st ->
            st.copy(
                items = st.items.map { item ->
                    if (item.index == index && item.status != ItemStatus.COMPLETED) {
                        item.copy(status = ItemStatus.CANCELED, percent = -1, message = null)
                    } else {
                        item
                    }
                },
            )
        }
        activePipelines[jobId]?.cancelItem(index)
        AppLog.d("Job", "Batal item #$index • $jobId")
        persistQueue()
    }

    @Synchronized
    fun cancel(context: Context, jobId: String) {
        val state = find(jobId) ?: return
        if (!state.isActive) return
        val appContext = context.applicationContext
        update(jobId) {
            it.copy(
                stage = JobStage.CANCELLED,
                message = null,
                result = null,
                error = null,
                speedBps = 0,
                etaSeconds = -1,
            )
        }
        runners.remove(jobId)?.cancel()
        HistoryRepository.addCancelled(appContext, state.request)
        AppLog.d("Job", "Batal $jobId")
        persistQueue()
        _events.tryEmit(JobEvent.Cancelled(jobId, state.title))
        startNext(appContext)
    }

    @Synchronized
    fun cancelAll(context: Context) {
        _jobs.value.filter { it.isActive }.forEach { cancel(context, it.jobId) }
    }

    @Synchronized
    fun remove(context: Context, jobId: String) {
        val state = find(jobId) ?: return
        if (state.isActive) return
        _jobs.update { list -> list.filterNot { it.jobId == jobId } }
        resumedIds.remove(jobId)
        canceledItems.remove(jobId)
        runCatching { File(context.filesDir, "jobs/$jobId").deleteRecursively() }
        persistQueue()
    }

    fun activeCount(): Int = _jobs.value.count { it.isActive }

    private fun find(jobId: String): UiState? = _jobs.value.firstOrNull { it.jobId == jobId }

    private fun stoppedByUser(jobId: String): Boolean {
        val stage = find(jobId)?.stage ?: return true
        return stage == JobStage.PAUSED || stage == JobStage.CANCELLED || stage == JobStage.QUEUED
    }

    private inline fun update(jobId: String, transform: (UiState) -> UiState) {
        _jobs.update { list -> list.map { if (it.jobId == jobId) transform(it) else it } }
    }

    @Synchronized
    private fun startNext(context: Context) {
        val running = _jobs.value.count { it.stage.isRunning }
        if (running >= maxConcurrent()) return
        val next = _jobs.value.lastOrNull { it.stage == JobStage.QUEUED } ?: return
        launchJob(context, next.jobId)
    }

    @Synchronized
    private fun launchJob(context: Context, jobId: String) {
        val existing = runners[jobId]
        if (existing != null) {
            scope.launch {
                existing.join()
                startNext(context.applicationContext)
            }
            return
        }
        val state = find(jobId) ?: return
        val request = state.request
        val isResume = resumedIds.contains(jobId)
        val canceled = canceledItems.computeIfAbsent(jobId) { ConcurrentHashMap.newKeySet() }
        state.items.forEach { if (it.status == ItemStatus.CANCELED) canceled.add(it.index) }
        update(jobId) {
            it.copy(
                stage = JobStage.DOWNLOADING,
                message = DownloaderApp.str(if (isResume) R.string.pl_resume else R.string.pl_preparing),
                error = null,
            )
        }
        val runner = scope.launch {
            runJob(context, jobId, request, isResume)
        }
        runners[jobId] = runner
        runner.invokeOnCompletion { runners.remove(jobId) }
        persistQueue()
    }

    private suspend fun runJob(
        context: Context,
        jobId: String,
        request: DownloadRequest,
        isResume: Boolean,
    ) {
        val reporter = object : JobReporter {
            override fun stage(stage: JobStage, message: String?) =
                update(jobId) { it.copy(stage = stage, message = message) }

            override fun progress(percent: Int) =
                update(jobId) { it.copy(progress = percent.coerceIn(0, 100)) }

            override fun bytes(downloaded: Long, total: Long) =
                update(jobId) {
                    it.copy(
                        downloadedBytes = downloaded.coerceAtLeast(0),
                        totalBytes = total.coerceAtLeast(0),
                    )
                }

            override fun speed(bytesPerSecond: Long, etaSeconds: Long?) =
                update(jobId) {
                    it.copy(
                        speedBps = bytesPerSecond.coerceAtLeast(0),
                        etaSeconds = etaSeconds ?: -1,
                    )
                }

            override fun items(current: Int, total: Int) =
                update(jobId) { it.copy(currentItem = current, totalItems = total) }

            override fun title(title: String) {
                if (title.isNotBlank()) update(jobId) { it.copy(title = title) }
            }

            override fun itemsInit(items: List<ItemState>) =
                update(jobId) { it.copy(items = items) }

            override fun item(index: Int, status: ItemStatus, message: String?, percent: Int) =
                update(jobId) { st ->
                    st.copy(
                        items = st.items.map { item ->
                            if (item.index != index) {
                                item
                            } else {
                                item.copy(
                                    status = status,
                                    message = if (status == ItemStatus.FAILED) message else null,
                                    percent = when {
                                        percent >= 0 -> percent
                                        status == ItemStatus.COMPLETED -> 100
                                        else -> -1
                                    },
                                )
                            }
                        },
                    )
                }
        }

        try {
            EngineManager.awaitReady()
            val pipeline = DownloadPipeline(
                context = context,
                jobId = jobId,
                reporter = reporter,
                resume = isResume,
                keepWorkDir = { find(jobId)?.isPaused == true },
                isItemCanceled = { index -> canceledItems[jobId]?.contains(index) == true },
            )
            activePipelines[jobId] = pipeline
            val result = withTimeout(JOB_TIMEOUT_MS) {
                pipeline.run(request)
            }
            update(jobId) {
                it.copy(
                    stage = JobStage.COMPLETED,
                    progress = 100,
                    speedBps = 0,
                    etaSeconds = -1,
                    message = null,
                    result = result,
                )
            }
            HistoryRepository.addCompleted(context, request, result)
            AppLog.d("Job", "Selesai $jobId • ${find(jobId)?.title ?: request.url}")
            _events.tryEmit(JobEvent.Completed(jobId, find(jobId)?.title ?: request.url, result))
        } catch (e: TimeoutCancellationException) {
            if (stoppedByUser(jobId)) return
            val message = DownloaderApp.str(R.string.err_job_timeout)
            update(jobId) {
                it.copy(
                    stage = JobStage.FAILED,
                    speedBps = 0,
                    etaSeconds = -1,
                    message = null,
                    error = AppException("JOB_TIMEOUT", message),
                )
            }
            HistoryRepository.addFailed(context, request, message)
            AppLog.e("Job", "Timeout $jobId")
            _events.tryEmit(JobEvent.Failed(jobId, find(jobId)?.title ?: request.url, message))
        } catch (e: CancellationException) {
            update(jobId) {
                if (
                    it.stage == JobStage.PAUSED ||
                    it.stage == JobStage.CANCELLED ||
                    it.stage == JobStage.QUEUED
                ) {
                    it
                } else {
                    it.copy(stage = JobStage.CANCELLED, speedBps = 0, etaSeconds = -1, message = null)
                }
            }
        } catch (e: AppException) {
            if (!stoppedByUser(jobId)) {
                update(jobId) {
                    it.copy(
                        stage = JobStage.FAILED,
                        speedBps = 0,
                        etaSeconds = -1,
                        message = null,
                        error = e,
                    )
                }
                HistoryRepository.addFailed(context, request, e.message)
                AppLog.e("Job", "Gagal $jobId: ${e.message}")
                _events.tryEmit(JobEvent.Failed(jobId, find(jobId)?.title ?: request.url, e.message))
            }
        } catch (e: Exception) {
            if (!stoppedByUser(jobId)) {
                val message = ErrorMapper.friendly(e.message)
                update(jobId) {
                    it.copy(
                        stage = JobStage.FAILED,
                        speedBps = 0,
                        etaSeconds = -1,
                        message = null,
                        error = AppException("DOWNLOAD_FAILED", message),
                    )
                }
                HistoryRepository.addFailed(context, request, message)
                AppLog.e("Job", "Gagal $jobId: $message")
                _events.tryEmit(JobEvent.Failed(jobId, find(jobId)?.title ?: request.url, message))
            }
        } finally {
            activePipelines.remove(jobId)
            persistQueue()
            startNext(context.applicationContext)
        }
    }
}
