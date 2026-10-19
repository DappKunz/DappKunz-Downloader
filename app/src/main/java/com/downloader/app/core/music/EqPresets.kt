package com.downloader.app.core.music

import kotlin.math.log10
import kotlin.math.roundToInt

object EqPresets {

    const val NORMAL = "Normal"
    const val BASS = "Bass Boost"
    const val TREBLE = "Treble Boost"
    const val VOCAL = "Vocal"
    const val ROCK = "Rock"
    const val POP = "Pop"
    const val CLASSICAL = "Classical"
    const val CUSTOM = "Custom"

    val BUILT_IN = listOf(NORMAL, BASS, TREBLE, VOCAL, ROCK, POP, CLASSICAL)

    private val ANCHORS: Map<String, List<Pair<Double, Int>>> = mapOf(
        NORMAL to listOf(50.0 to 0),
        BASS to listOf(
            50.0 to 8, 150.0 to 7, 400.0 to 3, 1000.0 to 0, 4000.0 to 0, 14000.0 to 0,
        ),
        TREBLE to listOf(
            50.0 to 0, 150.0 to 0, 400.0 to 0, 1000.0 to 1, 4000.0 to 6, 14000.0 to 8,
        ),
        VOCAL to listOf(
            50.0 to -3, 150.0 to -1, 500.0 to 3, 1500.0 to 5, 4000.0 to 3, 14000.0 to -2,
        ),
        ROCK to listOf(
            50.0 to 6, 200.0 to 4, 500.0 to -2, 1500.0 to -3, 4000.0 to 4, 14000.0 to 6,
        ),
        POP to listOf(
            50.0 to 3, 200.0 to 2, 800.0 to 0, 2500.0 to 2, 6000.0 to 4, 14000.0 to 3,
        ),
        CLASSICAL to listOf(
            50.0 to 4, 200.0 to 3, 800.0 to 1, 2500.0 to 0, 6000.0 to 2, 14000.0 to 4,
        ),
    )

    fun anchorsFor(preset: String): List<Pair<Double, Int>> =
        ANCHORS[preset] ?: ANCHORS.getValue(NORMAL)

    fun gainDbAt(preset: String, centerHz: Double): Int {
        val anchors = anchorsFor(preset)
        if (anchors.size == 1) return anchors.first().second
        val x = log10(centerHz.coerceAtLeast(1.0))
        if (x <= log10(anchors.first().first)) return anchors.first().second
        if (x >= log10(anchors.last().first)) return anchors.last().second
        for (i in 0 until anchors.size - 1) {
            val (f0, g0) = anchors[i]
            val (f1, g1) = anchors[i + 1]
            val x0 = log10(f0)
            val x1 = log10(f1)
            if (x in x0..x1) {
                val t = if (x1 == x0) 0.0 else (x - x0) / (x1 - x0)
                return (g0 + t * (g1 - g0)).roundToInt()
            }
        }
        return 0
    }

    fun gainsDb(preset: String, centerHz: List<Int>): List<Int> =
        centerHz.map { gainDbAt(preset, it.toDouble()) }

    fun clampDb(valueDb: Int, minDb: Int, maxDb: Int): Int = valueDb.coerceIn(minDb, maxDb)

    fun dbToMillibel(db: Int): Int = db * 100

    fun millibelToDb(millibel: Int): Int = (millibel / 100.0).roundToInt()
}
