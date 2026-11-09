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

        val tpMatcher = INPUT_TP_PATTERN.matcher(output)
        val inputTp = if (tpMatcher.find()) tpMatcher.group(1)?.toFloatOrNull() ?: -2.0f else -2.0f

        val lraMatcher = INPUT_LRA_PATTERN.matcher(output)
        val inputLra = if (lraMatcher.find()) lraMatcher.group(1)?.toFloatOrNull() ?: 7.0f else 7.0f

        val threshMatcher = INPUT_THRESH_PATTERN.matcher(output)
        val inputThresh = if (threshMatcher.find()) threshMatcher.group(1)?.toFloatOrNull() ?: -34.0f else -34.0f

        val offsetMatcher = TARGET_OFFSET_PATTERN.matcher(output)
        val targetOffset = if (offsetMatcher.find()) offsetMatcher.group(1)?.toFloatOrNull() ?: 0.0f else 0.0f

        return LoudnormAnalysis(
            inputI = inputI,
            inputTp = inputTp,
            inputLra = inputLra,
            inputThresh = inputThresh,
            targetOffset = targetOffset
        )
    }
}
