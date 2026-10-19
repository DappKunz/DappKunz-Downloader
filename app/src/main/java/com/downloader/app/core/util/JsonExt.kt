package com.downloader.app.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.stringOrNull(key: String): String? =
    try {
        this[key]?.jsonPrimitive?.content?.takeIf { it != "null" }
    } catch (_: Exception) {
        null
    }

internal fun JsonObject.intOrNull(key: String): Int? =
    try {
        this[key]?.jsonPrimitive?.intOrNull
    } catch (_: Exception) {
        null
    }

internal fun JsonObject.floatOrNull(key: String): Float? =
    try {
        this[key]?.jsonPrimitive?.floatOrNull
    } catch (_: Exception) {
        null
    }

internal fun lastThumbnailUrl(obj: JsonObject): String? {
    val arr = obj["thumbnails"] as? JsonArray ?: return null
    return arr.asReversed().firstNotNullOfOrNull { element ->
        (element as? JsonObject)?.stringOrNull("url")
    }
}

internal fun thumbnailCandidates(info: JsonObject): List<String> {
    val result = linkedSetOf<String>()
    info.stringOrNull("thumbnail")?.takeIf { it.startsWith("http") }?.let { result.add(it) }
    val thumbs = info["thumbnails"] as? JsonArray
    if (thumbs != null) {
        val ordered = thumbs
            .mapNotNull { it as? JsonObject }
            .sortedByDescending { it.intOrNull("width") ?: 0 }
        for (thumb in ordered) {
            thumb.stringOrNull("url")?.takeIf { it.startsWith("http") }?.let { result.add(it) }
        }
    }
    return result.toList()
}
