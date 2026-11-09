package com.example.deciflux.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizationConfigTest {

    @Test
    fun presets_targetLufsValues_conformToStandards() {
        assertEquals(-14.0f, NormalizationPreset.YOUTUBE_SOCIAL.targetLufs, 0.01f)
        assertEquals(-16.0f, NormalizationPreset.PODCAST_STREAMING.targetLufs, 0.01f)
        assertEquals(-15.0f, NormalizationPreset.DIALOGUE_FOCUS.targetLufs, 0.01f)
        assertEquals(-23.0f, NormalizationPreset.BROADCAST_EBU.targetLufs, 0.01f)
    }

    @Test
    fun presets_truePeakValues_complyWithHeadroom() {
        assertEquals(-1.0f, NormalizationPreset.YOUTUBE_SOCIAL.targetTruePeak, 0.01f)
        assertEquals(-1.5f, NormalizationPreset.PODCAST_STREAMING.targetTruePeak, 0.01f)
        assertEquals(-1.0f, NormalizationPreset.DIALOGUE_FOCUS.targetTruePeak, 0.01f)
        assertEquals(-1.0f, NormalizationPreset.BROADCAST_EBU.targetTruePeak, 0.01f)
    }

    @Test
    fun sceneLevelingIntensity_parameters_properlyConfigured() {
        assertEquals(2.0f, SceneLevelingIntensity.SUBTLE.filterWindowSec, 0.01f)
        assertEquals(6.0f, SceneLevelingIntensity.SUBTLE.maxGainDb, 0.01f)

        assertEquals(1.2f, SceneLevelingIntensity.BALANCED.filterWindowSec, 0.01f)
        assertEquals(10.0f, SceneLevelingIntensity.BALANCED.maxGainDb, 0.01f)

        assertEquals(0.8f, SceneLevelingIntensity.CONSISTENT.filterWindowSec, 0.01f)
        assertEquals(14.0f, SceneLevelingIntensity.CONSISTENT.maxGainDb, 0.01f)
    }

    @Test
    fun defaultConfig_isYouTubePresetWithReasonableDefaults() {
        val config = NormalizationConfig()
        assertEquals(NormalizationPreset.YOUTUBE_SOCIAL, config.preset)
        assertEquals(-14.0f, config.targetLufs, 0.01f)
        assertEquals(-1.0f, config.targetTruePeak, 0.01f)
        assertFalse(config.replaceOriginal)
        assertEquals("_normalized", config.outputSuffix)
    }

    @Test
    fun config_toggleReplaceOriginal_preservesOtherSettings() {
        val config = NormalizationConfig(replaceOriginal = true, sceneLevelingEnabled = true)
        assertTrue(config.replaceOriginal)
        assertTrue(config.sceneLevelingEnabled)
    }
}
