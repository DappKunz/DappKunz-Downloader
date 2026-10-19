package com.downloader.app.core.update

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateCheckerTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun returnsReleaseWithSelectedAsset() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(RELEASE_JSON))
        val result = UpdateChecker.fetchLatest(
            apiUrl = server.url("/latest").toString(),
            client = UpdateChecker.defaultClient,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertTrue(result is UpdateChecker.Result.Release)
        val release = (result as UpdateChecker.Result.Release).release
        assertEquals("v1.2.0", release.tagName)
        assertEquals("Notes", release.body)
        assertEquals("DappKunzXD-1.2.0-arm64-v8a.apk", result.asset?.name)
    }

    @Test
    fun returnsReleaseWithNullAssetWhenNoAbiMatches() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(RELEASE_JSON))
        val result = UpdateChecker.fetchLatest(
            apiUrl = server.url("/latest").toString(),
            client = UpdateChecker.defaultClient,
            deviceAbis = listOf("x86"),
        )
        assertTrue(result is UpdateChecker.Result.Release)
        assertEquals(null, (result as UpdateChecker.Result.Release).asset)
    }

    @Test
    fun maps404ToNoRelease() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val result = UpdateChecker.fetchLatest(
            apiUrl = server.url("/latest").toString(),
            client = UpdateChecker.defaultClient,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertEquals(UpdateChecker.Result.NoRelease, result)
    }

    @Test
    fun maps403ToRateLimited() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        val result = UpdateChecker.fetchLatest(
            apiUrl = server.url("/latest").toString(),
            client = UpdateChecker.defaultClient,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertEquals(
            UpdateChecker.Result.Failure(UpdateChecker.Result.Reason.RATE_LIMITED),
            result,
        )
    }

    @Test
    fun mapsServerError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = UpdateChecker.fetchLatest(
            apiUrl = server.url("/latest").toString(),
            client = UpdateChecker.defaultClient,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertEquals(
            UpdateChecker.Result.Failure(UpdateChecker.Result.Reason.SERVER),
            result,
        )
    }

    @Test
    fun mapsMalformedBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>error</html>"))
        val result = UpdateChecker.fetchLatest(
            apiUrl = server.url("/latest").toString(),
            client = UpdateChecker.defaultClient,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertEquals(
            UpdateChecker.Result.Failure(UpdateChecker.Result.Reason.MALFORMED),
            result,
        )
    }

    @Test
    fun mapsNetworkFailure() = runTest {
        val url = server.url("/latest").toString()
        server.shutdown()
        val result = UpdateChecker.fetchLatest(
            apiUrl = url,
            client = UpdateChecker.defaultClient,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertEquals(
            UpdateChecker.Result.Failure(UpdateChecker.Result.Reason.NETWORK),
            result,
        )
    }

    private companion object {
        val RELEASE_JSON = """
            {
              "tag_name": "v1.2.0",
              "name": "1.2.0",
              "body": "Notes",
              "html_url": "https://github.com/xdntenderman/DappKunzXD-Downloader/releases/tag/v1.2.0",
              "published_at": "2026-10-01T10:00:00Z",
              "prerelease": false,
              "draft": false,
              "assets": [
                {
                  "name": "DappKunzXD-1.2.0-arm64-v8a.apk",
                  "browser_download_url": "https://example.com/arm64.apk",
                  "size": 123
                },
                {
                  "name": "DappKunzXD-1.2.0-armeabi-v7a.apk",
                  "browser_download_url": "https://example.com/v7a.apk",
                  "size": 100
                },
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://example.com/checksums.txt",
                  "size": 10
                }
              ]
            }
        """.trimIndent()
    }
}
