package com.downloader.app.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.downloader.app.DownloaderApp
import com.downloader.app.MainActivity
import com.downloader.app.R
import com.downloader.app.core.util.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_ACTIVE) {
            JobController.cancelAll(this)
            if (JobController.jobs.value.none { it.isActive }) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        startForegroundCompat(buildNotification())

        if (!observing) {
            observing = true
            scope.launch {
                JobController.jobs.collect { jobs ->
                    if (jobs.none { it.isActive }) {
                        ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collect
                    }
                    val manager = getSystemService(NotificationManager::class.java) ?: return@collect
                    manager.notify(NOTIFICATION_ID, buildNotification())
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val active = JobController.jobs.value.filter { it.isActive }
        val single = active.size == 1
        val title: String
        val text: String
        val progress: Int
        val indeterminate: Boolean

        when {
            active.isEmpty() -> {
                title = DownloaderApp.str(R.string.notification_preparing)
                text = ""
                progress = 0
                indeterminate = true
            }

            single -> {
                val job = active.first()
                title = DownloaderApp.str(R.string.notification_title_single, job.title)
                text = describe(job)
                val percent = job.progressPercent
                progress = percent ?: 0
                indeterminate = percent == null
            }

            else -> {
                title = DownloaderApp.str(R.string.notification_title_multi, active.size)
                text = active.take(2).joinToString(" • ") { job ->
                    val percent = job.progressPercent
                    if (percent != null) "${job.title} $percent%" else job.title
                }
                val allDeterminate = active.all { it.progressPercent != null }
                progress = if (allDeterminate) active.sumOf { it.progressPercent ?: 0 } / active.size else 0
                indeterminate = !allDeterminate
            }
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL_ACTIVE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, DownloaderApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.coerceIn(0, 100), indeterminate)
            .addAction(0, DownloaderApp.str(R.string.notification_action_cancel), cancelIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun describe(job: JobController.UiState): String {
        val parts = mutableListOf<String>()
        val percent = job.progressPercent
        val speed = job.speedBps.takeIf { it > 0 }?.let { Formatters.speed(it) }
        when {
            percent != null && speed != null ->
                parts.add(DownloaderApp.str(R.string.notification_progress_text, percent, speed))

            percent != null ->
                parts.add(DownloaderApp.str(R.string.notification_progress_text_nospeed, percent))

            job.message != null -> parts.add(job.message)
            else -> parts.add(DownloaderApp.str(R.string.notification_preparing))
        }
        if (job.totalItems > 1 && job.currentItem > 0) {
            parts.add(DownloaderApp.str(R.string.job_item_progress, job.currentItem, job.totalItems))
        }
        return parts.joinToString(" • ")
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_CANCEL_ACTIVE = "com.downloader.app.action.CANCEL_ACTIVE"
        private const val NOTIFICATION_ID = 42
    }
}
