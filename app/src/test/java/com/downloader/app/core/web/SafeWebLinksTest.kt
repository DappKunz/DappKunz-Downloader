package com.downloader.app.core.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeWebLinksTest {

    private fun inApp(url: String): String {
        val decision = SafeWebLinks.decide(url)
        assertTrue("harus dimuat di aplikasi: $url -> $decision", decision is SafeWebLinks.Decision.InApp)
        return (decision as SafeWebLinks.Decision.InApp).url
    }

    private fun external(url: String, expectedScheme: String? = null) {
        val decision = SafeWebLinks.decide(url)
        assertTrue("harus keluar aplikasi: $url -> $decision", decision is SafeWebLinks.Decision.External)
        decision as SafeWebLinks.Decision.External
        assertEquals(url, decision.url)
        if (expectedScheme != null) assertEquals(expectedScheme, decision.scheme)
    }

    private fun blocked(url: String?, expected: SafeWebLinks.Reason) {
        val decision = SafeWebLinks.decide(url)
        assertTrue("harus ditolak: $url -> $decision", decision is SafeWebLinks.Decision.Blocked)
        assertEquals(expected, (decision as SafeWebLinks.Decision.Blocked).reason)
    }

    @Test
    fun donationUrlIsLoadedInApp() {
        assertEquals(SafeWebLinks.DONATION_URL, inApp(SafeWebLinks.DONATION_URL))
    }

    @Test
    fun saweriaSubdomainIsLoadedInApp() {
        inApp("https://www.saweria.co/DapPKunzz")
        inApp("https://cdn.saweria.co/assets/app.js")
    }

    @Test
    fun paymentGatewayIsLoadedInApp() {
        // Gateway pembayaran Saweria: tanpa ini alur pembayaran tidak selesai di dalam aplikasi.
        inApp("https://app.midtrans.com/snap/v2/vtweb/abc")
        inApp("https://snap.midtrans.com/")
    }

    @Test
    fun hostComparisonIgnoresCasePortAndTrailingDot() {
        inApp("HTTPS://SAWERIA.CO/Donate")
        inApp("https://saweria.co:8443/x")
        inApp("https://saweria.co./x")
    }

    @Test
    fun httpIsAlwaysBlocked() {
        blocked("http://saweria.co/DapPKunzz", SafeWebLinks.Reason.NOT_HTTPS)
        blocked("http://midtrans.com/", SafeWebLinks.Reason.NOT_HTTPS)
    }

    @Test
    fun lookalikeDomainsAreNotTrusted() {
        external("https://saweria.co.evil.com/DapPKunzz")
        external("https://evil-saweria.co/")
        external("https://saveria.co/")
        external("https://xn--sawera-6za.co/")
        external("https://saweria.co.id/")
    }

    @Test
    fun userInfoTrickResolvesToTheRealHost() {
        // "https://saweria.co@evil.com" sebenarnya membuka evil.com.
        external("https://saweria.co@evil.com/")
        inApp("https://user:pass@saweria.co/DapPKunzz")
    }

    @Test
    fun percentEncodedHostIsRejected() {
        blocked("https://saweria%2Eco/", SafeWebLinks.Reason.MALFORMED)
    }

    @Test
    fun unsafeSchemesAreAlwaysBlocked() {
        blocked("file:///etc/passwd", SafeWebLinks.Reason.UNSAFE_SCHEME)
        blocked("javascript:alert(1)", SafeWebLinks.Reason.UNSAFE_SCHEME)
        blocked("data:text/html;base64,PHNjcmlwdD4=", SafeWebLinks.Reason.UNSAFE_SCHEME)
        blocked("content://com.android.providers.media.documents/document/1", SafeWebLinks.Reason.UNSAFE_SCHEME)
        blocked("blob:https://saweria.co/1234", SafeWebLinks.Reason.UNSAFE_SCHEME)
        blocked("view-source:https://saweria.co/", SafeWebLinks.Reason.UNSAFE_SCHEME)
    }

    @Test
    fun blankDocumentIsAllowedButOtherAboutPagesAreNot() {
        inApp("about:blank")
        blocked("about:config", SafeWebLinks.Reason.UNSAFE_SCHEME)
    }

    @Test
    fun appSchemesNeedUserConfirmation() {
        external("gojek://gopay/merchanttransfer?tref=x", "gojek")
        external("ovo://pay?x=1", "ovo")
        external("dana://payment?x=1", "dana")
        external("tel:+628123456789", "tel")
        external("mailto:halo@saweria.co", "mailto")
        external(
            "intent://pay/#Intent;scheme=gojek;package=com.gojek.app;end",
            "intent",
        )
    }

    @Test
    fun localNetworkIsBlocked() {
        blocked("https://localhost:8080/admin", SafeWebLinks.Reason.LOOPBACK)
        blocked("https://127.0.0.1/", SafeWebLinks.Reason.LOOPBACK)
        blocked("https://[::1]/", SafeWebLinks.Reason.LOOPBACK)
    }

    @Test
    fun malformedInputIsBlocked() {
        blocked(null, SafeWebLinks.Reason.MALFORMED)
        blocked("", SafeWebLinks.Reason.MALFORMED)
        blocked("   ", SafeWebLinks.Reason.MALFORMED)
        blocked("saweria.co/DapPKunzz", SafeWebLinks.Reason.MALFORMED)
        blocked("https://", SafeWebLinks.Reason.MALFORMED)
    }

    @Test
    fun isTrustedMatchesDecision() {
        assertTrue(SafeWebLinks.isTrusted(SafeWebLinks.DONATION_URL))
        assertTrue(SafeWebLinks.isTrusted("https://snap.midtrans.com/x"))
        assertFalse(SafeWebLinks.isTrusted("https://example.com/"))
        assertFalse(SafeWebLinks.isTrusted("gojek://pay"))
        assertFalse(SafeWebLinks.isTrusted("javascript:alert(1)"))
        assertFalse(SafeWebLinks.isTrusted(null))
    }

    @Test
    fun unsafeSchemeHelperOnlyFlagsDangerousSchemes() {
        assertTrue(SafeWebLinks.isUnsafeScheme("file"))
        assertTrue(SafeWebLinks.isUnsafeScheme("JavaScript"))
        assertTrue(SafeWebLinks.isUnsafeScheme(" content "))
        assertFalse(SafeWebLinks.isUnsafeScheme("gojek"))
        assertFalse(SafeWebLinks.isUnsafeScheme("https"))
        assertFalse(SafeWebLinks.isUnsafeScheme(null))
    }
}
