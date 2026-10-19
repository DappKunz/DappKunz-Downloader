package com.downloader.app.core.pipeline

/**
 * Perencana operasi media: memilih cara termurah untuk menghasilkan berkas
 * target dari berkas sumber yang sudah diunduh.
 *
 * Kenapa dipisah dan murni (tanpa Android/FFmpeg): keputusan ini sebelumnya
 * tersebar sebagai rangkaian percobaan di [DownloadPipeline] (coba `copy`,
 * kalau gagal `remux`, kalau gagal `encode`). Dua akibatnya:
 *
 * 1. **Percobaan yang sia-sia.** Sumber WebM (VP9 + Opus) lebih dulu dicoba
 *    `-c copy -f mp4`; perintah itu "berhasil" secara teknis, jadi validasi
 *    lama menerimanya dan rangkaian cadangan tidak pernah dipakai. Untuk sumber
 *    yang lebih jauh dari MP4, rangkaian itu bisa memakan 2–3 proses FFmpeg.
 * 2. **Hasil yang tidak kompatibel.** Opus/Vorbis di dalam MP4 tidak diputar
 *    baik oleh pemutar Android, jadi "sukses" dari `copy` bisa berarti berkas
 *    yang tidak berguna.
 *
 * Karena itu keputusan diambil dari **codec hasil ffprobe**, bukan dari
 * ekstensi berkas. Bila tebakan salah (mis. ffprobe tidak tersedia),
 * [escalation] menyediakan tangga cadangan sehingga pipeline tetap pulih.
 *
 * Semua fungsi di sini murni sehingga bisa diuji tanpa perangkat.
 */
object ConversionPlanner {

    /** Operasi yang mungkin dijalankan FFmpeg terhadap berkas video. */
    enum class Operation {
        /** Berkas sudah sesuai target: pindahkan saja, tanpa FFmpeg. */
        NATIVE,

        /** Container diubah, video **dan** audio di-copy (tanpa re-encode). */
        REMUX,

        /** Video di-copy, audio di-encode (mis. Opus/Vorbis -> AAC). */
        REMUX_AUDIO,

        /** Video dan audio di-encode ulang. */
        TRANSCODE,
    }

    /** Operasi untuk berkas audio. */
    enum class AudioOperation {
        /** Container sudah sesuai target: pindahkan saja, tanpa FFmpeg. */
        NATIVE,

        /** Codec/kontainer tidak sesuai: harus di-encode ulang. */
        TRANSCODE,
    }

    /**
     * Codec video yang aman berada di dalam MP4 bagi pemutar Android.
     * VP8 sengaja tidak dimasukkan: dukungannya di MP4 tidak konsisten, jadi
     * lebih baik di-encode ulang daripada menghasilkan berkas yang gagal putar.
     */
    private val MP4_VIDEO_CODECS = setOf("h264", "avc1", "hevc", "h265", "hvc1", "mpeg4", "av1", "vp9")

    /** Codec audio yang aman berada di dalam MP4. */
    private val MP4_AUDIO_CODECS = setOf("aac", "mp3", "ac3", "eac3", "alac")

    /** Kontainer yang isinya sudah berupa audio-MP4, sehingga target m4a native. */
    private val M4A_NATIVE_CONTAINERS = setOf("m4a", "mp4")

    private fun normalizeContainer(container: String): String =
        container.lowercase().removePrefix(".").trim()

    /** `null` berarti belum diketahui -> optimistis (validasi yang akan menolak). */
    private fun isMp4Video(codec: String?): Boolean =
        codec == null || codec.lowercase() in MP4_VIDEO_CODECS

    private fun isMp4Audio(codec: String?): Boolean =
        codec == null || codec.lowercase() in MP4_AUDIO_CODECS

    /**
     * Berkas audio sudah sesuai target sehingga cukup dipindahkan (tanpa FFmpeg)?
     *
     * Sengaja hanya melihat kontainer: berkas `.aac` (ADTS) **bukan** MP4, jadi
     * mengganti namanya menjadi `.m4a` akan menghasilkan berkas dengan
     * kontainer yang salah meski terlihat benar.
     */
    fun isAudioNative(targetFormat: String, container: String): Boolean {
        val c = normalizeContainer(container)
        return when (targetFormat) {
            "mp3" -> c == "mp3"
            "m4a" -> c in M4A_NATIVE_CONTAINERS
            else -> false
        }
    }

    fun planAudio(targetFormat: String, container: String): AudioOperation =
        if (isAudioNative(targetFormat, container)) AudioOperation.NATIVE else AudioOperation.TRANSCODE

    /**
     * Operasi pertama yang dicoba untuk berkas video yang belum native MP4.
     *
     * Video yang harus di-encode tidak pernah dicoba `-c copy` lebih dulu, dan
     * audio yang tidak kompatibel langsung memakai jalur "copy video + encode
     * audio" sehingga satu proses FFmpeg cukup untuk kasus paling umum
     * (WebM VP9/Opus).
     */
    fun planVideo(videoCodec: String?, audioCodec: String?): Operation = when {
        !isMp4Video(videoCodec) -> Operation.TRANSCODE
        !isMp4Audio(audioCodec) -> Operation.REMUX_AUDIO
        else -> Operation.REMUX
    }

    /**
     * Tangga cadangan bila operasi pertama gagal validasi: selalu menuju operasi
     * yang lebih berat tetapi lebih pasti berhasil, dan berhenti di
     * [Operation.TRANSCODE].
     */
    fun escalation(operation: Operation): List<Operation> = when (operation) {
        Operation.NATIVE -> listOf(Operation.REMUX, Operation.REMUX_AUDIO, Operation.TRANSCODE)
        Operation.REMUX -> listOf(Operation.REMUX_AUDIO, Operation.TRANSCODE)
        Operation.REMUX_AUDIO -> listOf(Operation.TRANSCODE)
        Operation.TRANSCODE -> emptyList()
    }

    /** Urutan percobaan lengkap: operasi terpilih lebih dulu, lalu cadangannya. */
    fun attempts(operation: Operation): List<Operation> = listOf(operation) + escalation(operation)

    /**
     * Apakah kombinasi codec ini benar-benar bisa diputar dari dalam MP4?
     *
     * Dipakai validator: `-c copy` yang menyisipkan Opus ke MP4 ditolak di sini
     * sehingga pipeline menaikkan operasi ke [Operation.REMUX_AUDIO] alih-alih
     * menyajikan berkas yang tidak bisa diputar.
     */
    fun isMp4Compatible(videoCodec: String?, audioCodec: String?): Boolean =
        isMp4Video(videoCodec) && isMp4Audio(audioCodec)

    /** Label operasi yang dipakai log pipeline dan riwayat unduhan. */
    fun modeLabel(operation: Operation): String = when (operation) {
        Operation.NATIVE -> "native"
        Operation.REMUX -> "copy"
        Operation.REMUX_AUDIO -> "remux"
        Operation.TRANSCODE -> "encode"
    }
}
