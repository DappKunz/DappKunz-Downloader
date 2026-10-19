package com.downloader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FfmpegProgressTest {

    @Test
    fun parsesDurationFromBanner() {
        assertEquals(
            19_020_000L,
            FfmpegProgress.parseDurationUs("  Duration: 00:00:19.02, start: 0.000000, bitrate: 128 kb/s"),
        )
        assertEquals(
            3_723_456_000L,
            FfmpegProgress.parseDurationUs("Duration: 01:02:03.456, start: 0.0"),
        )
    }

    @Test
    fun rejectsLinesWithoutDuration() {
        assertNull(FfmpegProgress.parseDurationUs("frame= 100 fps=50 q=28.0 size= 512kB"))
        assertNull(FfmpegProgress.parseDurationUs("out_time_us=1000000"))
        assertNull(FfmpegProgress.parseDurationUs("Duration: N/A, start: 0.0"))
    }

    @Test
    fun parsesOutTimeMicroseconds() {
        assertEquals(4_500_000L, FfmpegProgress.parseOutTimeUs("out_time_us=4500000"))
        assertEquals(4_500_000L, FfmpegProgress.parseOutTimeUs("out_time_ms=4500000"))
    }

    @Test
    fun rejectsInvalidOutTime() {
        assertNull(FfmpegProgress.parseOutTimeUs("out_time_us=N/A"))
        assertNull(FfmpegProgress.parseOutTimeUs("progress=continue"))
        assertNull(FfmpegProgress.parseOutTimeUs("out_time_us=-5"))
    }
}
