package com.downloader.app.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0,
)

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)
