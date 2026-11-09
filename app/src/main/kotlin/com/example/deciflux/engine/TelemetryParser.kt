package com.example.deciflux.engine

import java.util.regex.Pattern

/**
 * Parsed real-time progress telemetry from FFmpeg status lines.
 */
data class TelemetryProgress(
    val currentTimeMs: Long,
    val progressFraction: Float,
    val speedMultiplier: String,
    val etaFormatted: String
)

/**
 * Parser for real-time FFmpeg progress status lines (time=..., speed=..., fps=...).
 */
object TelemetryParser {
    private val TIME_PATTERN = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)")
    private val SPEED_PATTERN = Pattern.compile("speed=\\s*([\\d\\.]+)x")

    /**
     * Extracts playback position in milliseconds from an FFmpeg status line.
     */
    fun parseTimeMs(line: String): Long? {
        val matcher = TIME_PATTERN.matcher(line)
        if (!matcher.find()) return null

        val hours = matcher.group(1)?.toLongOrNull() ?: 0L
        val mins = matcher.group(2)?.toLongOrNull() ?: 0L
        val secs = matcher.group(3)?.toFloatOrNull() ?: 0f
        return ((hours * 3600 + mins * 60 + secs) * 1000).toLong()
    }

    /**
     * Extracts the processing speed multiplier string (e.g. "28.5x").
     */
    fun parseSpeed(line: String): String {
        val matcher = SPEED_PATTERN.matcher(line)
        return if (matcher.find()) {
            "${matcher.group(1)}x"
        } else {
            "--"
        }
    }

    /**
     * Calculates the estimated time remaining based on remaining milliseconds and processing speed.
     */
    fun calculateEta(currentMs: Long, totalMs: Long, line: String): String {
        if (totalMs <= 0 || currentMs <= 0) return "--:--"
        val remainingMs = (totalMs - currentMs).coerceAtLeast(0)

        val matcher = SPEED_PATTERN.matcher(line)
        val speed = if (matcher.find()) matcher.group(1)?.toFloatOrNull() ?: 1.0f else 1.0f
        if (speed <= 0.01f) return "--:--"

        val remainingSecs = (remainingMs / (1000f * speed)).toLong()
        val mins = remainingSecs / 60
        val secs = remainingSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    /**
     * Parses a complete telemetry update from an FFmpeg line if a time token is present.
     */
    fun parseProgress(line: String, totalDurationMs: Long): TelemetryProgress? {
        val currentMs = parseTimeMs(line) ?: return null
        val progress = if (totalDurationMs > 0) {
            (currentMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
        } else {
            0.5f
        }
        val speedStr = parseSpeed(line)
        val etaStr = calculateEta(currentMs, totalDurationMs, line)

        return TelemetryProgress(
            currentTimeMs = currentMs,
            progressFraction = progress,
            speedMultiplier = speedStr,
            etaFormatted = etaStr
        )
    }
}
