package com.downloader.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetSelectorTest {

    private fun asset(name: String) = GithubAsset(
        name = name,
        browserDownloadUrl = "https://example.com/$name",
        size = 1,
    )

    @Test
    fun picksAssetMatchingDeviceAbiOrder() {
        val assets = listOf(asset("app-armeabi-v7a.apk"), asset("app-arm64-v8a.apk"))
        val picked = AssetSelector.select(assets, listOf("arm64-v8a", "armeabi-v7a"))
        assertEquals("app-arm64-v8a.apk", picked?.name)
    }

    @Test
    fun fallsBackToUniversal() {
        val assets = listOf(asset("app-x86_64.apk"), asset("app-universal.apk"))
        val picked = AssetSelector.select(assets, listOf("arm64-v8a"))
        assertEquals("app-universal.apk", picked?.name)
    }

    @Test
    fun acceptsSingleAssetWithoutAbiToken() {
        val picked = AssetSelector.select(listOf(asset("DappKunzXD-1.1.0.apk")), listOf("arm64-v8a"))
        assertEquals("DappKunzXD-1.1.0.apk", picked?.name)
    }

    @Test
    fun rejectsSingleAssetForAnotherAbi() {
        val picked = AssetSelector.select(listOf(asset("app-x86_64.apk")), listOf("arm64-v8a"))
        assertNull(picked)
    }

    @Test
    fun matchesAbiTokensCaseInsensitively() {
        val picked = AssetSelector.select(listOf(asset("App-ARM64-V8A.apk")), listOf("arm64-v8a"))
        assertEquals("App-ARM64-V8A.apk", picked?.name)
    }

    @Test
    fun ignoresNonApkAssets() {
        val picked = AssetSelector.select(
            listOf(asset("source.zip"), asset("checksums.txt")),
            listOf("arm64-v8a"),
        )
        assertNull(picked)
    }
}
