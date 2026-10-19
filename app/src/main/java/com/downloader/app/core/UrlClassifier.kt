package com.downloader.app.core

import com.downloader.app.core.util.FileNames
import java.net.URI
import java.util.Locale

enum class MediaClass { AUDIO, VIDEO, IMAGE, UNKNOWN }

enum class ContentKind(val value: String) {
    TRACK("track"),
    SONG("song"),
    ALBUM("album"),
    PLAYLIST("playlist"),
    VIDEO("video"),
    REEL("reel"),
    SHORT("short"),
    PIN("pin"),
    POST("post"),
    GALLERY("gallery"),
    ARTWORK("artwork"),
    UNKNOWN("unknown"),
}

data class UrlAnalysis(
    val platform: String,
    val contentKind: ContentKind,
    val mediaClass: MediaClass,
    val confidence: Double,
    val signals: List<String>,
)

object UrlClassifier {

    private val MUSIC_DOMAINS = mapOf(
        "open.spotify.com" to "spotify",
        "spotify.com" to "spotify",
        "music.apple.com" to "apple_music",
        "soundcloud.com" to "soundcloud",
        "m.soundcloud.com" to "soundcloud",
        "tidal.com" to "tidal",
        "listen.tidal.com" to "tidal",
        "deezer.com" to "deezer",
        "www.deezer.com" to "deezer",
        "music.youtube.com" to "youtube_music",
        "bandcamp.com" to "bandcamp",
        "music.amazon.com" to "amazon_music",
        "audiomack.com" to "audiomack",
        "mixcloud.com" to "mixcloud",
        "www.mixcloud.com" to "mixcloud",
        "pandora.com" to "pandora",
        "www.pandora.com" to "pandora",
        "qobuz.com" to "qobuz",
        "open.qobuz.com" to "qobuz",
        "jiosaavn.com" to "jiosaavn",
        "www.jiosaavn.com" to "jiosaavn",
        "gaana.com" to "gaana",
        "iheart.com" to "iheart",
        "www.iheart.com" to "iheart",
        "podcasts.apple.com" to "apple_podcasts",
        "castbox.fm" to "castbox",
        "podbean.com" to "podbean",
    )

    private val VIDEO_DOMAINS = mapOf(
        "youtube.com" to "youtube",
        "www.youtube.com" to "youtube",
        "m.youtube.com" to "youtube",
        "youtu.be" to "youtube",
        "tiktok.com" to "tiktok",
        "www.tiktok.com" to "tiktok",
        "vm.tiktok.com" to "tiktok",
        "vimeo.com" to "vimeo",
        "player.vimeo.com" to "vimeo",
        "twitter.com" to "twitter",
        "x.com" to "twitter",
        "facebook.com" to "facebook",
        "www.facebook.com" to "facebook",
        "fb.watch" to "facebook",
        "dailymotion.com" to "dailymotion",
        "www.dailymotion.com" to "dailymotion",
        "twitch.tv" to "twitch",
        "www.twitch.tv" to "twitch",
        "clips.twitch.tv" to "twitch",
        "kick.com" to "kick",
        "bilibili.com" to "bilibili",
        "www.bilibili.com" to "bilibili",
        "bilibili.tv" to "bilibili",
        "rumble.com" to "rumble",
        "odysee.com" to "odysee",
        "likee.video" to "likee",
        "kwai.com" to "kwai",
        "coub.com" to "coub",
        "threads.net" to "threads",
        "streamable.com" to "streamable",
        "loom.com" to "loom",
    )

    private val IMAGE_DOMAINS = mapOf(
        "pinterest.com" to "pinterest",
        "www.pinterest.com" to "pinterest",
        "pin.it" to "pinterest",
        "id.pinterest.com" to "pinterest",
        "instagram.com" to "instagram",
        "www.instagram.com" to "instagram",
        "flickr.com" to "flickr",
        "www.flickr.com" to "flickr",
        "imgur.com" to "imgur",
        "pixiv.net" to "pixiv",
        "www.pixiv.net" to "pixiv",
        "artstation.com" to "artstation",
        "www.artstation.com" to "artstation",
        "behance.net" to "behance",
        "www.behance.net" to "behance",
        "deviantart.com" to "deviantart",
        "www.deviantart.com" to "deviantart",
        "cara.app" to "cara",
        "dribbble.com" to "dribbble",
        "500px.com" to "500px",
        "unsplash.com" to "unsplash",
        "pexels.com" to "pexels",
        "www.pexels.com" to "pexels",
        "vsco.co" to "vsco",
        "giphy.com" to "giphy",
        "tenor.com" to "tenor",
        "reddit.com" to "reddit",
        "preview.redd.it" to "reddit",
        "i.redd.it" to "reddit",
    )

    private val DEDICATED_VIDEO_HOSTS = setOf(
        "youtube", "vimeo", "dailymotion", "twitch", "kick",
        "bilibili", "rumble", "odysee", "likee", "kwai", "coub",
        "streamable", "loom",
    )

    private val DEDICATED_MUSIC_PLATFORMS = setOf(
        "spotify", "apple_music", "soundcloud", "tidal", "deezer",
        "youtube_music", "bandcamp", "amazon_music", "audiomack",
        "mixcloud", "pandora", "qobuz", "jiosaavn", "gaana",
        "iheart", "apple_podcasts", "castbox", "podbean",
    )

    private val DEDICATED_IMAGE_PORTFOLIOS = setOf(
        "pinterest", "flickr", "imgur", "pixiv", "artstation",
        "behance", "deviantart", "cara", "dribbble", "500px",
        "unsplash", "pexels", "vsco", "giphy", "tenor",
    )

    val DRM_UNSUPPORTED_PLATFORMS = setOf("apple_music", "tidal", "deezer")

    fun parseHost(url: String): String? = try {
        URI(url).host?.lowercase(Locale.ROOT)
    } catch (_: Exception) {
        null
    }

    fun analyze(url: String): UrlAnalysis {
        val trimmed = url.trim()
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return UrlAnalysis("unknown", ContentKind.UNKNOWN, MediaClass.UNKNOWN, 0.0, emptyList())
        }
        val host = (uri.host ?: "").lowercase(Locale.ROOT)
        val path = uri.path ?: ""
        val query = uri.query ?: ""

        val platform = detectPlatform(host)
        val kind = detectContentKind(path, query, platform, host)

        val signals = mutableListOf<String>()
        var audioScore = 0.0
        var videoScore = 0.0
        var imageScore = 0.0
        val ext = FileNames.extensionOf(trimmed)

        if (platform in DEDICATED_MUSIC_PLATFORMS) {
            audioScore += 0.40
            signals.add("known_music_platform")
        }
        if (kind == ContentKind.TRACK || kind == ContentKind.SONG) {
            audioScore += 0.35
            signals.add("track_or_song_path")
        }
        if (kind == ContentKind.ALBUM) {
            audioScore += 0.35
            signals.add("album_path")
        }
        if (kind == ContentKind.PLAYLIST && platform !in setOf("youtube", "tiktok", "facebook")) {
            audioScore += 0.30
            signals.add("music_playlist_pattern")
        }
        if (ext in FileNames.DIRECT_AUDIO_EXTENSIONS) {
            audioScore += 0.85
            signals.add("direct_audio_extension")
        }

        if (platform in DEDICATED_VIDEO_HOSTS) {
            videoScore += 0.35
            signals.add("known_video_platform")
        } else if (platform in VIDEO_DOMAINS.values.toSet()) {
            videoScore += 0.25
            signals.add("known_video_platform")
        }
        if (kind == ContentKind.VIDEO) {
            videoScore += 0.55
            signals.add("video_path")
        }
        if (kind == ContentKind.SHORT) {
            videoScore += 0.70
            signals.add("short_path")
        }
        if (kind == ContentKind.REEL) {
            videoScore += 0.65
            signals.add("reel_path")
        }
        if (platform == "tiktok") {
            videoScore += 0.45
            signals.add("tiktok_video_stream")
        }
        if (host == "fb.watch" || host.endsWith(".fb.watch")) {
            videoScore += 0.50
            signals.add("fb_watch_shortcut")
        }
        if (host == "youtu.be") {
            videoScore += 0.50
            signals.add("youtu_be_shortcut")
        }
        if (platform == "twitter" && ("/status/" in path || "/video/" in path)) {
            videoScore += 0.45
            signals.add("twitter_media_tweet")
        }
        if (ext in FileNames.DIRECT_VIDEO_EXTENSIONS) {
            videoScore += 0.85
            signals.add("direct_video_extension")
        }

        if (platform in DEDICATED_IMAGE_PORTFOLIOS) {
            imageScore += 0.40
            signals.add("known_image_platform")
        } else if (platform in IMAGE_DOMAINS.values.toSet()) {
            imageScore += 0.30
            signals.add("known_image_platform")
        }
        if (kind == ContentKind.PIN || kind == ContentKind.ARTWORK) {
            imageScore += 0.45
            signals.add("pin_or_artwork_path")
        }
        if (kind == ContentKind.GALLERY) {
            imageScore += 0.45
            signals.add("gallery_path")
        }
        if (kind == ContentKind.POST && platform in IMAGE_DOMAINS.values.toSet()) {
            imageScore += 0.35
            signals.add("image_post_path")
        }
        if (ext in FileNames.DIRECT_IMAGE_EXTENSIONS) {
            imageScore += 0.85
            signals.add("direct_image_extension")
        }

        val scores = mapOf(
            MediaClass.AUDIO to audioScore.coerceAtMost(1.0),
            MediaClass.VIDEO to videoScore.coerceAtMost(1.0),
            MediaClass.IMAGE to imageScore.coerceAtMost(1.0),
        )
        val (mediaClass, confidence) = chooseMediaClass(scores)

        return UrlAnalysis(platform, kind, mediaClass, confidence, signals)
    }

    private fun chooseMediaClass(scores: Map<MediaClass, Double>): Pair<MediaClass, Double> {
        val ordered = scores.entries.sortedByDescending { it.value }
        val best = ordered.first()
        if (best.value < 0.60) return MediaClass.UNKNOWN to best.value
        if (ordered.size > 1 && (best.value - ordered[1].value) < 0.15) {
            return MediaClass.UNKNOWN to best.value
        }
        return best.key to best.value
    }

    private fun detectPlatform(host: String): String {
        val noWww = if (host.startsWith("www.")) host.removePrefix("www.") else host
        MUSIC_DOMAINS[host]?.let { return it }
        MUSIC_DOMAINS[noWww]?.let { return it }
        VIDEO_DOMAINS[host]?.let { return it }
        VIDEO_DOMAINS[noWww]?.let { return it }
        IMAGE_DOMAINS[host]?.let { return it }
        IMAGE_DOMAINS[noWww]?.let { return it }

        if (host.endsWith(".bandcamp.com")) return "bandcamp"
        if (host == "pin.it" || host.endsWith(".pin.it")) return "pinterest"
        if (host == "fb.watch" || host.endsWith(".fb.watch")) return "facebook"
        if (host == "youtu.be") return "youtube"
        if (host == "t.co") return "twitter"

        for ((domain, platform) in MUSIC_DOMAINS) {
            if (host == domain || host.endsWith(".$domain")) return platform
        }
        for ((domain, platform) in VIDEO_DOMAINS) {
            if (host == domain || host.endsWith(".$domain")) return platform
        }
        for ((domain, platform) in IMAGE_DOMAINS) {
            if (host == domain || host.endsWith(".$domain")) return platform
        }
        return "unknown"
    }

    private fun detectContentKind(path: String, query: String, platform: String, host: String): ContentKind {
        val value = "$path?$query".lowercase(Locale.ROOT)
        val parts = path.trim('/').split('/').filter { it.isNotEmpty() }

        if (platform == "youtube_music") {
            if ("/watch" in value || "/channel/" in value || "v=" in value) return ContentKind.TRACK
            if ("/playlist" in value || "list=" in value) return ContentKind.PLAYLIST
            return ContentKind.SONG
        }

        if ("/track/" in value || "/tracks/" in value) return ContentKind.TRACK
        if ("/song/" in value || "/songs/" in value) return ContentKind.SONG
        if ("/album/" in value || "/albums/" in value) return ContentKind.ALBUM
        if ("/playlist/" in value || "/playlists/" in value || "list=" in value || "/sets/" in value) return ContentKind.PLAYLIST
        if ("/episode/" in value || "/podcast/" in value || "/podcasts/" in value) return ContentKind.TRACK

        if ("/shorts/" in value || "/short/" in value) return ContentKind.SHORT
        if ("/reel/" in value || "/reels/" in value) return ContentKind.REEL
        if ("/watch" in value || "/video/" in value || "/videos/" in value || "/v/" in value ||
            "/clip/" in value || "/clips/" in value
        ) return ContentKind.VIDEO
        if (host == "fb.watch" || host.endsWith("fb.watch")) return ContentKind.VIDEO
        if (host == "youtu.be" && parts.isNotEmpty()) return ContentKind.VIDEO
        if (host.startsWith("clips.twitch.tv") || (platform == "twitch" && parts.firstOrNull() == "videos")) {
            return ContentKind.VIDEO
        }

        if ("/pin/" in value || host == "pin.it" || host.endsWith("pin.it")) return ContentKind.PIN
        if ("/artwork/" in value || "/artworks/" in value || "/art/" in value || "/shots/" in value ||
            "/illustration/" in value
        ) return ContentKind.ARTWORK
        if ("/photo/" in value || "/photos/" in value || "/p/" in value) return ContentKind.POST
        if ("/gallery/" in value || "/galleries/" in value) return ContentKind.GALLERY
        if ("/gifs/" in value || "/gif/" in value) return ContentKind.POST
        if ("/post/" in value || "/posts/" in value || "/comments/" in value || "/media/" in value) return ContentKind.POST

        val ext = FileNames.extensionOf(path.lowercase(Locale.ROOT))
        if (ext in FileNames.DIRECT_IMAGE_EXTENSIONS) return ContentKind.POST
        if (ext in FileNames.DIRECT_VIDEO_EXTENSIONS) return ContentKind.VIDEO
        if (ext in FileNames.DIRECT_AUDIO_EXTENSIONS) return ContentKind.TRACK

        if (platform == "soundcloud") {
            if (parts.size >= 2 && parts[1] == "sets") return ContentKind.PLAYLIST
            if (parts.size >= 2) return ContentKind.TRACK
        }
        if (platform in setOf("mixcloud", "audiomack", "pandora", "qobuz", "jiosaavn", "gaana", "iheart")) {
            if (parts.isNotEmpty()) return ContentKind.TRACK
        }
        if (platform == "bandcamp") {
            if (parts.size >= 2 && parts[0] == "track") return ContentKind.TRACK
            if (parts.size >= 2 && parts[0] == "album") return ContentKind.ALBUM
            if (parts.isNotEmpty()) return ContentKind.TRACK
        }
        if (platform in DEDICATED_IMAGE_PORTFOLIOS && parts.isNotEmpty()) return ContentKind.ARTWORK
        if (platform in DEDICATED_VIDEO_HOSTS && parts.isNotEmpty()) return ContentKind.VIDEO
        if (platform == "twitter" && "/status/" in value) return ContentKind.VIDEO

        return ContentKind.UNKNOWN
    }
}
