package com.downloader.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VersioningTest {

    @Test
    fun parsesTagsWithAndWithoutPrefix() {
        assertEquals(listOf(1, 1, 0), Versioning.parse("v1.1.0"))
        assertEquals(listOf(1, 1, 0), Versioning.parse("V1.1.0"))
        assertEquals(listOf(1, 2, 0), Versioning.parse("1.2"))
        assertEquals(listOf(2, 0, 0), Versioning.parse("v2"))
    }

    @Test
    fun rejectsInvalidVersions() {
        assertNull(Versioning.parse(null))
        assertNull(Versioning.parse(""))
        assertNull(Versioning.parse("release-1.2.3"))
        assertNull(Versioning.parse("v1.x.0"))
    }

    @Test
    fun mapsVersionsToVersionCode() {
        assertEquals(10100L, Versioning.toVersionCode(listOf(1, 1, 0)))
        assertEquals(11000L, Versioning.toVersionCode(listOf(1, 10, 0)))
        assertEquals(20001L, Versioning.toVersionCode(listOf(2, 0, 1)))
        assertNull(Versioning.toVersionCode(listOf(1, 100, 0)))
    }

    @Test
    fun comparesVersions() {
        assertEquals(1, Versioning.compare(listOf(1, 2, 0), listOf(1, 1, 5)))
        assertEquals(0, Versioning.compare(listOf(1, 2, 0), listOf(1, 2, 0)))
        assertEquals(-1, Versioning.compare(listOf(1, 0, 0), listOf(2, 0, 0)))
    }
}
