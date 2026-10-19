package com.downloader.app.core.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Validasi hasil unduhan berdasarkan output `ffprobe -print_format json`.
 *
 * Dipisahkan dari Android agar bisa diuji di JVM. Pipeline tetap memakai
 * pemeriksaan signature [FileSignature] lebih dulu; probe ini memastikan
 * kontainer benar-benar berisi stream yang diminta dan punya durasi wajar,
 * sehingga file terpotong tidak ditandai selesai.
 *
 * Sejak optimasi smart pipeline, [Info] juga menyimpan codec per stream supaya
 * pipeline bisa memutuskan operasi FFmpeg termurah (copy/remux/encode) dan
 * mendeteksi cover yang sudah tertanam tanpa pemanggilan FFmpeg tambahan.
 */
object MediaProbe {

    enum class Expect { AUDIO, VIDEO }

    /**
     * Codec stream `video` yang sebenarnya adalah gambar sampul (attached_pic),
     * bukan video bergerak. Dipakai untuk menyaring stream saat membaca codec
     * video asli dan saat mendeteksi cover tertanam.
     */
    val ATTACHED_PICTURE_CODECS = setOf("mjpeg", "png", "bmp", "gif", "webp")

    /** Satu stream hasil `-show_streams`. */
    data class StreamInfo(val type: String, val codec: String?)

    data class Info(
        val durationSeconds: Double?,
        val streamTypes: List<String>,
        val formatName: String?,
        val streams: List<StreamInfo> = emptyList(),
        /** Tag kontainer (judul/artis/album), kunci sudah dihuruf-kecilkan. */
        val tags: Map<String, String> = emptyMap(),
    ) {
        /** Codec video bergerak; stream sampul (attached_pic) diabaikan. */
        fun videoCodec(): String? = streams
            .firstOrNull { it.type == "video" && it.codec !in ATTACHED_PICTURE_CODECS }
            ?.codec

        fun audioCodec(): String? = streams.firstOrNull { it.type == "audio" }?.codec

        /** Berkas sudah punya cover tertanam, jadi tidak perlu di-embed lagi. */
        fun hasAttachedPicture(): Boolean =
            streams.any { it.type == "video" && it.codec in ATTACHED_PICTURE_CODECS }

        /** Nilai tag kontainer, mis. [tag]("artist"). Null bila tidak ada. */
        fun tag(name: String): String? = tags[name.lowercase()]?.takeIf { it.isNotBlank() }
    }

    private const val MIN_DURATION_SECONDS = 0.5

    fun parse(json: String): Info? {
        val start = json.indexOf('{')
        val end = json.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        val root = try {
            Json.parseToJsonElement(json.substring(start, end + 1)).jsonObject
        } catch (_: Exception) {
            return null
        }

        val streams = (root["streams"] as? JsonArray).orEmpty()
        val parsed = streams.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val type = obj["codec_type"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            StreamInfo(type = type, codec = obj["codec_name"]?.jsonPrimitive?.contentOrNull)
        }
        if (parsed.isEmpty()) return null

        val format = root["format"] as? JsonObject
        val duration = format?.get("duration")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            ?: streams.firstNotNullOfOrNull { element ->
                (element as? JsonObject)
                    ?.get("duration")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.toDoubleOrNull()
            }

        return Info(
            durationSeconds = duration,
            streamTypes = parsed.map { it.type },
            formatName = format?.get("format_name")?.jsonPrimitive?.contentOrNull,
            streams = parsed,
            tags = parseTags(format),
        )
    }

    /**
     * Tag kontainer dari `format.tags`. Kunci ffprobe bisa datang dalam huruf
     * besar-kecil berbeda antar kontainer (ID3 di MP3, ilst di MP4), jadi
     * semuanya dihuruf-kecilkan agar pencarian konsisten.
     */
    private fun parseTags(format: JsonObject?): Map<String, String> {
        val tags = format?.get("tags") as? JsonObject ?: return emptyMap()
        val out = LinkedHashMap<String, String>(tags.size)
        tags.forEach { (key, value) ->
            val text = (value as? JsonPrimitive)?.contentOrNull ?: return@forEach
            if (text.isNotBlank()) out[key.lowercase()] = text.trim()
        }
        return out
    }

    fun isValid(info: Info?, expect: Expect): Boolean {
        if (info == null) return false
        val required = when (expect) {
            Expect.AUDIO -> "audio"
            Expect.VIDEO -> "video"
        }
        if (required !in info.streamTypes) return false
        val duration = info.durationSeconds ?: return false
        return duration >= MIN_DURATION_SECONDS
    }
}
