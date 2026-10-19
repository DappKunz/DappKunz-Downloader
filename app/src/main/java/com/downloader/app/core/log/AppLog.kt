package com.downloader.app.core.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AppLog {

    data class Entry(
        val timeMs: Long,
        val tag: String,
        val message: String,
    ) {
        val timeText: String
            get() = TIME_FORMAT.format(Date(timeMs))
    }

    private const val MAX_ENTRIES = 300

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun d(tag: String, message: String) {
        runCatching { Log.d("DlApp/$tag", message) }
        _entries.update { listOf(Entry(System.currentTimeMillis(), tag, message)) + it.take(MAX_ENTRIES - 1) }
    }

    fun e(tag: String, message: String) {
        runCatching { Log.e("DlApp/$tag", message) }
        _entries.update { listOf(Entry(System.currentTimeMillis(), tag, message)) + it.take(MAX_ENTRIES - 1) }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
}
