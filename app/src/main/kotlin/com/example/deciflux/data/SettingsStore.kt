package com.example.deciflux.data

import android.content.Context
import com.example.deciflux.model.NormalizationConfig

/**
 * Remembers the user's normalization options between launches (Android SharedPreferences).
 *
 * The conversion between [NormalizationConfig] and plain key/value pairs ([encode] / [decode]) is pure Kotlin so it can
 * be unit-tested on the JVM. [decode] never trusts what it reads: unknown or missing values fall back to the defaults,
 * numbers are kept within sensible ranges, and text that ends up in file names or FFmpeg arguments must match a safe
 * pattern, so a corrupted or edited settings file can never break the app.
 */
object SettingsStore {

    private const val PREFS_NAME = "deciflux_settings"
    private const val FORMAT_VERSION = 1

    private const val KEY_VERSION = "format_version"
    private const val KEY_PRESET = "preset"
    private const val KEY_TARGET_LUFS = "target_lufs"
    private const val KEY_TARGET_TRUE_PEAK = "target_true_peak"
    private const val KEY_TARGET_LRA = "target_lra"
    private const val KEY_SCENE_ENABLED = "scene_leveling_enabled"
    private const val KEY_SCENE_INTENSITY = "scene_leveling_intensity"
    private const val KEY_CUSTOM_LEVELING = "custom_leveling_percentage"
    private const val KEY_AUDIO_CODEC = "audio_codec"
    private const val KEY_AUDIO_BITRATE = "audio_bitrate"
    private const val KEY_OUTPUT_SUFFIX = "output_suffix"
    private const val KEY_REPLACE_ORIGINAL = "replace_original"

    private val SAFE_CODEC = Regex("^[a-z0-9_]{1,20}$")
    private val SAFE_BITRATE = Regex("^[0-9]{2,4}k$")
    private val SAFE_SUFFIX = Regex("^[A-Za-z0-9_-]{1,40}$")

    /** The saved options, or the defaults when nothing has been saved yet. */
    fun load(context: Context): NormalizationConfig =
        decode(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).all)

    /** Saves the options in the background (SharedPreferences.apply never blocks the UI). */
    fun save(context: Context, config: NormalizationConfig) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        for ((key, value) in encode(config)) {
            when (value) {
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
            }
        }
        editor.apply()
    }

    fun encode(config: NormalizationConfig): Map<String, Any> = mapOf(
        KEY_VERSION to FORMAT_VERSION,
        KEY_PRESET to config.preset.name,
        KEY_TARGET_LUFS to config.targetLufs,
        KEY_TARGET_TRUE_PEAK to config.targetTruePeak,
        KEY_TARGET_LRA to config.targetLra,
        KEY_SCENE_ENABLED to config.sceneLevelingEnabled,
        KEY_SCENE_INTENSITY to config.sceneLevelingIntensity.name,
        KEY_CUSTOM_LEVELING to config.customLevelingPercentage,
        KEY_AUDIO_CODEC to config.audioCodec,
        KEY_AUDIO_BITRATE to config.audioBitrate,
        KEY_OUTPUT_SUFFIX to config.outputSuffix,
        KEY_REPLACE_ORIGINAL to config.replaceOriginal,
    )

    fun decode(values: Map<String, *>): NormalizationConfig {
        val defaults = NormalizationConfig()
        if (values.isEmpty()) return defaults

        fun float(key: String, fallback: Float, min: Float, max: Float): Float {
            val v = (values[key] as? Number)?.toFloat() ?: return fallback
            return if (v.isNaN()) fallback else v.coerceIn(min, max)
        }

        fun text(key: String, fallback: String, pattern: Regex): String {
            val v = values[key] as? String ?: return fallback
            return if (pattern.matches(v)) v else fallback
        }

        return NormalizationConfig(
            preset = enumOr(values[KEY_PRESET], defaults.preset),
            targetLufs = float(KEY_TARGET_LUFS, defaults.targetLufs, -70f, 0f),
            targetTruePeak = float(KEY_TARGET_TRUE_PEAK, defaults.targetTruePeak, -9f, 0f),
            targetLra = float(KEY_TARGET_LRA, defaults.targetLra, 1f, 50f),
            sceneLevelingEnabled = values[KEY_SCENE_ENABLED] as? Boolean ?: defaults.sceneLevelingEnabled,
            sceneLevelingIntensity = enumOr(values[KEY_SCENE_INTENSITY], defaults.sceneLevelingIntensity),
            customLevelingPercentage = ((values[KEY_CUSTOM_LEVELING] as? Number)?.toInt() ?: defaults.customLevelingPercentage)
                .coerceIn(0, 100),
            audioCodec = text(KEY_AUDIO_CODEC, defaults.audioCodec, SAFE_CODEC),
            audioBitrate = text(KEY_AUDIO_BITRATE, defaults.audioBitrate, SAFE_BITRATE),
            outputSuffix = text(KEY_OUTPUT_SUFFIX, defaults.outputSuffix, SAFE_SUFFIX),
            replaceOriginal = values[KEY_REPLACE_ORIGINAL] as? Boolean ?: defaults.replaceOriginal,
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(raw: Any?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == raw } ?: fallback
}
