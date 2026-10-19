package com.downloader.app.core.pipeline

import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.AppException
import com.downloader.app.core.ContentKind
import com.downloader.app.core.MediaClass
import com.downloader.app.core.UrlClassifier
import com.downloader.app.core.engine.YtDlpEngine
import com.downloader.app.core.log.AppLog
import com.downloader.app.core.model.AudioFormat
import com.downloader.app.core.model.Capabilities
import com.downloader.app.core.model.Formats
import com.downloader.app.core.model.MediaInfo
import com.downloader.app.core.model.NormalizedMetadata
import com.downloader.app.core.model.PlaylistInfo
import com.downloader.app.core.model.PlaylistItem
import com.downloader.app.core.model.SourceInfo
import com.downloader.app.core.model.VideoFormat
import com.downloader.app.core.util.FileNames
import com.downloader.app.core.util.Redact
import com.downloader.app.core.util.floatOrNull
import com.downloader.app.core.util.intOrNull
import com.downloader.app.core.util.lastThumbnailUrl
import com.downloader.app.core.util.stringOrNull
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object MediaAnalyzer {

    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    private data class Cached(val at: Long, val meta: NormalizedMetadata)

    private val cache = ConcurrentHashMap<String, Cached>()

    suspend fun analyze(urlRaw: String): NormalizedMetadata {
        val url = urlRaw.trim()
        validateUrl(url)

        cache[url]?.let {
            if (System.currentTimeMillis() - it.at < CACHE_TTL_MS) return it.meta
        }

        val cls = UrlClassifier.analyze(url)

        if (cls.platform == "spotify") {
            throw AppException("UNSUPPORTED_SOURCE", DownloaderApp.str(R.string.err_spotify))
        }

        if (cls.platform in UrlClassifier.DRM_UNSUPPORTED_PLATFORMS && cls.mediaClass == MediaClass.AUDIO) {
            val name = cls.platform.replace('_', ' ').split(' ').joinToString(" ") { part ->
                part.replaceFirstChar { it.uppercase() }
            }
            throw AppException("UNSUPPORTED_SOURCE", DownloaderApp.str(R.string.err_drm_platform, name))
        }

        val analysisStartedAt = System.currentTimeMillis()
        val result = when (cls.mediaClass) {
            MediaClass.IMAGE -> analyzeImage(url, cls.platform)
            else -> analyzeWithYtDlp(url, cls.platform, cls.contentKind, cls.mediaClass)
        }
        // Analisis terpisah dari unduhan: satu jalur ini bisa memanggil proses
        // yt-dlp penuh, jadi durasinya perlu terlihat di log untuk perbandingan.
        AppLog.d(
            "Analyzer",
            "analisis=${"%.2f".format(Locale.US, (System.currentTimeMillis() - analysisStartedAt) / 1000.0)}s " +
                "platform=${cls.platform} tipe=${result.media.type} " +
                "playlist=${result.playlist?.items?.size ?: 0} url=${Redact.url(url)}",
        )

        cache[url] = Cached(System.currentTimeMillis(), result)
        return result
    }

    fun invalidate(url: String) {
        cache.remove(url.trim())
    }

    fun validateUrl(url: String) {
        if (url.isBlank()) {
            throw AppException("INVALID_URL", DownloaderApp.str(R.string.err_empty_url))
        }
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            throw AppException("INVALID_URL", DownloaderApp.str(R.string.err_invalid_url))
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw AppException("INVALID_URL", DownloaderApp.str(R.string.err_http_only))
        }
        if (uri.host.isNullOrBlank()) {
            throw AppException("INVALID_URL", DownloaderApp.str(R.string.err_no_domain))
        }
    }

    private suspend fun analyzeImage(url: String, platform: String): NormalizedMetadata {
        val direct = FileNames.isDirectImage(url)
        if (direct) {
            val fileName = url.substringBefore('?').substringAfterLast('/').ifBlank { "photo.jpg" }
            return NormalizedMetadata(
                source = SourceInfo(url = url, platform = platform),
                media = MediaInfo(
                    type = "photo",
                    isPlaylist = false,
                    title = FileNames.sanitizeFilename(fileName),
                    creator = null,
                    thumbnail = url,
                    duration = null,
                ),
                capabilities = Capabilities(photo = true, image = true),
                formats = Formats(),
                playlist = null,
            )
        }

        val info = try {
            YtDlpEngine.dumpInfo(url, flatPlaylist = false)
        } catch (e: AppException) {
            throw AppException("UNSUPPORTED_SOURCE", DownloaderApp.str(R.string.err_image_gallery))
        }
        val normalized = normalize(url, info, platform, MediaClass.IMAGE)
        return normalized
    }

    private suspend fun analyzeWithYtDlp(
        url: String,
        platform: String,
        kind: ContentKind,
        mediaClass: MediaClass,
    ): NormalizedMetadata {
        val lower = url.lowercase()
        val looksLikePlaylist = kind == ContentKind.PLAYLIST ||
            (("list=" in lower) && ("watch?" !in lower)) ||
            "/playlist/" in lower ||
            "/sets/" in lower

        val info = YtDlpEngine.dumpInfo(url, flatPlaylist = looksLikePlaylist)
        return normalize(url, info, platform, mediaClass)
    }

    private fun normalize(
        url: String,
        info: JsonObject,
        platform: String,
        mediaClass: MediaClass,
    ): NormalizedMetadata {
        val type = info.stringOrNull("_type")
        val rawEntries = (info["entries"] as? JsonArray)
        val isPlaylist = type == "playlist" || rawEntries != null
        val entries = rawEntries?.mapNotNull { it as? JsonObject } ?: emptyList()

        var playlist: PlaylistInfo? = null
        if (isPlaylist && entries.isNotEmpty()) {
            val items = entries.mapIndexed { idx, entry ->
                PlaylistItem(
                    index = idx + 1,
                    id = entry.stringOrNull("id") ?: "item_${idx + 1}",
                    title = entry.stringOrNull("title") ?: "Item ${idx + 1}",
                    duration = entry.intOrNull("duration"),
                    thumbnail = entry.stringOrNull("thumbnail") ?: lastThumbnailUrl(entry),
                    url = entry.stringOrNull("url") ?: entry.stringOrNull("webpage_url"),
                    // Artis per lagu. Pada daftar rata (`--flat-playlist`) tidak
                    // semua extractor mengirimkannya; kalau kosong, nilai asli
                    // diambil dari info berkas hasil unduhan itu sendiri
                    // (lihat DownloadPipeline.readDownloadInfo).
                    artist = entry.stringOrNull("artist")
                        ?: entry.stringOrNull("uploader")
                        ?: entry.stringOrNull("channel"),
                )
            }
            playlist = PlaylistInfo(itemCount = items.size, items = items)
        }

        val target = entries.firstOrNull() ?: info

        val title = when {
            isPlaylist && info.stringOrNull("title") != null -> info.stringOrNull("title")!!
            else -> info.stringOrNull("title") ?: target.stringOrNull("title") ?: "Untitled Media"
        }

        val artist = info.stringOrNull("artist")
            ?: target.stringOrNull("artist")
            ?: info.stringOrNull("creator")
            ?: target.stringOrNull("creator")

        val album = info.stringOrNull("album") ?: target.stringOrNull("album")

        val creator = artist
            ?: info.stringOrNull("uploader")
            ?: info.stringOrNull("channel")
            ?: info.stringOrNull("uploader_id")
            ?: target.stringOrNull("uploader")
            ?: target.stringOrNull("channel")

        val thumbnail = info.stringOrNull("thumbnail")
            ?: target.stringOrNull("thumbnail")
            ?: lastThumbnailUrl(info)
            ?: lastThumbnailUrl(target)

        val duration: Int? = if (isPlaylist && entries.isNotEmpty()) {
            var total = 0
            var has = false
            entries.forEach { entry ->
                entry.intOrNull("duration")?.let {
                    total += it
                    has = true
                }
            }
            when {
                has -> total
                info.intOrNull("duration") != null -> info.intOrNull("duration")
                else -> target.intOrNull("duration")
            }
        } else {
            target.intOrNull("duration")
        }

        playlist = playlist?.copy(title = title, totalDuration = duration)

        val rawFormats = (target["formats"] as? JsonArray)
            ?: (info["formats"] as? JsonArray)
            ?: JsonArray(emptyList())

        val videoFormats = mutableListOf<VideoFormat>()
        val audioFormats = mutableListOf<AudioFormat>()
        val seenHeights = mutableSetOf<Int>()
        val seenAudio = mutableSetOf<String>()

        for (element in rawFormats) {
            val f = element as? JsonObject ?: continue
            val vcodec = f.stringOrNull("vcodec")
            val acodec = f.stringOrNull("acodec")
            val height = f.intOrNull("height")
            val abr = f.intOrNull("abr") ?: f.intOrNull("tbr")
            val ext = f.stringOrNull("ext") ?: ""
            val note = f.stringOrNull("format_note")?.lowercase() ?: ""
            val fps = f.floatOrNull("fps")

            if ("storyboard" in note || (fps != null && fps < 1f)) continue

            if (vcodec != null && vcodec != "none" && height != null && height >= 144 && height !in seenHeights) {
                seenHeights.add(height)
                videoFormats.add(VideoFormat(container = "mp4", height = height, fps = fps))
            }

            if (acodec != null && acodec != "none" && (vcodec == null || vcodec == "none")) {
                val key = "$acodec|$ext|$abr"
                if (key !in seenAudio) {
                    seenAudio.add(key)
                    audioFormats.add(
                        AudioFormat(
                            codec = acodec,
                            container = if ("mp4a" in acodec) "m4a" else "mp3",
                            bitrate = abr ?: 128,
                        ),
                    )
                }
            }
        }

        videoFormats.sortBy { it.height ?: 0 }

        val isRealPlaylist = isPlaylist && entries.size > 1
        val detectedVideo = videoFormats.isNotEmpty()
        val detectedAudio = audioFormats.isNotEmpty()
        val photoImages = collectImageUrls(url, info, target, entries)
        val isPhotoPost = photoImages.isNotEmpty() &&
            (isTikTokPhotoUrl(url, platform) || (!detectedVideo && !detectedAudio && !isRealPlaylist))

        if (!isPhotoPost) {
            val audioLikely = mediaClass == MediaClass.AUDIO
            if (videoFormats.isEmpty() && !isRealPlaylist && !audioLikely) {
                videoFormats.add(VideoFormat(container = "mp4", height = 720))
                videoFormats.add(VideoFormat(container = "mp4", height = 1080))
            }
            if (audioFormats.isEmpty()) {
                audioFormats.add(AudioFormat(codec = "aac", container = "m4a", bitrate = 128))
                audioFormats.add(AudioFormat(codec = "mp3", container = "mp3", bitrate = 320))
            }
        }

        val audioPlaylist = isRealPlaylist && mediaClass == MediaClass.AUDIO
        val hasVideo = videoFormats.isNotEmpty() || (isRealPlaylist && !audioPlaylist)
        val hasAudio = audioFormats.isNotEmpty() || hasVideo
        val mediaType = when {
            isPhotoPost -> "photo"
            hasVideo -> "video"
            else -> "audio"
        }

        val platformResolved = when {
            platform != "unknown" && platform.isNotBlank() -> platform
            else -> info.stringOrNull("extractor_key") ?: info.stringOrNull("extractor") ?: "unknown"
        }

        return NormalizedMetadata(
            source = SourceInfo(url = url, platform = platformResolved),
            media = MediaInfo(
                type = mediaType,
                isPlaylist = isRealPlaylist,
                title = title,
                creator = creator,
                artist = artist,
                album = album,
                thumbnail = thumbnail,
                duration = if (isRealPlaylist) duration else duration,
            ),
            capabilities = Capabilities(
                audio = hasAudio,
                video = hasVideo,
                photo = isPhotoPost,
                mp3 = hasAudio,
                m4a = hasAudio,
                mp4 = hasVideo,
                image = isPhotoPost,
            ),
            formats = Formats(audio = audioFormats, video = videoFormats),
            playlist = playlist.takeIf { isRealPlaylist },
            photoImages = if (isPhotoPost) photoImages else emptyList(),
        )
    }

    private fun isTikTokPhotoUrl(url: String, platform: String): Boolean {
        val lower = url.lowercase()
        val isTiktok = platform == "tiktok" || "tiktok.com" in lower
        return isTiktok && ("/photo/" in lower || "/image/" in lower)
    }

    private fun collectImageUrls(
        url: String,
        info: JsonObject,
        target: JsonObject,
        entries: List<JsonObject>,
    ): List<String> {
        val out = linkedSetOf<String>()
        val sources = mutableListOf(info)
        if (target !== info) sources.add(target)
        sources.forEach { obj ->
            (obj["thumbnails"] as? JsonArray)?.forEach { element ->
                val thumb = element as? JsonObject ?: return@forEach
                thumb.stringOrNull("url")?.takeIf { it.startsWith("http") }?.let { out.add(it) }
            }
            (obj["formats"] as? JsonArray)?.forEach { element ->
                val format = element as? JsonObject ?: return@forEach
                val ext = format.stringOrNull("ext")?.lowercase().orEmpty()
                if (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp") {
                    format.stringOrNull("url")?.takeIf { it.startsWith("http") }?.let { out.add(it) }
                }
            }
        }
        entries.forEach { entry ->
            entry.stringOrNull("url")
                ?.takeIf { it.startsWith("http") && FileNames.isDirectImage(it) }
                ?.let { out.add(it) }
            entry.stringOrNull("thumbnail")?.takeIf { it.startsWith("http") }?.let { out.add(it) }
            lastThumbnailUrl(entry)?.takeIf { it.startsWith("http") }?.let { out.add(it) }
        }
        if (FileNames.isDirectImage(url)) out.add(url)
        return out.take(100)
    }

}
