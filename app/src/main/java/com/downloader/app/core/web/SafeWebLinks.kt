package com.downloader.app.core.web

import java.util.Locale

/**
 * Kebijakan navigasi untuk halaman web yang dimuat di dalam aplikasi (layar Donasi).
 *
 * Objek ini sengaja murni (tanpa Android/WebView) supaya aturannya bisa diuji di JVM.
 * WebView hanya memakai [decide] untuk navigasi frame utama dan [isTrusted] untuk subframe:
 *
 * - [Decision.InApp]    -> boleh dimuat di WebView.
 * - [Decision.External] -> jangan dimuat diam-diam; UI wajib bertanya dulu ke pengguna,
 *                          lalu membukanya lewat peramban/aplikasi lain.
 * - [Decision.Blocked]  -> ditolak; UI hanya menampilkan alasan singkat.
 */
object SafeWebLinks {

    /** Halaman donasi resmi pengembang. */
    const val DONATION_URL = "https://saweria.co/DapPKunzz"

    /**
     * Domain yang boleh dimuat di dalam aplikasi.
     *
     * "midtrans.com" adalah gateway pembayaran yang dipakai Saweria; tanpa domain ini
     * alur pembayaran tidak bisa selesai di dalam aplikasi. Domain lain (bank, e-wallet,
     * dsb.) tidak didaftarkan di sini dengan sengaja: tautan seperti itu dikembalikan
     * sebagai [Decision.External] sehingga pengguna memutuskan sendiri.
     */
    private val TRUSTED_HOSTS = listOf(
        "saweria.co",
        "midtrans.com",
    )

    /** Skema yang tidak pernah boleh dibuka, baik di WebView maupun lewat Intent. */
    private val UNSAFE_SCHEMES = setOf(
        "file",
        "javascript",
        "data",
        "content",
        "blob",
        "view-source",
        "chrome",
        "jar",
    )

    private val LOOPBACK_HOSTS = setOf(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1",
    )

    enum class Reason {
        /** URL kosong atau tidak bisa dibaca. */
        MALFORMED,

        /** Skema berbahaya (file, javascript, data, content, ...). */
        UNSAFE_SCHEME,

        /** Bukan HTTPS. */
        NOT_HTTPS,

        /** Menunjuk ke perangkat/jaringan lokal. */
        LOOPBACK,
    }

    sealed interface Decision {
        data class InApp(val url: String) : Decision

        data class External(val url: String, val scheme: String) : Decision

        data class Blocked(val reason: Reason) : Decision
    }

    fun decide(rawUrl: String?): Decision {
        val url = rawUrl?.trim().orEmpty()
        if (url.isEmpty()) return Decision.Blocked(Reason.MALFORMED)

        val parsed = parse(url) ?: return Decision.Blocked(Reason.MALFORMED)
        val scheme = parsed.scheme

        // Halaman kosong dipakai sebagian skrip untuk membersihkan dokumen; tidak berisiko.
        if (scheme == "about") {
            return if (parsed.host == "blank" || url.equals("about:blank", ignoreCase = true)) {
                Decision.InApp(url)
            } else {
                Decision.Blocked(Reason.UNSAFE_SCHEME)
            }
        }

        if (isUnsafeScheme(scheme)) return Decision.Blocked(Reason.UNSAFE_SCHEME)

        // gojek://, ovo://, dana://, tel:, mailto:, intent:, ... -> butuh aplikasi lain.
        if (scheme != "http" && scheme != "https") return Decision.External(url, scheme)

        val host = parsed.host
        if (host.isEmpty() || host.contains('%')) return Decision.Blocked(Reason.MALFORMED)
        if (isLoopbackHost(host)) return Decision.Blocked(Reason.LOOPBACK)
        if (scheme != "https") return Decision.Blocked(Reason.NOT_HTTPS)

        return if (isTrustedHost(host)) {
            Decision.InApp(url)
        } else {
            Decision.External(url, scheme)
        }
    }

    /** Apakah URL ini boleh dimuat di dalam aplikasi (tanpa bertanya ke pengguna). */
    fun isTrusted(url: String?): Boolean = decide(url) is Decision.InApp

    /** Apakah skema termasuk yang berbahaya dan tidak boleh dibuka sama sekali. */
    fun isUnsafeScheme(scheme: String?): Boolean {
        if (scheme.isNullOrBlank()) return false
        return scheme.trim().lowercase(Locale.ROOT) in UNSAFE_SCHEMES
    }

    /** Cocokkan host dengan daftar domain tepercaya, termasuk subdomainnya. */
    fun isTrustedHost(host: String): Boolean {
        val clean = host.trim().lowercase(Locale.ROOT).trimEnd('.')
        if (clean.isEmpty()) return false
        return TRUSTED_HOSTS.any { clean == it || clean.endsWith(".$it") }
    }

    fun isLoopbackHost(host: String): Boolean {
        val clean = host.trim().lowercase(Locale.ROOT).trimEnd('.')
        if (clean.isEmpty()) return false
        return clean in LOOPBACK_HOSTS || clean.startsWith("127.")
    }

    private class Parsed(val scheme: String, val host: String)

    /**
     * Penguraian seadanya: hanya butuh skema dan host.
     *
     * Ditulis manual supaya tidak bergantung pada kelonggaran/kekakuan parser lain, dan
     * supaya trik seperti `https://saweria.co@evil.com` atau `https://saweria.co.evil.com`
     * tidak pernah lolos sebagai domain Saweria.
     */
    private fun parse(url: String): Parsed? {
        val colon = url.indexOf(':')
        if (colon <= 0) return null

        val scheme = url.substring(0, colon).lowercase(Locale.ROOT)
        val validScheme = scheme.all { ch ->
            ch in 'a'..'z' || ch in '0'..'9' || ch == '+' || ch == '-' || ch == '.'
        }
        if (!validScheme) return null

        val rest = url.substring(colon + 1)
        if (scheme == "about") return Parsed(scheme, rest.trim().lowercase(Locale.ROOT))
        if (!rest.startsWith("//")) return Parsed(scheme, "")

        val authority = rest.substring(2).takeWhile { it != '/' && it != '?' && it != '#' }
        // Buang bagian userinfo: "saweria.co@evil.com" sebenarnya menuju evil.com.
        val hostPart = authority.substringAfterLast('@')

        if (hostPart.startsWith("[")) {
            val close = hostPart.indexOf(']')
            val host = if (close > 0) hostPart.substring(1, close) else hostPart
            return Parsed(scheme, normalize(host))
        }

        // Buang port bila ada.
        return Parsed(scheme, normalize(hostPart.takeWhile { it != ':' }))
    }

    private fun normalize(host: String): String = host.lowercase(Locale.ROOT).trimEnd('.')
}
