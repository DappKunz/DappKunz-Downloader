package com.downloader.app.core.music

import android.media.audiofx.Equalizer
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.log.AppLog
import com.downloader.app.core.storage.PrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EqualizerController {

    data class BandUi(
        val index: Int,
        val centerHz: Int,
        val minDb: Int,
        val maxDb: Int,
    )

    data class UiState(
        val available: Boolean = false,
        val enabled: Boolean = true,
        val preset: String = EqPresets.NORMAL,
        val bands: List<BandUi> = emptyList(),
        val gainsDb: List<Int> = emptyList(),
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var effect: Equalizer? = null
    private var attachedSession: Int = SESSION_UNSET
    private var failedSession: Int = SESSION_UNSET

    @Synchronized
    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == attachedSession && effect != null) return
        if (audioSessionId == failedSession && effect == null) return
        releaseLocked()
        val created: Equalizer
        try {
            created = Equalizer(0, audioSessionId)
        } catch (t: Throwable) {
            failedSession = audioSessionId
            markUnavailable(DownloaderApp.str(R.string.eq_unavailable))
            AppLog.e("Equalizer", "Inisialisasi gagal (session=$audioSessionId): ${t.message}")
            return
        }
        try {
            val bandCount = created.numberOfBands.toInt()
            if (bandCount <= 0) {
                created.release()
                failedSession = audioSessionId
                markUnavailable(DownloaderApp.str(R.string.eq_unavailable))
                return
            }
            val range = created.bandLevelRange
            val minDb = EqPresets.millibelToDb(range[0].toInt())
            val maxDb = EqPresets.millibelToDb(range[1].toInt())
            val bands = (0 until bandCount).map { index ->
                val centerHz = (created.getCenterFreq(index.toShort()) / 1000).coerceAtLeast(20)
                BandUi(index, centerHz, minDb, maxDb)
            }
            effect = created
            attachedSession = audioSessionId
            failedSession = SESSION_UNSET
            val gains = restoreGains(bandCount, bands)
            applyGainsLocked(gains, bands)
            created.enabled = PrefsRepository.eqEnabledValue()
            _state.value = UiState(
                available = true,
                enabled = created.enabled,
                preset = PrefsRepository.eqPresetValue().takeIf { it in EqPresets.BUILT_IN + EqPresets.CUSTOM }
                    ?: EqPresets.NORMAL,
                bands = bands,
                gainsDb = gains,
                message = null,
            )
            AppLog.d("Equalizer", "Terhubung (session=$audioSessionId, bands=$bandCount)")
        } catch (t: Throwable) {
            runCatching { created.release() }
            effect = null
            attachedSession = SESSION_UNSET
            failedSession = audioSessionId
            markUnavailable(DownloaderApp.str(R.string.eq_unavailable))
            AppLog.e("Equalizer", "Konfigurasi gagal: ${t.message}")
        }
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        PrefsRepository.setEqEnabled(enabled)
        val fx = effect
        if (fx == null) {
            _state.value = _state.value.copy(enabled = enabled)
            return
        }
        try {
            fx.enabled = enabled
        } catch (t: Throwable) {
            AppLog.e("Equalizer", "setEnabled gagal: ${t.message}")
        }
        _state.value = _state.value.copy(enabled = enabled)
    }

    @Synchronized
    fun setGain(bandIndex: Int, gainDb: Int) {
        val current = _state.value
        if (bandIndex !in current.bands.indices) return
        val band = current.bands[bandIndex]
        val clamped = EqPresets.clampDb(gainDb, band.minDb, band.maxDb)
        val fx = effect
        if (fx == null) {
            markUnavailable(DownloaderApp.str(R.string.eq_unavailable))
            return
        }
        try {
            fx.setBandLevel(bandIndex.toShort(), EqPresets.dbToMillibel(clamped).toShort())
        } catch (t: Throwable) {
            AppLog.e("Equalizer", "setGain gagal: ${t.message}")
            return
        }
        val gains = current.gainsDb.toMutableList().also { it[bandIndex] = clamped }
        persistGains(gains)
        val preset = if (current.preset != EqPresets.CUSTOM) EqPresets.CUSTOM else current.preset
        if (preset != current.preset) PrefsRepository.setEqPreset(preset)
        _state.value = current.copy(gainsDb = gains, preset = preset)
    }

    @Synchronized
    fun applyPreset(preset: String) {
        val current = _state.value
        val fx = effect
        val name = if (preset in EqPresets.BUILT_IN) preset else EqPresets.NORMAL
        if (fx == null || current.bands.isEmpty()) {
            PrefsRepository.setEqPreset(name)
            PrefsRepository.setEqGainsCsv("")
            _state.value = current.copy(preset = name)
            return
        }
        val gains = EqPresets.gainsDb(name, current.bands.map { it.centerHz })
            .mapIndexed { index, db ->
                EqPresets.clampDb(db, current.bands[index].minDb, current.bands[index].maxDb)
            }
        applyGainsLocked(gains, current.bands)
        persistGains(gains)
        PrefsRepository.setEqPreset(name)
        _state.value = current.copy(preset = name, gainsDb = gains)
        AppLog.d("Equalizer", "Preset: $name")
    }

    @Synchronized
    fun reset() {
        applyPreset(EqPresets.NORMAL)
    }

    @Synchronized
    fun release() {
        releaseLocked()
        failedSession = SESSION_UNSET
        _state.value = UiState(
            available = false,
            enabled = PrefsRepository.eqEnabledValue(),
            preset = PrefsRepository.eqPresetValue(),
        )
    }

    private fun releaseLocked() {
        runCatching { effect?.release() }
        effect = null
        attachedSession = SESSION_UNSET
    }

    private fun markUnavailable(message: String) {
        _state.value = _state.value.copy(available = false, message = message)
    }

    private fun restoreGains(bandCount: Int, bands: List<BandUi>): List<Int> {
        val stored = PrefsRepository.eqGainsValue()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
        if (stored.size == bandCount) {
            return stored.mapIndexed { index, db ->
                EqPresets.clampDb(db, bands[index].minDb, bands[index].maxDb)
            }
        }
        val preset = PrefsRepository.eqPresetValue().takeIf { it in EqPresets.BUILT_IN } ?: EqPresets.NORMAL
        PrefsRepository.setEqPreset(preset)
        val computed = if (preset == EqPresets.NORMAL) {
            List(bandCount) { 0 }
        } else {
            EqPresets.gainsDb(preset, bands.map { it.centerHz })
                .mapIndexed { index, db ->
                    EqPresets.clampDb(db, bands[index].minDb, bands[index].maxDb)
                }
        }
        persistGains(computed)
        return computed
    }

    private fun applyGainsLocked(gainsDb: List<Int>, bands: List<BandUi>) {
        val fx = effect ?: return
        gainsDb.forEachIndexed { index, db ->
            if (index !in bands.indices) return@forEachIndexed
            val clamped = EqPresets.clampDb(db, bands[index].minDb, bands[index].maxDb)
            runCatching { fx.setBandLevel(index.toShort(), EqPresets.dbToMillibel(clamped).toShort()) }
        }
    }

    private fun persistGains(gainsDb: List<Int>) {
        PrefsRepository.setEqGainsCsv(gainsDb.joinToString(","))
    }

    private const val SESSION_UNSET = -1
}
