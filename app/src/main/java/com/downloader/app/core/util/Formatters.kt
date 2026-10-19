package com.downloader.app.core.util

import com.downloader.app.DownloaderApp
import com.downloader.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    fun bytes(value: Long): String {
        if (value <= 0) return "0 B"
        if (value < 1024) return "$value B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var size = value / 1024.0
        var index = 0
        while (size >= 1024 && index < units.lastIndex) {
            size /= 1024.0
            index++
        }
        return String.format(Locale.getDefault(), "%.1f %s", size, units[index])
    }

    fun speed(bytesPerSecond: Long): String = "${bytes(bytesPerSecond)}/s"

    fun duration(seconds: Int?): String = durationLong(seconds?.toLong())

    fun durationLong(seconds: Long?): String {
        if (seconds == null || seconds <= 0) return "--:--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun eta(seconds: Long?): String? {
        if (seconds == null || seconds <= 0) return null
        return durationLong(seconds)
    }

    fun dateTime(millis: Long): String {
        val locale = Locale.getDefault()
        val pattern = if (locale.language == "in") "d MMM yyyy • HH:mm" else "MMM d, yyyy • HH:mm"
        return SimpleDateFormat(pattern, locale).format(Date(millis))
    }

    fun platformLabel(platform: String): String = when (platform.lowercase(Locale.US)) {
        "youtube" -> "YouTube"
        "youtube_music" -> "YouTube Music"
        "tiktok" -> "TikTok"
        "instagram" -> "Instagram"
        "twitter", "x" -> "Twitter/X"
        "facebook" -> "Facebook"
        "vimeo" -> "Vimeo"
        "reddit" -> "Reddit"
        "pinterest" -> "Pinterest"
        "spotify" -> "Spotify"
        "apple_music" -> "Apple Music"
        "tidal" -> "Tidal"
        "deezer" -> "Deezer"
        "soundcloud" -> "SoundCloud"
        "twitch" -> "Twitch"
        "dailymotion" -> "Dailymotion"
        "snapchat" -> "Snapchat"
        "linkedin" -> "LinkedIn"
        "tumblr" -> "Tumblr"
        "unsplash" -> "Unsplash"
        "pexels" -> "Pexels"
        "pixabay" -> "Pixabay"
        "bandcamp" -> "Bandcamp"
        "bilibili" -> "Bilibili"
        else -> platform.replace('_', ' ').split(' ')
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
    }

    fun formatTag(format: String): String = when (format) {
        "mp3" -> "MP3"
        "m4a" -> "M4A"
        "mp4" -> "MP4"
        "photo" -> DownloaderApp.str(R.string.format_tag_photo).uppercase(Locale.getDefault())
        else -> format.uppercase(Locale.US)
    }

    fun qualityLabel(format: String, bitrate: Int, resolution: String, hd: Boolean): String = when (format) {
        "mp3", "m4a" -> DownloaderApp.str(R.string.quality_label_kbps, bitrate)
        "photo" -> DownloaderApp.str(R.string.quality_label_full)
        else -> when {
            resolution != "auto" -> {
                val height = resolution.toIntOrNull()
                if (height != null) {
                    DownloaderApp.str(R.string.quality_label_height, height)
                } else {
                    resolution
                }
            }
            hd -> DownloaderApp.str(R.string.quality_label_auto_hd)
            else -> DownloaderApp.str(R.string.quality_label_auto)
        }
    }
}
