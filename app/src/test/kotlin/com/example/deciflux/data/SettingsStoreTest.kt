package com.example.deciflux.data

import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.NormalizationPreset
import com.example.deciflux.model.SceneLevelingIntensity
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStoreTest {

    @Test
    fun nothingSaved_givesDefaults() {
        assertEquals(NormalizationConfig(), SettingsStore.decode(emptyMap<String, Any>()))
    }

    @Test
    fun savedOptions_comeBackExactly() {
        val custom = NormalizationConfig(
            preset = NormalizationPreset.CUSTOM,
            targetLufs = -18.5f,
            targetTruePeak = -2.0f,
            targetLra = 6.0f,
            sceneLevelingEnabled = true,
            sceneLevelingIntensity = SceneLevelingIntensity.CUSTOM,
            customLevelingPercentage = 72,
            audioCodec = "libopus",
            audioBitrate = "160k",
            outputSuffix = "_leveled",
            replaceOriginal = true,
        )
        assertEquals(custom, SettingsStore.decode(SettingsStore.encode(custom)))
    }

    @Test
    fun everyPresetAndIntensity_survivesSaving() {
        for (preset in NormalizationPreset.entries) {
            for (intensity in SceneLevelingIntensity.entries) {
                val config = NormalizationConfig.fromPreset(preset).copy(sceneLevelingIntensity = intensity)
                assertEquals(config, SettingsStore.decode(SettingsStore.encode(config)))
            }
        }
    }

    @Test
    fun unknownOrDamagedValues_fallBackSafely() {
        val defaults = NormalizationConfig()
        val damaged = mapOf(
            "preset" to "NOT_A_PRESET",
            "scene_leveling_intensity" to 42,
            "target_lufs" to Float.NaN,
            "target_true_peak" to 7.0f,          // above 0 dBTP is impossible: clamped
            "custom_leveling_percentage" to 250, // clamped to 100
            "audio_codec" to "aac; rm -rf /",    // unsafe text: default
            "audio_bitrate" to "lots",
            "output_suffix" to "../../evil",
            "replace_original" to "yes",         // wrong type: default
        )
        val out = SettingsStore.decode(damaged)
        assertEquals(defaults.preset, out.preset)
        assertEquals(defaults.sceneLevelingIntensity, out.sceneLevelingIntensity)
        assertEquals(defaults.targetLufs, out.targetLufs, 0.0f)
        assertEquals(0.0f, out.targetTruePeak, 0.0f)
        assertEquals(100, out.customLevelingPercentage)
        assertEquals(defaults.audioCodec, out.audioCodec)
        assertEquals(defaults.audioBitrate, out.audioBitrate)
        assertEquals(defaults.outputSuffix, out.outputSuffix)
        assertEquals(defaults.replaceOriginal, out.replaceOriginal)
    }
}
