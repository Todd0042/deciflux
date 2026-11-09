package com.example.deciflux.engine

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.deciflux.model.LogTag
import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.SceneLevelingIntensity
import com.example.deciflux.model.TerminalLogEntry
import com.example.deciflux.util.MediaUriHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern

data class LoudnormAnalysis(
    val inputI: Float,
    val inputTp: Float,
    val inputLra: Float,
    val inputThresh: Float,
    val targetOffset: Float
)

class FFmpegAudioNormalizer(private val context: Context) {

    private val ffmpegBinary: File
    private val ffmpegLibsDir: File
    private val nativeLibDir: File

    init {
        nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
        ffmpegBinary = File(nativeLibDir, "libffmpeg.so")
        ffmpegLibsDir = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
    }

    suspend fun normalizeVideo(
        inputUri: android.net.Uri,
        durationMs: Long,
        config: NormalizationConfig,
        tempOutputFile: File,
        onLog: (TerminalLogEntry) -> Unit,
        onProgress: (passNumber: Int, progress: Float, speed: String, eta: String) -> Unit
    ): Result<LoudnormAnalysis> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        try {
            val (fd, inputPath) = MediaUriHelper.openReadableFd(context, inputUri)
            pfd = fd
            if (inputPath.isEmpty()) {
                val errorMsg = "Unable to open input file descriptor for $inputUri"
                onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            onLog(TerminalLogEntry(tag = LogTag.INFO, message = "Probing input stream: $inputPath"))
            if (!ffmpegBinary.exists() || !ffmpegLibsDir.exists()) {
                try {
                    com.yausername.ffmpeg.FFmpeg.getInstance().init(context)
                } catch (t: Throwable) {
                    Log.w(TAG, "On-demand FFmpeg init encountered: ${t.message}")
                }
            }

            if (!ffmpegBinary.exists()) {
                val errorMsg = "FFmpeg binary not found at ${ffmpegBinary.absolutePath}. Please check device ABI."
                onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            // -------------------------------------------------------------------------
            // PASS 1: Loudness & Dynamic Analysis
            // -------------------------------------------------------------------------
            onLog(TerminalLogEntry(tag = LogTag.ANALYSIS, message = "Pass 1/2: Analyzing integrated loudness (EBU R128)..."))

            val pass1Filter = "loudnorm=I=${config.targetLufs}:TP=${config.targetTruePeak}:LRA=${config.targetLra}:print_format=json"
            val pass1Cmd = listOf(
                ffmpegBinary.absolutePath,
                "-hide_banner",
                "-nostats",
                "-i", inputPath,
                "-af", pass1Filter,
                "-vn",
                "-sn",
                "-dn",
                "-f", "null",
                "-"
            )

            val pass1Output = StringBuilder()
            val pass1ExitCode = executeCommand(
                command = pass1Cmd,
                onLine = { line ->
                    pass1Output.appendLine(line)
                    if (line.contains("Parsed_loudnorm") || line.contains("loudness")) {
                        onLog(TerminalLogEntry(tag = LogTag.ANALYSIS, message = line.trim()))
                    }
                }
            )

            if (pass1ExitCode != 0) {
                val errorMsg = "Pass 1 analysis failed with exit code $pass1ExitCode"
                onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
                return@withContext Result.failure(RuntimeException(errorMsg))
            }

            val analysis = parseLoudnormJson(pass1Output.toString())
            if (analysis == null) {
                val errorMsg = "Failed to parse Pass 1 loudness measurement JSON"
                onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            onLog(
                TerminalLogEntry(
                    tag = LogTag.SUCCESS,
                    message = String.format(
                        "Pass 1 Complete: Input = %.1f LUFS | Peak = %.1f dBTP | LRA = %.1f LU",
                        analysis.inputI, analysis.inputTp, analysis.inputLra
                    )
                )
            )

            // -------------------------------------------------------------------------
            // SCENE LEVELING CONFIGURATION (If enabled)
            // -------------------------------------------------------------------------
            val filterChain = buildFilterChain(config, analysis, onLog)

            // -------------------------------------------------------------------------
            // PASS 2: Audio Re-encode with Lossless Video Bitstream Copy
            // -------------------------------------------------------------------------
            onLog(
                TerminalLogEntry(
                    tag = LogTag.REMUX,
                    message = "Pass 2/2: Remuxing with lossless video passthrough (-c:v copy) & ${config.audioCodec} @ ${config.audioBitrate}..."
                )
            )

            if (tempOutputFile.exists()) {
                tempOutputFile.delete()
            }

            val pass2Cmd = listOf(
                ffmpegBinary.absolutePath,
                "-y",
                "-hide_banner",
                "-i", inputPath,
                "-c:v", "copy",
                "-af", filterChain,
                "-c:a", config.audioCodec,
                "-b:a", config.audioBitrate,
                "-avoid_negative_ts", "make_zero",
                "-max_muxing_queue_size", "2048",
                tempOutputFile.absolutePath
            )

            val timePattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.\\d+)")
            val speedPattern = Pattern.compile("speed=\\s*([\\d\\.]+)x")

            val pass2ExitCode = executeCommand(
                command = pass2Cmd,
                onLine = { line ->
                    // Parse live progress telemetry from FFmpeg status lines
                    val timeMatcher = timePattern.matcher(line)
                    val speedMatcher = speedPattern.matcher(line)

                    if (timeMatcher.find()) {
                        val hours = timeMatcher.group(1)?.toLongOrNull() ?: 0L
                        val mins = timeMatcher.group(2)?.toLongOrNull() ?: 0L
                        val secs = timeMatcher.group(3)?.toFloatOrNull() ?: 0f
                        val currentTimeMs = ((hours * 3600 + mins * 60 + secs) * 1000).toLong()

                        val progress = if (durationMs > 0) {
                            (currentTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0.0f, 1.0f)
                        } else 0.5f

                        val speedStr = if (speedMatcher.find()) {
                            "${speedMatcher.group(1)}x"
                        } else "--"

                        val etaStr = calculateEta(currentTimeMs, durationMs, speedMatcher)
                        onProgress(2, progress, speedStr, etaStr)
                    }

                    if (line.contains("error", ignoreCase = true) || line.contains("warning", ignoreCase = true)) {
                        onLog(TerminalLogEntry(tag = LogTag.WARN, message = line.trim()))
                    } else if (line.contains("frame=") || line.contains("size=")) {
                        onLog(TerminalLogEntry(tag = LogTag.REMUX, message = line.trim()))
                    }
                }
            )

            if (pass2ExitCode != 0) {
                val errorMsg = "Pass 2 remux failed with exit code $pass2ExitCode"
                onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
                return@withContext Result.failure(RuntimeException(errorMsg))
            }

            if (!tempOutputFile.exists() || tempOutputFile.length() <= 0L) {
                val errorMsg = "Output file was not created or is empty"
                onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            onLog(
                TerminalLogEntry(
                    tag = LogTag.SUCCESS,
                    message = String.format("Normalization complete! Output size: %.2f MB", tempOutputFile.length() / (1024.0 * 1024.0))
                )
            )

            Result.success(analysis)
        } catch (e: Exception) {
            onLog(TerminalLogEntry(tag = LogTag.ERROR, message = "Exception: ${e.localizedMessage}"))
            Result.failure(e)
        } finally {
            try {
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    private fun buildFilterChain(
        config: NormalizationConfig,
        analysis: LoudnormAnalysis,
        onLog: (TerminalLogEntry) -> Unit
    ): String {
        return if (config.sceneLevelingEnabled) {
            val intensity = config.sceneLevelingIntensity
            val frameLenMs = (intensity.filterWindowSec * 1000).toInt()
            val gaussSize = when (intensity) {
                SceneLevelingIntensity.SUBTLE -> 45
                SceneLevelingIntensity.BALANCED -> 31
                SceneLevelingIntensity.CONSISTENT -> 21
                SceneLevelingIntensity.CUSTOM -> 31
            }
            val maxGain = intensity.maxGainDb

            onLog(
                TerminalLogEntry(
                    tag = LogTag.SCENE,
                    message = "Applying Scene-Aware Dynamic Leveling [${intensity.label}]: Window=${frameLenMs}ms, MaxGain=+${maxGain}dB, Smoothing Gauss=$gaussSize"
                )
            )

            // Dynamic Audio Normalizer with Gaussian smoothing, followed by linear loudnorm target normalization
            "dynaudnorm=f=$frameLenMs:g=$gaussSize:m=$maxGain:p=0.95:r=0.6,loudnorm=I=${config.targetLufs}:TP=${config.targetTruePeak}:LRA=${config.targetLra}:linear=true"
        } else {
            // Standard 2-Pass EBU R128 Loudnorm Filter
            "loudnorm=I=${config.targetLufs}:TP=${config.targetTruePeak}:LRA=${config.targetLra}:measured_I=${analysis.inputI}:measured_TP=${analysis.inputTp}:measured_LRA=${analysis.inputLra}:measured_thresh=${analysis.inputThresh}:offset=${analysis.targetOffset}:linear=true"
        }
    }

    private fun executeCommand(
        command: List<String>,
        onLine: (String) -> Unit
    ): Int {
        val pb = ProcessBuilder(command)
        pb.redirectErrorStream(true)

        val env = pb.environment()
        val ldPath = buildString {
            if (ffmpegLibsDir.exists()) append(ffmpegLibsDir.absolutePath).append(":")
            append(nativeLibDir.absolutePath)
        }
        env["LD_LIBRARY_PATH"] = ldPath

        val process = pb.start()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { onLine(it) }
            }
        }
        return process.waitFor()
    }

    private fun parseLoudnormJson(output: String): LoudnormAnalysis? {
        val startIdx = output.lastIndexOf('{')
        val endIdx = output.lastIndexOf('}')
        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) return null

        val jsonStr = output.substring(startIdx, endIdx + 1)
        return try {
            val json = JSONObject(jsonStr)
            LoudnormAnalysis(
                inputI = json.optDouble("input_i", -24.0).toFloat(),
                inputTp = json.optDouble("input_tp", -2.0).toFloat(),
                inputLra = json.optDouble("input_lra", 7.0).toFloat(),
                inputThresh = json.optDouble("input_thresh", -34.0).toFloat(),
                targetOffset = json.optDouble("target_offset", 0.0).toFloat()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing loudnorm JSON", e)
            null
        }
    }

    private fun calculateEta(currentMs: Long, totalMs: Long, speedMatcher: java.util.regex.Matcher): String {
        if (totalMs <= 0 || currentMs <= 0) return "--:--"
        val remainingMs = (totalMs - currentMs).coerceAtLeast(0)
        val speed = if (speedMatcher.find()) speedMatcher.group(1)?.toFloatOrNull() ?: 1.0f else 1.0f
        if (speed <= 0.01f) return "--:--"

        val remainingSecs = (remainingMs / (1000f * speed)).toLong()
        val mins = remainingSecs / 60
        val secs = remainingSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }

    companion object {
        private const val TAG = "FFmpegNormalizer"
    }
}
