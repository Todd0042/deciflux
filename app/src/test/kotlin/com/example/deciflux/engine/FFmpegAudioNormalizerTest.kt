package com.example.deciflux.engine

import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.NormalizationPreset
import com.example.deciflux.model.SceneLevelingIntensity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FFmpegAudioNormalizerTest {

    private val sampleAnalysis = LoudnormAnalysis(
        inputI = -26.4f,
        inputTp = -3.2f,
        inputLra = 14.8f,
        inputThresh = -37.1f,
        targetOffset = 0.5f
    )

    @Test
    fun buildFilterChain_standardEbuR128_buildsLinearLoudnormChain() {
        val config = NormalizationConfig(
            preset = NormalizationPreset.YOUTUBE_SOCIAL,
            targetLufs = -14.0f,
            targetTruePeak = -1.0f,
            targetLra = 11.0f,
            sceneLevelingEnabled = false
        )

        val filter = FFmpegAudioNormalizer.buildFilterChain(config, sampleAnalysis)

        assertTrue(filter.startsWith("loudnorm=I=-14.0:TP=-1.0:LRA=11.0"))
        assertTrue(filter.contains("measured_I=-26.4"))
        assertTrue(filter.contains("measured_TP=-3.2"))
        assertTrue(filter.contains("measured_LRA=14.8"))
        assertTrue(filter.contains("measured_thresh=-37.1"))
        assertTrue(filter.contains("offset=0.5"))
        assertTrue(filter.contains("linear=true"))
        assertFalse(filter.contains("dynaudnorm"))
    }

    @Test
    fun buildFilterChain_sceneLevelingSubtle_prependsDynaudnormFilter() {
        val config = NormalizationConfig(
            preset = NormalizationPreset.DIALOGUE_FOCUS,
            targetLufs = -15.0f,
            targetTruePeak = -1.5f,
            targetLra = 9.0f,
            sceneLevelingEnabled = true,
            sceneLevelingIntensity = SceneLevelingIntensity.SUBTLE
        )

        val filter = FFmpegAudioNormalizer.buildFilterChain(config, sampleAnalysis)

        assertTrue(filter.contains("dynaudnorm=f=2000:g=45:m=6.0:p=0.95:r=0.6"))
        assertTrue(filter.contains("loudnorm=I=-15.0:TP=-1.5:LRA=9.0"))
    }

    @Test
    fun buildFilterChain_sceneLevelingConsistent_usesHigherGainAndShorterWindow() {
        val config = NormalizationConfig(
            preset = NormalizationPreset.PODCAST_STREAMING,
            targetLufs = -16.0f,
            targetTruePeak = -1.5f,
            targetLra = 10.0f,
            sceneLevelingEnabled = true,
            sceneLevelingIntensity = SceneLevelingIntensity.CONSISTENT
        )

        val filter = FFmpegAudioNormalizer.buildFilterChain(config, sampleAnalysis)

        assertTrue(filter.contains("dynaudnorm=f=800:g=21:m=14.0:p=0.95:r=0.6"))
        assertTrue(filter.contains("loudnorm=I=-16.0:TP=-1.5:LRA=10.0"))
    }
}
