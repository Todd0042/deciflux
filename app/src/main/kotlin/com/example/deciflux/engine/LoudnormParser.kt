package com.example.deciflux.engine

import java.util.regex.Pattern

/**
 * Parsed measurement values from FFmpeg's loudnorm filter in pass 1.
 */
data class LoudnormAnalysis(
    val inputI: Float,
    val inputTp: Float,
    val inputLra: Float,
    val inputThresh: Float,
    val targetOffset: Float
)

/**
 * High-performance parser for extracting EBU R128 / loudnorm JSON telemetry blocks.
 * Uses robust regex extraction without dependency on platform JSON stubs.
 */
object LoudnormParser {
    private val INPUT_I_PATTERN = Pattern.compile("\"input_i\"\\s*:\\s*\"?([-\\d.]+)\"?")
    private val INPUT_TP_PATTERN = Pattern.compile("\"input_tp\"\\s*:\\s*\"?([-\\d.]+)\"?")
    private val INPUT_LRA_PATTERN = Pattern.compile("\"input_lra\"\\s*:\\s*\"?([-\\d.]+)\"?")
    private val INPUT_THRESH_PATTERN = Pattern.compile("\"input_thresh\"\\s*:\\s*\"?([-\\d.]+)\"?")
    private val TARGET_OFFSET_PATTERN = Pattern.compile("\"target_offset\"\\s*:\\s*\"?([-\\d.]+)\"?")

    private fun extractMetric(pattern: Pattern, text: String, fallback: Float): Float {
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1)?.toFloatOrNull() ?: fallback else fallback
    }

    /**
     * Parses the Pass 1 FFmpeg stdout/stderr buffer to locate and extract the loudnorm metrics.
     * Returns null if no valid measurement block was found.
     */
    fun parseLoudnormJson(output: String): LoudnormAnalysis? {
        val startBrace = output.lastIndexOf('{')
        val endBrace = output.lastIndexOf('}')
        if (startBrace == -1 || endBrace == -1 || endBrace <= startBrace) return null

        val jsonBlock = output.substring(startBrace, endBrace + 1)
        val iMatcher = INPUT_I_PATTERN.matcher(jsonBlock)
        if (!iMatcher.find()) return null
        val inputI = iMatcher.group(1)?.toFloatOrNull() ?: return null

        return LoudnormAnalysis(
            inputI = inputI,
            inputTp = extractMetric(INPUT_TP_PATTERN, jsonBlock, -2.0f),
            inputLra = extractMetric(INPUT_LRA_PATTERN, jsonBlock, 7.0f),
            inputThresh = extractMetric(INPUT_THRESH_PATTERN, jsonBlock, -34.0f),
            targetOffset = extractMetric(TARGET_OFFSET_PATTERN, jsonBlock, 0.0f)
        )
    }
}
