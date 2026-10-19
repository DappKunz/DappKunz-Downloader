package com.downloader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageGuardTest {

    private val mb = 1024L * 1024L

    @Test
    fun usesMinimumWhenEstimateIsUnknown() {
        assertEquals(96 * mb, StorageGuard.requiredBytes(null))
        assertEquals(96 * mb, StorageGuard.requiredBytes(0))
        assertEquals(96 * mb, StorageGuard.requiredBytes(-5))
    }

    @Test
    fun scalesWithEstimatePlusMargin() {
        assertEquals(264 * mb, StorageGuard.requiredBytes(100 * mb))
        assertEquals(2_064 * mb, StorageGuard.requiredBytes(1_000 * mb))
    }

    @Test
    fun clampsOverflow() {
        assertEquals(Long.MAX_VALUE, StorageGuard.requiredBytes(Long.MAX_VALUE))
    }
}
