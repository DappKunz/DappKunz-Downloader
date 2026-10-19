package com.downloader.app.core.update

/**
 * Aturan pemilihan aset APK (dokumentasikan di laporan rilis):
 * 1. Hanya aset berakhiran `.apk` dengan URL unduhan.
 * 2. Cocokkan token ABI perangkat secara berurutan (arm64-v8a, armeabi-v7a, x86_64).
 * 3. Jika tidak ada, pakai aset bertoken `universal`.
 * 4. Jika hanya ada satu aset dan namanya tidak menyebut ABI spesifik,
 *    anggap universal; aset bertoken ABI lain tidak dipakai.
 */
object AssetSelector {

    private val ABI_TOKENS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86", "universal")

    fun select(assets: List<GithubAsset>, deviceAbis: List<String>): GithubAsset? {
        val apks = assets.filter {
            it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank()
        }
        if (apks.isEmpty()) return null

        for (abi in deviceAbis) {
            apks.firstOrNull { it.name.contains(abi, ignoreCase = true) }?.let { return it }
        }
        apks.firstOrNull { it.name.contains("universal", ignoreCase = true) }?.let { return it }

        if (apks.size == 1) {
            val name = apks.first().name.lowercase()
            if (ABI_TOKENS.none { name.contains(it) }) return apks.first()
        }
        return null
    }
}
