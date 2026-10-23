package com.downloader.app.core.update

import android.os.Build
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateChecker {

    const val RELEASE_API =
        "https://api.github.com/repos/DappKunz/DappKunz-Downloader/releases/latest"

    /** Halaman utama repository (tombol "GitHub Repository" di layar Tentang). */
    const val REPO_PAGE =
        "https://github.com/DappKunz/DappKunz-Downloader"

    const val RELEASES_PAGE =
        "https://github.com/DappKunz/DappKunz-Downloader/releases"
    const val USER_AGENT = "DappKunz-Downloader"

    sealed interface Result {
        data class Release(val release: GithubRelease, val asset: GithubAsset?) : Result
        data object NoRelease : Result
        data class Failure(val reason: Reason) : Result

        enum class Reason { NETWORK, RATE_LIMITED, SERVER, MALFORMED }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    internal val defaultClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatest(
        apiUrl: String = RELEASE_API,
        client: OkHttpClient = defaultClient,
        deviceAbis: List<String> = Build.SUPPORTED_ABIS.toList(),
    ): Result = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.github+json")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> Result.NoRelease
                    response.code == 403 || response.code == 429 ->
                        Result.Failure(Result.Reason.RATE_LIMITED)

                    !response.isSuccessful -> Result.Failure(Result.Reason.SERVER)
                    else -> parse(response.body?.string().orEmpty(), deviceAbis)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.Failure(Result.Reason.NETWORK)
        }
    }

    private fun parse(body: String, deviceAbis: List<String>): Result {
        val release = try {
            json.decodeFromString<GithubRelease>(body)
        } catch (_: Exception) {
            null
        }
        if (release == null || release.tagName.isBlank()) {
            return Result.Failure(Result.Reason.MALFORMED)
        }
        val asset = AssetSelector.select(release.assets, deviceAbis)
        return Result.Release(release, asset)
    }
}
