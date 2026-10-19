package com.downloader.app.core.update

object UpdateStatusResolver {

    enum class Comparison { NEWER, UP_TO_DATE, UNCOMPARABLE }

    fun resolve(
        installedVersionCode: Long,
        installedVersionName: String?,
        release: GithubRelease,
    ): Comparison {
        val parsed = Versioning.parse(release.tagName) ?: Versioning.parse(release.name)
        ?: return Comparison.UNCOMPARABLE

        val latestCode = Versioning.toVersionCode(parsed)
        if (latestCode != null && installedVersionCode > 0) {
            return if (latestCode > installedVersionCode) Comparison.NEWER else Comparison.UP_TO_DATE
        }

        val installed = Versioning.parse(installedVersionName) ?: return Comparison.UNCOMPARABLE
        return if (Versioning.compare(parsed, installed) > 0) Comparison.NEWER
        else Comparison.UP_TO_DATE
    }
}
