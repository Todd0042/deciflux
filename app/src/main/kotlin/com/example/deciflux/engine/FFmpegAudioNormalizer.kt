package com.example.deciflux.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.deciflux.model.LogTag
import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.SceneLevelingIntensity
import com.example.deciflux.model.TerminalLogEntry
import com.example.deciflux.util.MediaUriHelper
import com.example.deciflux.util.SecuritySanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * High-performance offline audio normalizer powered by bundled native FFmpeg.
 * Implements two-pass ITU-R BS.1770 / EBU R128 loudness normalization and
 * scene-aware dynamic audio leveling with lossless video passthrough (-c:v copy).
 */
class FFmpegAudioNormalizer(private val context: Context) {

    private val ffmpegBinary: File by lazy {
        File(context.filesDir.parentFile, "no_backup/youtubedl-android/packages/ffmpeg/usr/bin/ffmpeg")
    }

    private val ffmpegLibsDir: File by lazy {
        File(context.filesDir.parentFile, "no_backup/youtubedl-android/packages/ffmpeg/usr/lib")
    }

    private val pythonLibsDir: File by lazy {
        File(context.filesDir.parentFile, "no_backup/youtubedl-android/packages/python/usr/lib")
    }

    private val nativeLibDir: File by lazy {
        File(context.applicationInfo.nativeLibraryDir)
    }

    @Volatile
    private var currentProcess: Process? = null

    /**
     * Immediately terminates any active FFmpeg subprocess.
     */
    fun cancelCurrentExecution() {
        try {
            currentProcess?.destroyForcibly()
        } catch (e: Exception) {
            Log.w(TAG, "Error while terminating FFmpeg process forcibly", e)
        }
        currentProcess = null
    }

    /**
     * Orchestrates the 2-pass normalization workflow for a single video file.
     */
    suspend fun normalizeVideo(
        inputUri: Uri,
        durationMs: Long,
        config: NormalizationConfig,
        tempOutputFile: File,
        onLog: (TerminalLogEntry) -> Unit,
        onProgress: (passNumber: Int, progress: Float, speed: String, eta: String) -> Unit
    ): Result<LoudnormAnalysis> = withContext(Dispatchers.IO) {
        var cachedInputFile: File? = null
        try {
            cachedInputFile = prepareInputStream(inputUri, onLog)
                ?: return@withContext Result.failure(IllegalStateException("Unable to read input media from $inputUri"))

            if (!ensureNativeBinaries(onLog)) {
                return@withContext Result.failure(IllegalStateException("FFmpeg native engine initialization failed"))
            }

            val analysisResult = executePass1Analysis(cachedInputFile.absolutePath, config, onLog)
            if (analysisResult.isFailure) {
                return@withContext Result.failure(analysisResult.exceptionOrNull()!!)
            }
            val analysis = analysisResult.getOrThrow()

            val filterChain = buildFilterChain(config, analysis, onLog)
            val remuxResult = executePass2Remux(
                inputPath = cachedInputFile.absolutePath,
                tempOutputFile = tempOutputFile,
                filterChain = filterChain,
                config = config,
                durationMs = durationMs,
                onLog = onLog,
                onProgress = onProgress
            )
            if (remuxResult.isFailure) {
                return@withContext Result.failure(remuxResult.exceptionOrNull()!!)
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
                cachedInputFile?.delete()
            } catch (_: Exception) {}
        }
    }

    private fun prepareInputStream(inputUri: Uri, onLog: (TerminalLogEntry) -> Unit): File? {
        onLog(TerminalLogEntry(tag = LogTag.INFO, message = "Preparing input stream from URI..."))
        val cached = MediaUriHelper.prepareInputFile(context, inputUri)
        if (cached == null || !cached.exists() || cached.length() <= 0L) {
            onLog(TerminalLogEntry(tag = LogTag.ERROR, message = "Unable to read input media from $inputUri"))
            return null
        }
        val inputSizeMb = cached.length() / (1024.0 * 1024.0)
        onLog(TerminalLogEntry(tag = LogTag.INFO, message = String.format("Input stream ready: %.2f MB", inputSizeMb)))
        return cached
    }

    private fun ensureNativeBinaries(onLog: (TerminalLogEntry) -> Unit): Boolean {
        if (!ffmpegBinary.exists() || !ffmpegLibsDir.exists() || !pythonLibsDir.exists()) {
            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().init(context)
                com.yausername.ffmpeg.FFmpeg.getInstance().init(context)
            } catch (t: Throwable) {
                Log.w(TAG, "On-demand FFmpeg/packages init: ${t.message}")
            }
        }
        if (!ffmpegBinary.exists()) {
            val errorMsg = "FFmpeg binary not found at ${ffmpegBinary.absolutePath}. Please check device ABI."
            onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
            return false
        }
        return true
    }

    private fun executePass1Analysis(
        inputPath: String,
        config: NormalizationConfig,
        onLog: (TerminalLogEntry) -> Unit
    ): Result<LoudnormAnalysis> {
        onLog(TerminalLogEntry(tag = LogTag.ANALYSIS, message = "Pass 1/2: Analyzing integrated loudness (EBU R128)..."))
        val pass1Filter = "loudnorm=I=${config.targetLufs}:TP=${config.targetTruePeak}:LRA=${config.targetLra}:print_format=json"
        val pass1Cmd = listOf(
            ffmpegBinary.absolutePath,
            "-hide_banner",
            "-nostats",
            "-i", inputPath,
            "-af", pass1Filter,
            "-vn", "-sn", "-dn",
            "-f", "null", "-"
        )

        val pass1Output = StringBuilder()
        val pass1RecentLines = ArrayDeque<String>(25)

        val exitCode = executeCommand(pass1Cmd) { line ->
            pass1Output.appendLine(line)
            synchronized(pass1RecentLines) {
                if (pass1RecentLines.size >= 20) pass1RecentLines.removeFirst()
                pass1RecentLines.addLast(line)
            }
            if (line.contains("Parsed_loudnorm") || line.contains("loudness") || line.contains("error", ignoreCase = true)) {
                onLog(TerminalLogEntry(tag = LogTag.ANALYSIS, message = line.trim()))
            }
        }

        if (exitCode != 0) {
            val errorMsg = "Pass 1 analysis failed with exit code $exitCode"
            onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
            pass1RecentLines.takeLast(10).forEach { onLog(TerminalLogEntry(tag = LogTag.WARN, message = "[FFmpeg] ${it.trim()}")) }
            return Result.failure(RuntimeException(errorMsg))
        }

        val analysis = LoudnormParser.parseLoudnormJson(pass1Output.toString())
            ?: return Result.failure(IllegalStateException("Failed to parse Pass 1 loudness measurement JSON"))

        onLog(
            TerminalLogEntry(
                tag = LogTag.SUCCESS,
                message = String.format("Pass 1 Complete: Input = %.1f LUFS | Peak = %.1f dBTP | LRA = %.1f LU",
                    analysis.inputI, analysis.inputTp, analysis.inputLra)
            )
        )
        return Result.success(analysis)
    }

    private fun executePass2Remux(
        inputPath: String,
        tempOutputFile: File,
        filterChain: String,
        config: NormalizationConfig,
        durationMs: Long,
        onLog: (TerminalLogEntry) -> Unit,
        onProgress: (passNumber: Int, progress: Float, speed: String, eta: String) -> Unit
    ): Result<Unit> {
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

        val pass2RecentLines = ArrayDeque<String>(25)
        val exitCode = executeCommand(pass2Cmd) { line ->
            synchronized(pass2RecentLines) {
                if (pass2RecentLines.size >= 20) pass2RecentLines.removeFirst()
                pass2RecentLines.addLast(line)
            }

            TelemetryParser.parseProgress(line, durationMs)?.let { telemetry ->
                onProgress(2, telemetry.progressFraction, telemetry.speedMultiplier, telemetry.etaFormatted)
            }

            if (line.contains("error", ignoreCase = true) || line.contains("warning", ignoreCase = true)) {
                onLog(TerminalLogEntry(tag = LogTag.WARN, message = line.trim()))
            } else if (line.contains("frame=") || line.contains("size=")) {
                onLog(TerminalLogEntry(tag = LogTag.REMUX, message = line.trim()))
            }
        }

        if (exitCode != 0) {
            val errorMsg = "Pass 2 remux failed with exit code $exitCode"
            onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
            pass2RecentLines.takeLast(10).forEach { onLog(TerminalLogEntry(tag = LogTag.WARN, message = "[FFmpeg] ${it.trim()}")) }
            return Result.failure(RuntimeException(errorMsg))
        }

        if (!tempOutputFile.exists() || tempOutputFile.length() <= 0L) {
            val errorMsg = "Output file was not created or is empty"
            onLog(TerminalLogEntry(tag = LogTag.ERROR, message = errorMsg))
            return Result.failure(IllegalStateException(errorMsg))
        }
        return Result.success(Unit)
    }

    companion object {
        private const val TAG = "FFmpegNormalizer"

        /**
         * Assembles the audio filter chain string for FFmpeg Pass 2 based on normalization configuration
         * and Pass 1 measured loudness telemetry.
         */
        fun buildFilterChain(
            config: NormalizationConfig,
            analysis: LoudnormAnalysis,
            onLog: ((TerminalLogEntry) -> Unit)? = null
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

                onLog?.invoke(
                    TerminalLogEntry(
                        tag = LogTag.SCENE,
                        message = "Applying Scene-Aware Dynamic Leveling [${intensity.label}]: Window=${frameLenMs}ms, MaxGain=+${maxGain}dB, Smoothing Gauss=$gaussSize"
                    )
                )
                "dynaudnorm=f=$frameLenMs:g=$gaussSize:m=$maxGain:p=0.95:r=0.6,loudnorm=I=${config.targetLufs}:TP=${config.targetTruePeak}:LRA=${config.targetLra}:linear=true"
            } else {
                "loudnorm=I=${config.targetLufs}:TP=${config.targetTruePeak}:LRA=${config.targetLra}:measured_I=${analysis.inputI}:measured_TP=${analysis.inputTp}:measured_LRA=${analysis.inputLra}:measured_thresh=${analysis.inputThresh}:offset=${analysis.targetOffset}:linear=true"
            }
        }
    }

    private fun executeCommand(command: List<String>, onLine: (String) -> Unit): Int {
        for (arg in command) {
            require(SecuritySanitizer.isProcessArgumentSafe(arg)) { "Unsafe process argument detected: $arg" }
        }

        val pb = ProcessBuilder(command)
        pb.redirectErrorStream(true)

        val env = pb.environment()
        val ldPath = buildString {
            if (ffmpegLibsDir.exists()) append(ffmpegLibsDir.absolutePath).append(":")
            if (pythonLibsDir.exists()) append(pythonLibsDir.absolutePath).append(":")
            append(nativeLibDir.absolutePath)
        }
        env["LD_LIBRARY_PATH"] = ldPath

        val process = pb.start()
        currentProcess = process
        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { onLine(it) }
                }
            }
            return process.waitFor()
        } finally {
            if (currentProcess === process) {
                currentProcess = null
            }
        }
    }
}
