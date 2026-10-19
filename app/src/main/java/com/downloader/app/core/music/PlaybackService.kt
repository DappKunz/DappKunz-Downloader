package com.downloader.app.core.music

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.downloader.app.MainActivity
import com.downloader.app.core.log.AppLog

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val player = MusicController.ensure(this)
            val activityIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val created = MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntent)
                .build()
            session = created
            // Wajib: tanpa addSession(), MediaNotificationManager Media3 tidak mengenal
            // session ini -> tidak ada notifikasi media dan tidak ada startForeground().
            // Dulu efeknya: ForegroundServiceDidNotStartInTimeException (force close).
            addSession(created)
        } catch (t: Throwable) {
            AppLog.e("PlaybackService", "Gagal menyiapkan sesi media: ${t.message}")
            session = null
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val playing = runCatching { MusicController.playerOrNull()?.isPlaying == true }.getOrDefault(false)
        if (!playing) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching { session?.release() }
        session = null
        super.onDestroy()
    }
}
