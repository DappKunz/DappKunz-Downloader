package com.downloader.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateStatusResolverTest {

    private fun release(tag: String, name: String? = null) = GithubRelease(
        tagName = tag,
        name = name,
        htmlUrl = "https://example.com",
    )

    @Test
    fun newerWhenReleaseCodeIsHigher() {
        val result = UpdateStatusResolver.resolve(10100, "1.1.0", release("v1.2.0"))
        assertEquals(UpdateStatusResolver.Comparison.NEWER, result)
    }

    @Test
    fun upToDateWhenVersionsMatch() {
        val result = UpdateStatusResolver.resolve(10100, "1.1.0", release("v1.1.0"))
        assertEquals(UpdateStatusResolver.Comparison.UP_TO_DATE, result)
    }

    @Test
    fun upToDateWhenInstalledIsAhead() {
        val result = UpdateStatusResolver.resolve(10100, "1.1.0", release("v1.0.0"))
        assertEquals(UpdateStatusResolver.Comparison.UP_TO_DATE, result)
    }

    @Test
    fun usesNameWhenTagIsNotAVersion() {
        val result = UpdateStatusResolver.resolve(
            10100,
            "1.1.0",
            release("release-notes", name = "v1.3.0"),
        )
        assertEquals(UpdateStatusResolver.Comparison.NEWER, result)
    }

    @Test
    fun uncomparableWhenNoVersionFound() {
        val result = UpdateStatusResolver.resolve(
            10100,
            "1.1.0",
            release("latest", name = "some build"),
        )
        assertEquals(UpdateStatusResolver.Comparison.UNCOMPARABLE, result)
    }

    @Test
    fun fallsBackToSemverWhenInstalledCodeUnknown() {
        val result = UpdateStatusResolver.resolve(0, "1.0.0", release("v1.1.0"))
        assertEquals(UpdateStatusResolver.Comparison.NEWER, result)
    }
}
