package com.downloader.app.core.util

object SizeEstimator {

    fun audioBytes(bitrateKbps: Int, durationSeconds: Int?): Long? {
        val duration = durationSeconds?.takeIf { it > 0 } ?: return null
        val safeBitrate = bitrateKbps.coerceIn(8, 512)
        return duration.toLong() * safeBitrate * 1000L / 8L
    }

    fun videoBytes(resolution: String, hd: Boolean, durationSeconds: Int?): Long? {
        val duration = durationSeconds?.takeIf { it > 0 } ?: return null
        val height = resolution.toIntOrNull() ?: if (hd) 1080 else 720
        val videoBps = when {
            height <= 360 -> 700_000L
            height <= 480 -> 1_300_000L
            height <= 720 -> 2_600_000L
            height <= 1080 -> 4_500_000L
            else -> 8_000_000L
        }
        return duration * (videoBps + 128_000L) / 8L
    }

    fun estimateBytes(
        format: String,
        bitrate: Int,
        resolution: String,
        hd: Boolean,
        durationSeconds: Int?,
    ): Long? = when (format) {
        "mp3", "m4a" -> audioBytes(bitrate, durationSeconds)
        "mp4" -> videoBytes(resolution, hd, durationSeconds)
        else -> null
    }
}
