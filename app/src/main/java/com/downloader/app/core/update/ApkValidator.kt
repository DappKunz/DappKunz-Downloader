package com.downloader.app.core.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Validasi APK sebelum ditawarkan ke pemasang sistem:
 * - dapat dibaca sebagai paket valid,
 * - package/application ID sama dengan aplikasi terpasang,
 * - sertifikat penandatangan kompatibel dengan aplikasi terpasang.
 */
object ApkValidator {

    sealed interface Verdict {
        data object Valid : Verdict

        data class Invalid(val reason: Reason) : Verdict

        enum class Reason { UNREADABLE, PACKAGE_MISMATCH, SIGNATURE_MISMATCH }
    }

    fun validate(context: Context, apk: File): Verdict {
        if (!apk.exists() || apk.length() <= 0) return Verdict.Invalid(Verdict.Reason.UNREADABLE)
        val pm = context.packageManager
        val flags = archiveFlags()
        val archive = try {
            pm.getPackageArchiveInfo(apk.absolutePath, flags)
        } catch (_: Exception) {
            null
        } ?: return Verdict.Invalid(Verdict.Reason.UNREADABLE)

        if (archive.packageName != context.packageName) {
            return Verdict.Invalid(Verdict.Reason.PACKAGE_MISMATCH)
        }

        val installed = try {
            pm.getPackageInfo(context.packageName, flags)
        } catch (_: PackageManager.NameNotFoundException) {
            return Verdict.Invalid(Verdict.Reason.UNREADABLE)
        }

        val archiveCerts = certificateDigests(archive)
        val installedCerts = certificateDigests(installed)
        if (archiveCerts.isEmpty() || installedCerts.isEmpty()) {
            return Verdict.Invalid(Verdict.Reason.SIGNATURE_MISMATCH)
        }
        return if (archiveCerts.any { it in installedCerts }) Verdict.Valid
        else Verdict.Invalid(Verdict.Reason.SIGNATURE_MISMATCH)
    }

    private fun archiveFlags(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

    @Suppress("DEPRECATION")
    private fun certificateDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptySet()
            val history = signing.signingCertificateHistory
            if (history != null && history.isNotEmpty()) history else signing.apkContentsSigners
        } else {
            info.signatures
        } ?: return emptySet()

        return signatures.mapNotNull { signature ->
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                digest.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }
}
