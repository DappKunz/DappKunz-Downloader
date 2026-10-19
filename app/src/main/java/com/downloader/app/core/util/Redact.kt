package com.downloader.app.core.util

/**
 * Penyaring data sensitif untuk log. URL bertanda tangan (token/expiry di
 * query string) tidak boleh ditulis utuh ke log aplikasi maupun Logcat.
 */
object Redact {

    private val httpUrlRegex = Regex("""https?://\S+""")

    fun url(value: String): String =
        value.trim().substringBefore('?').substringBefore('#')

    fun text(value: String): String =
        httpUrlRegex.replace(value) { match -> url(match.value) }
}
