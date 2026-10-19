package com.downloader.app.core

import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.util.Redact

class AppException(
    val code: String,
    override val message: String,
    val details: Map<String, String> = emptyMap(),
) : Exception(message)

object ErrorMapper {

    private val ansiRegex = Regex("\u001B\\[[0-9;]*[A-Za-z]|\u001B\\([A-Z0-9]")
    private val errorPrefixRegex = Regex("^(ERROR:\\s*)+", RegexOption.IGNORE_CASE)

    fun friendly(raw: String?, maxLen: Int = 300): String {
        var text = (raw ?: "").trim()
        text = ansiRegex.replace(text, "")
        text = errorPrefixRegex.replace(text, "").trim()
        val lowered = text.lowercase()

        if (text.isEmpty()) return DownloaderApp.str(R.string.err_unknown)

        return when {
            "http error 403" in lowered || "forbidden" in lowered ->
                DownloaderApp.str(R.string.err_403)

            "http error 404" in lowered || "not found" in lowered ->
                DownloaderApp.str(R.string.err_404)

            "http error 429" in lowered || "too many requests" in lowered ->
                DownloaderApp.str(R.string.err_429)

            "video is unavailable" in lowered ||
                "this video is no longer available" in lowered ||
                "removed by the uploader" in lowered ||
                "private video" in lowered ->
                DownloaderApp.str(R.string.err_unavailable)

            "age" in lowered && ("confirm" in lowered || "restricted" in lowered || "sign in" in lowered) ->
                DownloaderApp.str(R.string.err_age_restricted)

            "region" in lowered || "country" in lowered || "geo" in lowered || "blocked" in lowered ->
                DownloaderApp.str(R.string.err_region)

            "sign in" in lowered || "login required" in lowered || "log in" in lowered ->
                DownloaderApp.str(R.string.err_signin)

            "unsupported url" in lowered ->
                DownloaderApp.str(R.string.err_unsupported_url)

            "name or service not known" in lowered ||
                "failed to resolve" in lowered ||
                "unable to resolve host" in lowered ||
                "no address associated" in lowered ->
                DownloaderApp.str(R.string.err_dns)

            "connection timed out" in lowered || "timed out" in lowered ->
                DownloaderApp.str(R.string.err_timeout)

            else -> {
                val safe = Redact.text(text)
                if (safe.length > maxLen) safe.take(maxLen).trimEnd() + "..." else safe
            }
        }
    }
}
