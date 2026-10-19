package com.downloader.app.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetsTest {

    private val typicalBands = listOf(60, 230, 910, 3600, 14000)

    @Test
    fun normal_isFlatZero() {
        assertEquals(listOf(0, 0, 0, 0, 0), EqPresets.gainsDb(EqPresets.NORMAL, typicalBands))
    }

    @Test
    fun bassBoost_boostsLows_notHighs() {
        val gains = EqPresets.gainsDb(EqPresets.BASS, typicalBands)
        assertTrue("bass band should be boosted, was ${gains[0]}", gains[0] >= 6)
        assertEquals(0, gains[3])
        assertEquals(0, gains[4])
        assertTrue("gain should fall from low to mid", gains[0] >= gains[1] && gains[1] >= gains[2])
    }

    @Test
    fun trebleBoost_boostsHighs_notLows() {
        val gains = EqPresets.gainsDb(EqPresets.TREBLE, typicalBands)
        assertEquals(0, gains[0])
        assertTrue("treble band should be boosted, was ${gains[4]}", gains[4] >= 6)
    }

    @Test
    fun rock_hasMidDip() {
        val gains = EqPresets.gainsDb(EqPresets.ROCK, typicalBands)
        assertTrue("lows boosted", gains[0] > 0)
        assertTrue("highs boosted", gains[4] > 0)
        assertTrue("mids dipped, was ${gains[2]}", gains[2] < 0)
    }

    @Test
    fun vocal_boostsPresence() {
        val gains = EqPresets.gainsDb(EqPresets.VOCAL, typicalBands)
        assertTrue("presence boosted, was ${gains[3]}", gains[3] > 0)
        assertTrue("sub-bass cut, was ${gains[0]}", gains[0] < 0)
    }

    @Test
    fun worksWithAnyBandCount() {
        val three = EqPresets.gainsDb(EqPresets.BASS, listOf(100, 1000, 10000))
        assertEquals(3, three.size)
        assertTrue(three[0] > three[2])
        val ten = EqPresets.gainsDb(EqPresets.CLASSICAL, List(10) { 60 * (it + 1) })
        assertEquals(10, ten.size)
    }

    @Test
    fun unknownPreset_fallsBackToNormal() {
        assertEquals(listOf(0, 0), EqPresets.gainsDb("Tidak Ada", listOf(100, 1000)))
    }

    @Test
    fun clampDb_respectsDeviceRange() {
        assertEquals(12, EqPresets.clampDb(20, -12, 12))
        assertEquals(-12, EqPresets.clampDb(-20, -12, 12))
        assertEquals(3, EqPresets.clampDb(3, -12, 12))
    }

    @Test
    fun millibelConversion_roundTrips() {
        assertEquals(600, EqPresets.dbToMillibel(6))
        assertEquals(-6, EqPresets.millibelToDb(-600))
        assertEquals(0, EqPresets.millibelToDb(0))
    }
}
