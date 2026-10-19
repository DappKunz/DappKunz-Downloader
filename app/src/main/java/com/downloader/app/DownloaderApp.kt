package com.downloader.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import com.downloader.app.core.engine.EngineManager
import com.downloader.app.core.service.JobController
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.core.util.LocaleHelper
import com.downloader.app.data.HistoryRepository

class DownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        PrefsRepository.init(this)
        EngineManager.startAsync(this)
        HistoryRepository.init(this)
        JobController.restore(this)
        createNotificationChannel()
    }

    fun recreateNotificationChannel() {
        // createNotificationChannel dengan ID sama hanya memperbarui nama/deskripsi.
        // deleteNotificationChannel DILARANG saat channel sedang dipakai foreground
        // service (SecurityException "Not allowed to delete channel ... with a foreground service").
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            str(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = str(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "downloads"

        @Volatile
        private var instance: DownloaderApp? = null

        fun ctx(): Context =
            checkNotNull(instance) { "DownloaderApp belum diinisialisasi" }.applicationContext

        fun localized(): Context = LocaleHelper.wrap(ctx())

        fun str(@StringRes resId: Int): String = localized().getString(resId)

        fun str(@StringRes resId: Int, vararg args: Any): String = localized().getString(resId, *args)

        fun refreshNotificationChannel() {
            instance?.recreateNotificationChannel()
        }
    }
}
