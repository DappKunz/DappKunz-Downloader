package com.downloader.app.core.service

import android.content.Context
import com.downloader.app.core.log.AppLog
import com.downloader.app.core.model.DownloadRequest
import com.downloader.app.core.model.ItemState
import com.downloader.app.core.model.JobStage
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Snapshot job antrean yang disimpan ke disk agar antrean bertahan saat
 * proses aplikasi dimatikan. Job terminal selain FAILED tidak disimpan
 * (sudah tercatat di riwayat).
 */
@Serializable
data class PersistedJob(
    val jobId: String,
    val url: String,
    val title: String,
    val platform: String,
    val thumbnail: String? = null,
    val format: String,
    val qualityLabel: String,
    val stage: JobStage,
    val message: String? = null,
    val errorMessage: String? = null,
    val currentItem: Int = 0,
    val totalItems: Int = 0,
    val items: List<ItemState> = emptyList(),
    val request: DownloadRequest,
    val createdAt: Long,
)

object JobQueueStore {

    private const val FILE_NAME = "job_queue.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun shouldPersist(stage: JobStage): Boolean = !stage.isTerminal || stage == JobStage.FAILED

    fun load(context: Context): List<PersistedJob> {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<PersistedJob>>(file.readText())
        } catch (e: Exception) {
            AppLog.e("JobQueue", "Baca antrean gagal: ${e.message}")
            emptyList()
        }
    }

    fun save(context: Context, jobs: List<PersistedJob>) {
        val dir = context.applicationContext.filesDir
        try {
            val file = File(dir, FILE_NAME)
            val tmp = File(dir, "$FILE_NAME.tmp")
            tmp.writeText(json.encodeToString(jobs))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (e: Exception) {
            AppLog.e("JobQueue", "Simpan antrean gagal: ${e.message}")
        }
    }
}
