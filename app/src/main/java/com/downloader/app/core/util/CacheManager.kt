package com.downloader.app.core.util

import android.content.Context
import com.downloader.app.core.service.JobController
import java.io.File

object CacheManager {

    fun activeJobIds(): Set<String> =
        JobController.jobs.value.filter { it.isActive }.map { it.jobId }.toSet()

    fun sizeBytes(context: Context, keepJobIds: Set<String> = activeJobIds()): Long =
        collectTargets(context, keepJobIds).sumOf { fileSize(it) }

    fun clear(context: Context, keepJobIds: Set<String> = activeJobIds()): Long {
        val targets = collectTargets(context, keepJobIds)
        val before = targets.sumOf { fileSize(it) }
        targets.forEach { runCatching { it.deleteRecursively() } }
        val after = collectTargets(context, keepJobIds).sumOf { fileSize(it) }
        return (before - after).coerceAtLeast(0)
    }

    private fun collectTargets(context: Context, keepJobIds: Set<String>): List<File> {
        val result = mutableListOf<File>()
        val cacheDir = context.cacheDir
        val jobsDir = File(cacheDir, "jobs")
        val updatesDir = File(cacheDir, "updates")
        val jobsActive = keepJobIds.isNotEmpty()
        cacheDir.listFiles()?.forEach { entry ->
            when {
                entry.absolutePath == jobsDir.absolutePath -> {
                    entry.listFiles()?.forEach { jobDir ->
                        if (jobDir.name !in keepJobIds) result.add(jobDir)
                    }
                }

                entry.absolutePath == updatesDir.absolutePath -> Unit

                jobsActive -> Unit

                else -> result.add(entry)
            }
        }
        context.externalCacheDir?.let { ext -> if (ext.exists()) result.add(ext) }
        return result
    }

    private fun fileSize(file: File): Long =
        if (file.isDirectory) {
            file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            file.length()
        }
}
