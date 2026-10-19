package com.downloader.app.ui.home

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downloader.app.core.service.DownloadService
import com.downloader.app.core.service.JobController
import com.downloader.app.data.HistoryRepository
import com.downloader.app.data.HistoryStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    data class Stats(
        val completed: Int = 0,
        val active: Int = 0,
        val totalBytes: Long = 0,
    )

    val jobs: StateFlow<List<JobController.UiState>> = JobController.jobs
    val history = HistoryRepository.entries

    val stats: StateFlow<Stats> = combine(HistoryRepository.entries, JobController.jobs) { entries, jobs ->
        Stats(
            completed = entries.count { it.status == HistoryStatus.COMPLETED },
            active = jobs.count { it.isActive },
            totalBytes = entries
                .filter { it.status == HistoryStatus.COMPLETED }
                .sumOf { it.sizeBytes },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    fun pause(jobId: String) = JobController.pause(getApplication(), jobId)

    fun resume(jobId: String) {
        JobController.resume(getApplication(), jobId)
        ensureService()
    }

    fun retry(jobId: String) {
        JobController.retry(getApplication(), jobId)
        ensureService()
    }

    fun cancel(jobId: String) {
        JobController.cancel(getApplication(), jobId)
    }

    fun cancelItem(jobId: String, index: Int) {
        JobController.cancelItem(getApplication(), jobId, index)
    }

    fun dismiss(jobId: String) = JobController.remove(getApplication(), jobId)

    fun deleteHistory(id: String) = HistoryRepository.remove(id)

    private fun ensureService() {
        if (JobController.jobs.value.none { it.isActive }) return
        val app = getApplication<Application>()
        ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
    }
}
