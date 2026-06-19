package com.example.musicplayer.viewmodel

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.example.musicplayer.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class EqBand(
    val index: Short,
    val centerFreqHz: Int,
    val level: Short
)

// ------------------------------------------------------------------
// 1. THE MANAGER (Survives screen changes & saves to device memory)
// ------------------------------------------------------------------
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Singleton
class EqualizerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerController: PlayerController
) {
    // SharedPreferences saves the user's settings permanently
    private val prefs = context.getSharedPreferences("equalizer_prefs", Context.MODE_PRIVATE)

    private val exoPlayer = playerController.exoPlayer
    private var equalizer: Equalizer? = null

    // Load initial states directly from phone memory
    val isEqEnabled = MutableStateFlow(prefs.getBoolean("eq_enabled", false))
    val bands = MutableStateFlow<List<EqBand>>(emptyList())
    val presets = MutableStateFlow<List<String>>(emptyList())
    val currentPreset = MutableStateFlow<String?>(prefs.getString("eq_preset", null))

    val minBandLevel = MutableStateFlow<Short>(0)
    val maxBandLevel = MutableStateFlow<Short>(0)

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
                enabled = isEqEnabled.value
            }

            equalizer?.let { eq ->
                val bandRange = eq.bandLevelRange
                minBandLevel.value = bandRange[0]
                maxBandLevel.value = bandRange[1]

                val bandList = mutableListOf<EqBand>()

                // Load hardware bands and apply saved levels
                for (i in 0 until eq.numberOfBands) {
                    val defaultLevel = eq.getBandLevel(i.toShort())
                    // Read saved level from Prefs, fallback to hardware default
                    val savedLevel = prefs.getInt("band_$i", defaultLevel.toInt()).toShort()

                    eq.setBandLevel(i.toShort(), savedLevel)

                    bandList.add(
                        EqBand(
                            index = i.toShort(),
                            centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                            level = savedLevel
                        )
                    )
                }
                bands.value = bandList

                val presetList = mutableListOf<String>()
                for (i in 0 until eq.numberOfPresets) {
                    presetList.add(eq.getPresetName(i.toShort()))
                }
                presets.value = presetList

                // Restore the saved preset name
                val savedPreset = prefs.getString("eq_preset", null)
                if (savedPreset != null && savedPreset != "Custom" && presetList.contains(savedPreset)) {
                    // We don't call setPreset() here because we already loaded the individual band levels
                    currentPreset.value = savedPreset
                } else if (savedPreset == null && presetList.isNotEmpty()) {
                    currentPreset.value = presetList[0]
                } else {
                    currentPreset.value = savedPreset ?: "Custom"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        equalizer?.enabled = enabled
        isEqEnabled.value = enabled
        prefs.edit().putBoolean("eq_enabled", enabled).apply()
    }

    fun setBandLevel(bandIndex: Short, level: Short) {
        equalizer?.setBandLevel(bandIndex, level)

        bands.value = bands.value.map {
            if (it.index == bandIndex) it.copy(level = level) else it
        }
        currentPreset.value = "Custom"

        // Save new band level and custom state to memory
        prefs.edit()
            .putInt("band_$bandIndex", level.toInt())
            .putString("eq_preset", "Custom")
            .apply()
    }

    fun setPreset(presetName: String) {
        val eq = equalizer ?: return
        val presetIndex = presets.value.indexOf(presetName)
        if (presetIndex != -1) {
            eq.usePreset(presetIndex.toShort())
            currentPreset.value = presetName

            val editor = prefs.edit()
            editor.putString("eq_preset", presetName)

            val updatedBands = bands.value.map { band ->
                val newLevel = eq.getBandLevel(band.index)
                editor.putInt("band_${band.index}", newLevel.toInt())
                band.copy(level = newLevel)
            }

            editor.apply() // Commit all changes to memory at once
            bands.value = updatedBands
        }
    }
}

// ------------------------------------------------------------------
// 2. THE VIEWMODEL (Just acts as a bridge for your Compose UI)
// ------------------------------------------------------------------
@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerManager: EqualizerManager
) : ViewModel() {

    // Mirror the StateFlows from the Manager so the UI can read them
    val isEqEnabled: StateFlow<Boolean> = equalizerManager.isEqEnabled
    val bands: StateFlow<List<EqBand>> = equalizerManager.bands
    val presets: StateFlow<List<String>> = equalizerManager.presets
    val currentPreset: StateFlow<String?> = equalizerManager.currentPreset
    val minBandLevel: StateFlow<Short> = equalizerManager.minBandLevel
    val maxBandLevel: StateFlow<Short> = equalizerManager.maxBandLevel

    // Pass UI actions directly to the Manager
    fun toggleEqualizer(enabled: Boolean) = equalizerManager.toggleEqualizer(enabled)
    fun setBandLevel(bandIndex: Short, level: Short) = equalizerManager.setBandLevel(bandIndex, level)
    fun setPreset(presetName: String) = equalizerManager.setPreset(presetName)
}