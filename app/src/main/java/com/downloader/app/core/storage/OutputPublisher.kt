package com.downloader.app.core.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.AppException
import com.downloader.app.core.util.FileNames
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class PublishResult(
    val uri: String,
    val location: String,
)

object OutputPublisher {

    const val PUBLIC_FOLDER = "Downloader"

    private const val COPY_BUFFER = 512 * 1024

    suspend fun publish(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
        subFolder: String? = null,
    ): PublishResult = withContext(Dispatchers.IO) {
        if (!source.exists()) {
            throw AppException("INTERNAL_ERROR", DownloaderApp.str(R.string.err_file_missing))
        }
        val safeName = FileNames.sanitizeFilename(displayName)
        val safeSub = subFolder?.takeIf { it.isNotBlank() }?.let { FileNames.sanitizeFilename(it) }

        val treeUri = PrefsRepository.folderUriValue()
        if (treeUri != null) {
            try {
                return@withContext publishToTree(context, Uri.parse(treeUri), source, safeName, mimeType, safeSub)
            } catch (e: SecurityException) {
                PrefsRepository.setFolderUri(context, null)
            } catch (e: Exception) {
                if (e is AppException) throw e
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishViaMediaStore(context, source, safeName, mimeType, safeSub)
        } else {
            publishLegacy(context, source, safeName, safeSub)
        }
    }

    private fun publishToTree(
        context: Context,
        treeUri: Uri,
        source: File,
        displayName: String,
        mimeType: String,
        subFolder: String?,
    ): PublishResult {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw SecurityException("tree uri tidak dapat dibuka")
        if (!tree.exists() || !tree.canWrite()) {
            throw SecurityException("folder pilihan tidak dapat ditulis")
        }
        val folder = if (subFolder != null) {
            tree.findFile(subFolder)?.takeIf { it.isDirectory }
                ?: tree.createDirectory(subFolder)
                ?: throw SecurityException("subfolder tidak dapat dibuat")
        } else {
            tree
        }

        folder.findFile(displayName)?.let { existing -> runCatching { existing.delete() } }

        val document = folder.createFile(mimeType, displayName)
            ?: throw AppException(
                "STORAGE_FULL",
                DownloaderApp.str(R.string.err_storage_failed, DownloaderApp.str(R.string.settings_folder_no_write)),
            )

        try {
            context.contentResolver.openOutputStream(document.uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output, COPY_BUFFER) }
            } ?: throw AppException(
                "STORAGE_FULL",
                DownloaderApp.str(R.string.err_storage_failed, DownloaderApp.str(R.string.settings_folder_no_write)),
            )
        } catch (e: Exception) {
            runCatching { document.delete() }
            if (e is AppException) throw e
            throw SecurityException("penulisan ke folder pilihan gagal: ${e.message}")
        }

        return PublishResult(
            uri = document.uri.toString(),
            location = describeTreeLocation(context, treeUri, subFolder),
        )
    }

    fun describeTreeLocation(context: Context, treeUri: Uri, subFolder: String? = null): String {
        val base = try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            docId.substringAfter(':').takeIf { it.isNotBlank() } ?: "Folder pilihan"
        } catch (_: Exception) {
            DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Folder pilihan"
        }
        return if (subFolder != null) "$base/$subFolder" else base
    }

    private fun publishViaMediaStore(
        context: Context,
        source: File,
        displayName: String,
        mimeType: String,
        subFolder: String?,
    ): PublishResult {
        val relativePath = buildString {
            append(Environment.DIRECTORY_DOWNLOADS)
            append('/').append(PUBLIC_FOLDER)
            if (subFolder != null) append('/').append(subFolder)
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: throw AppException("STORAGE_FULL", DownloaderApp.str(R.string.err_storage_failed, "MediaStore"))

        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output, COPY_BUFFER) }
            } ?: throw AppException("STORAGE_FULL", DownloaderApp.str(R.string.err_storage_failed, "MediaStore"))
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            if (e is AppException) throw e
            throw AppException(
                "STORAGE_FULL",
                DownloaderApp.str(R.string.err_storage_failed, e.message ?: "storage"),
            )
        }

        val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
        resolver.update(uri, done, null, null)
        return PublishResult(
            uri = uri.toString(),
            location = "Download/$PUBLIC_FOLDER" + (subFolder?.let { "/$it" } ?: ""),
        )
    }

    private suspend fun publishLegacy(
        context: Context,
        source: File,
        displayName: String,
        subFolder: String?,
    ): PublishResult {
        val base = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), PUBLIC_FOLDER)
        val targetDir = if (subFolder != null) File(base, subFolder) else base
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw AppException(
                "STORAGE_FULL",
                DownloaderApp.str(R.string.err_storage_failed, "Download/$PUBLIC_FOLDER"),
            )
        }
        val target = FileNames.uniqueFile(targetDir, displayName)
        try {
            source.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output, COPY_BUFFER) }
            }
        } catch (e: Exception) {
            throw AppException("STORAGE_FULL", DownloaderApp.str(R.string.err_storage_failed, e.message ?: "storage"))
        }
        val scanned = scanFile(context, target)
        return PublishResult(
            uri = scanned ?: Uri.fromFile(target).toString(),
            location = "Download/$PUBLIC_FOLDER" + (subFolder?.let { "/$it" } ?: ""),
        )
    }

    private suspend fun scanFile(context: Context, file: File): String? =
        suspendCancellableCoroutine { cont ->
            try {
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null) { _, uri ->
                    if (cont.isActive) cont.resume(uri?.toString() ?: Uri.fromFile(file).toString())
                }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(Uri.fromFile(file).toString())
            }
        }
}
