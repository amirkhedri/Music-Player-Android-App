package com.example.musicplayer.viewmodel

import android.media.audiofx.Equalizer
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.example.musicplayer.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class EqBand(
    val index: Short,
    val centerFreqHz: Int,
    val level: Short
)

// THE FIX: Explicitly opt-in to Media3's advanced features
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@HiltViewModel
class EqualizerViewModel @Inject constructor(
    // THE FIX: Removed 'private val' to satisfy Kotlin's memory warning
    playerController: PlayerController
) : ViewModel() {

    private val exoPlayer = playerController.exoPlayer
    private var equalizer: Equalizer? = null

    private val _isEqEnabled = MutableStateFlow(false)
    val isEqEnabled: StateFlow<Boolean> = _isEqEnabled

    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    val bands: StateFlow<List<EqBand>> = _bands

    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets

    private val _currentPreset = MutableStateFlow<String?>(null)
    val currentPreset: StateFlow<String?> = _currentPreset

    private val _minBandLevel = MutableStateFlow<Short>(0)
    val minBandLevel: StateFlow<Short> = _minBandLevel

    private val _maxBandLevel = MutableStateFlow<Short>(0)
    val maxBandLevel: StateFlow<Short> = _maxBandLevel

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0) {
                    setupEqualizer(audioSessionId)
                }
            }
        })

        if (exoPlayer.audioSessionId != 0) {
            setupEqualizer(exoPlayer.audioSessionId)
        }
    }

    private fun setupEqualizer(sessionId: Int) {
        try {
            equalizer?.release()

            equalizer = Equalizer(0, sessionId).apply {
                enabled = _isEqEnabled.value
            }

            equalizer?.let { eq ->
                val bandRange = eq.bandLevelRange
                _minBandLevel.value = bandRange[0]
                _maxBandLevel.value = bandRange[1]

                val bandList = mutableListOf<EqBand>()
                for (i in 0 until eq.numberOfBands) {
                    bandList.add(
                        EqBand(
                            index = i.toShort(),
                            centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                            level = eq.getBandLevel(i.toShort())
                        )
                    )
                }
                _bands.value = bandList

                val presetList = mutableListOf<String>()
                for (i in 0 until eq.numberOfPresets) {
                    presetList.add(eq.getPresetName(i.toShort()))
                }
                _presets.value = presetList

                if (presetList.isNotEmpty() && _currentPreset.value == null) {
                    _currentPreset.value = presetList[0]
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        equalizer?.enabled = enabled
        _isEqEnabled.value = enabled
    }

    fun setBandLevel(bandIndex: Short, level: Short) {
        equalizer?.setBandLevel(bandIndex, level)
        _bands.value = _bands.value.map {
            if (it.index == bandIndex) it.copy(level = level) else it
        }
        _currentPreset.value = "Custom"
    }

    fun setPreset(presetName: String) {
        val eq = equalizer ?: return
        val presetIndex = _presets.value.indexOf(presetName)
        if (presetIndex != -1) {
            eq.usePreset(presetIndex.toShort())
            _currentPreset.value = presetName

            val updatedBands = _bands.value.map { band ->
                band.copy(level = eq.getBandLevel(band.index))
            }
            _bands.value = updatedBands
        }
    }

    override fun onCleared() {
        super.onCleared()
        equalizer?.release()
    }
}