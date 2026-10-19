package com.downloader.app.core.util

import android.os.SystemClock

class SpeedMeter {

    private var lastBytes = 0L
    private var lastAt = 0L
    private var ema = 0.0

    @Synchronized
    fun update(totalBytes: Long): Long {
        val now = SystemClock.elapsedRealtime()
        if (lastAt == 0L) {
            lastAt = now
            lastBytes = totalBytes
            return 0L
        }
        val dt = now - lastAt
        if (dt < MIN_SAMPLE_MS) return ema.toLong()
        val delta = totalBytes - lastBytes
        val instant = if (delta > 0) delta * 1000.0 / dt else 0.0
        ema = if (ema <= 0.0) instant else ema * (1 - SMOOTHING) + instant * SMOOTHING
        lastBytes = totalBytes
        lastAt = now
        return ema.toLong()
    }

    @Synchronized
    fun decay(): Long {
        ema *= 0.6
        if (ema < 512) ema = 0.0
        return ema.toLong()
    }

    @Synchronized
    fun reset() {
        lastBytes = 0L
        lastAt = 0L
        ema = 0.0
    }

    private companion object {
        const val MIN_SAMPLE_MS = 350L
        const val SMOOTHING = 0.35
    }
}
