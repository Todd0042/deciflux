package com.example.deciflux.model

enum class NormalizationPreset(
    val title: String,
    val description: String,
    val targetLufs: Float,
    val targetTruePeak: Float,
    val targetLra: Float
) {
    YOUTUBE_SOCIAL(
        title = "YouTube & Social",
        description = "-14 LUFS • Modern standard for web, YouTube, and mobile clips",
        targetLufs = -14.0f,
        targetTruePeak = -1.0f,
        targetLra = 11.0f
    ),
    PODCAST_STREAMING(
        title = "Streaming & Podcasts",
        description = "-16 LUFS • Apple, Spotify, and spoken word standard",
        targetLufs = -16.0f,
        targetTruePeak = -1.5f,
        targetLra = 9.0f
    ),
    BROADCAST_EBU(
        title = "Broadcast (EBU R128)",
        description = "-23 LUFS • European broadcast & television standard",
        targetLufs = -23.0f,
        targetTruePeak = -1.0f,
        targetLra = 7.0f
    ),
    DIALOGUE_FOCUS(
        title = "Dialogue Boost",
        description = "-15 LUFS • Enhanced vocal clarity for speech & tutorials",
        targetLufs = -15.0f,
        targetTruePeak = -1.0f,
        targetLra = 8.0f
    ),
    CUSTOM(
        title = "Custom Tuner",
        description = "User-defined target LUFS, True Peak, and Loudness Range",
        targetLufs = -14.0f,
        targetTruePeak = -1.0f,
        targetLra = 11.0f
    )
}

enum class SceneLevelingIntensity(
    val label: String,
    val description: String,
    val levelingPercentage: Int, // e.g. 30% leveling = 70% contrast kept
    val maxGainDb: Float,
    val filterWindowSec: Float
) {
    SUBTLE(
        label = "Subtle",
        description = "Gently softens abrupt jumps while preserving clear scene dynamics",
        levelingPercentage = 30,
        maxGainDb = 6.0f,
        filterWindowSec = 2.0f
    ),
    BALANCED(
        label = "Balanced (Default)",
        description = "Comfortable listening volume with natural, slight scene contrast",
        levelingPercentage = 55,
        maxGainDb = 10.0f,
        filterWindowSec = 1.2f
    ),
    CONSISTENT(
        label = "Consistent",
        description = "Uniform volume throughout; brings quiet speech up close to loud scenes",
        levelingPercentage = 85,
        maxGainDb = 14.0f,
        filterWindowSec = 0.8f
    ),
    CUSTOM(
        label = "Custom...",
        description = "Granular adjustment of leveling percentage and gain limits",
        levelingPercentage = 55,
        maxGainDb = 10.0f,
        filterWindowSec = 1.2f
    )
}

data class NormalizationConfig(
    val preset: NormalizationPreset = NormalizationPreset.YOUTUBE_SOCIAL,
    val targetLufs: Float = -14.0f,
    val targetTruePeak: Float = -1.0f,
    val targetLra: Float = 11.0f,
    val sceneLevelingEnabled: Boolean = false,
    val sceneLevelingIntensity: SceneLevelingIntensity = SceneLevelingIntensity.BALANCED,
    val customLevelingPercentage: Int = 55,
    val audioCodec: String = "aac",
    val audioBitrate: String = "192k",
    val outputSuffix: String = "_normalized",
    val replaceOriginal: Boolean = false
) {
    companion object {
        fun fromPreset(preset: NormalizationPreset): NormalizationConfig {
            return NormalizationConfig(
                preset = preset,
                targetLufs = preset.targetLufs,
                targetTruePeak = preset.targetTruePeak,
                targetLra = preset.targetLra
            )
        }
    }
}
