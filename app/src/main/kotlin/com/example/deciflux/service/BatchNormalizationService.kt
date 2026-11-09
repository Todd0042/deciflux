package com.example.deciflux.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.deciflux.DecifluxApp
import com.example.deciflux.MainActivity
import com.example.deciflux.R
import com.example.deciflux.data.BatchQueueRepository
import com.example.deciflux.engine.FFmpegAudioNormalizer
import com.example.deciflux.model.BatchItemStatus
import com.example.deciflux.model.LogTag
import com.example.deciflux.model.TerminalLogEntry
import com.example.deciflux.model.VideoBatchItem
import com.example.deciflux.util.MediaUriHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class BatchNormalizationService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var normalizer: FFmpegAudioNormalizer
    private var isBatchCancelled = false

    override fun onCreate() {
        super.onCreate()
        normalizer = FFmpegAudioNormalizer(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Deciflux:BatchNormalizationWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_BATCH -> startBatchProcessing()
            ACTION_PAUSE_BATCH -> pauseBatchProcessing()
            ACTION_RESUME_BATCH -> resumeBatchProcessing()
            ACTION_SCHEDULE_PAUSE_AFTER_CURRENT -> schedulePauseAfterCurrent()
            ACTION_CANCEL_SCHEDULED_PAUSE -> cancelScheduledPause()
            ACTION_CANCEL_BATCH -> cancelBatchProcessing()
        }
        return START_NOT_STICKY
    }

    private var activeQueueJob: Job? = null

    private fun startBatchProcessing() {
        isBatchCancelled = false
        activeQueueJob?.cancel()
        BatchQueueRepository.setProcessingState(processing = true, paused = false)
        wakeLock?.acquire(4 * 60 * 60 * 1000L) // Safety timeout: 4 hours

        startForegroundWithNotification(
            title = "Deciflux Batch Normalizer",
            message = "Starting batch queue...",
            progress = 0,
            max = 100
        )

        activeQueueJob = serviceScope.launch {
            processQueue()
        }
    }

    private fun schedulePauseAfterCurrent() {
        BatchQueueRepository.setPauseAfterCurrent(true)
        BatchQueueRepository.addLog(
            TerminalLogEntry(tag = LogTag.WARN, message = "Pause scheduled: Batch will pause once the current video finishes.")
        )
    }

    private fun cancelScheduledPause() {
        BatchQueueRepository.setPauseAfterCurrent(false)
        BatchQueueRepository.addLog(
            TerminalLogEntry(tag = LogTag.INFO, message = "Scheduled pause cancelled: Batch will continue processing.")
        )
    }

    private fun pauseBatchProcessing() {
        BatchQueueRepository.setProcessingState(processing = true, paused = true)
        BatchQueueRepository.addLog(
            TerminalLogEntry(tag = LogTag.WARN, message = "Batch paused by user.")
        )
        updateNotification(
            title = "Deciflux (Paused)",
            message = "Batch processing paused",
            progress = (BatchQueueRepository.overallProgress.value * 100).toInt(),
            max = 100
        )
    }

    private fun resumeBatchProcessing() {
        BatchQueueRepository.setProcessingState(processing = true, paused = false)
        BatchQueueRepository.setPauseAfterCurrent(false)
        BatchQueueRepository.addLog(
            TerminalLogEntry(tag = LogTag.INFO, message = "Resuming batch queue...")
        )
        activeQueueJob?.cancel()
        activeQueueJob = serviceScope.launch {
            processQueue()
        }
    }

    private fun cancelBatchProcessing() {
        isBatchCancelled = true
        activeQueueJob?.cancel()
        normalizer.cancelCurrentExecution()

        val currentId = BatchQueueRepository.currentItemId.value
        if (currentId != null) {
            BatchQueueRepository.updateItem(currentId) {
                if (it.status != BatchItemStatus.COMPLETED) {
                    it.copy(status = BatchItemStatus.CANCELLED, statusMessage = "Stopped by user", progress = 0f)
                } else it
            }
        }

        BatchQueueRepository.setProcessingState(processing = false, paused = false)
        BatchQueueRepository.setCurrentItem(null)
        BatchQueueRepository.setSpeedAndEta("--", "--:--")
        BatchQueueRepository.addLog(
            TerminalLogEntry(tag = LogTag.WARN, message = "Batch processing stopped by user.")
        )

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        androidx.core.app.ServiceCompat.stopForeground(this, androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun processQueue() {
        val config = BatchQueueRepository.config.value
        val items = BatchQueueRepository.queue.value

        var processedCount = items.count { it.status == BatchItemStatus.COMPLETED }
        val totalCount = items.size

        for (item in items) {
            if (isBatchCancelled || !serviceScope.isActive) break

            // Handle pause state
            while (BatchQueueRepository.isPaused.value && !isBatchCancelled && serviceScope.isActive) {
                delay(500)
            }
            if (isBatchCancelled) break

            if (item.status != BatchItemStatus.QUEUED) continue

            BatchQueueRepository.setCurrentItem(item.id)
            BatchQueueRepository.updateItem(item.id) {
                it.copy(status = BatchItemStatus.ANALYZING, statusMessage = "Pass 1: Analyzing Loudness...")
            }

            val itemIndex = items.indexOfFirst { it.id == item.id } + 1
            val notificationTitle = "Processing $itemIndex of $totalCount"
            updateNotification(
                title = notificationTitle,
                message = item.filename,
                progress = ((itemIndex - 1).toFloat() / totalCount.toFloat() * 100).toInt(),
                max = 100
            )

            BatchQueueRepository.addLog(
                TerminalLogEntry(
                    tag = LogTag.INFO,
                    message = "=================================================="
                )
            )
            BatchQueueRepository.addLog(
                TerminalLogEntry(
                    tag = LogTag.INFO,
                    message = "[$itemIndex/$totalCount] Starting: ${item.filename} (${item.formattedDuration})"
                )
            )

            // Pre-flight safeguard: verify storage capacity before processing video
            val (hasSpace, freeBytes) = MediaUriHelper.checkStorageCapacity(this, item.sizeBytes)
            if (!hasSpace) {
                val freeMb = freeBytes / (1024 * 1024)
                val requiredMb = (item.sizeBytes / (1024 * 1024)) + 250
                val errorReason = "Insufficient storage space: ${freeMb}MB free, requires ~${requiredMb}MB"
                BatchQueueRepository.updateItem(item.id) {
                    it.copy(
                        status = BatchItemStatus.FAILED,
                        statusMessage = errorReason,
                        errorReason = errorReason
                    )
                }
                BatchQueueRepository.addLog(
                    TerminalLogEntry(tag = LogTag.ERROR, message = "[$itemIndex/$totalCount] Skipped ${item.filename}: $errorReason")
                )
                continue
            }

            // Isolated temporary output file
            val tempOutputFile = File(cacheDir, "deciflux_tmp_${System.currentTimeMillis()}.mp4")

            val result = normalizer.normalizeVideo(
                inputUri = item.uri,
                durationMs = item.durationMs,
                config = config,
                tempOutputFile = tempOutputFile,
                onLog = { entry -> BatchQueueRepository.addLog(entry) },
                onProgress = { pass, progress, speed, eta ->
                    BatchQueueRepository.setSpeedAndEta(speed, eta)
                    val statusText = if (pass == 1) {
                        "Pass 1/2: Analyzing (Speed: $speed)"
                    } else {
                        "Pass 2/2: Remuxing (${(progress * 100).toInt()}%, Speed: $speed, ETA: $eta)"
                    }

                    BatchQueueRepository.updateItem(item.id) {
                        it.copy(
                            status = BatchItemStatus.PROCESSING,
                            progress = progress,
                            statusMessage = statusText
                        )
                    }

                    val overallPercent = ((processedCount.toFloat() + progress) / totalCount.toFloat() * 100).toInt()
                    updateNotification(
                        title = "Processing $itemIndex of $totalCount (${overallPercent}%)",
                        message = "${item.filename} • ETA: $eta",
                        progress = overallPercent,
                        max = 100
                    )
                }
            )

            if (isBatchCancelled || !serviceScope.isActive) {
                tempOutputFile.delete()
                break
            }

            if (result.isSuccess) {
                val analysis = result.getOrNull()
                // Atomic move to Movies/Deciflux MediaStore collection
                val finalUri = saveVideoToMediaStore(tempOutputFile, item.filename, config.outputSuffix)
                tempOutputFile.delete()

                // If user enabled "Replace original file", delete original after verified successful export
                if (config.replaceOriginal && finalUri != null) {
                    val deleted = MediaUriHelper.deleteOriginalUri(this, item.uri)
                    if (deleted) {
                        BatchQueueRepository.addLog(
                            TerminalLogEntry(
                                tag = LogTag.INFO,
                                message = "Original file replaced/deleted: ${item.filename}"
                            )
                        )
                    } else {
                        BatchQueueRepository.addLog(
                            TerminalLogEntry(
                                tag = LogTag.WARN,
                                message = "Could not delete original file (system permission restricted). Normalized file preserved in Movies/Deciflux."
                            )
                        )
                    }
                }

                processedCount++
                BatchQueueRepository.updateItem(item.id) {
                    it.copy(
                        status = BatchItemStatus.COMPLETED,
                        progress = 1.0f,
                        statusMessage = "Completed",
                        initialLufs = analysis?.inputI,
                        initialPeak = analysis?.inputTp,
                        normalizedLufs = config.targetLufs,
                        outputUri = finalUri
                    )
                }

                BatchQueueRepository.addLog(
                    TerminalLogEntry(
                        tag = LogTag.SUCCESS,
                        message = "[$itemIndex/$totalCount] Successfully normalized and saved: ${item.filename}"
                    )
                )
            } else {
                tempOutputFile.delete()
                if (isBatchCancelled) {
                    break
                }
                val errorReason = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                BatchQueueRepository.updateItem(item.id) {
                    it.copy(
                        status = BatchItemStatus.FAILED,
                        statusMessage = "Failed: $errorReason",
                        errorReason = errorReason
                    )
                }
                BatchQueueRepository.addLog(
                    TerminalLogEntry(
                        tag = LogTag.ERROR,
                        message = "[$itemIndex/$totalCount] Processing failed for ${item.filename}: $errorReason"
                    )
                )
            }

            // If user requested pause after current video, enter paused state now
            if (BatchQueueRepository.pauseAfterCurrent.value && !isBatchCancelled) {
                BatchQueueRepository.setPauseAfterCurrent(false)
                pauseBatchProcessing()
            }
        }

        if (isBatchCancelled) {
            return
        }

        // Batch finished
        BatchQueueRepository.setProcessingState(processing = false, paused = false)
        BatchQueueRepository.setCurrentItem(null)
        BatchQueueRepository.setSpeedAndEta("--", "--:--")

        val completedCount = BatchQueueRepository.queue.value.count { it.status == BatchItemStatus.COMPLETED }
        val failedCount = BatchQueueRepository.queue.value.count { it.status == BatchItemStatus.FAILED }

        BatchQueueRepository.addLog(
            TerminalLogEntry(
                tag = LogTag.SUCCESS,
                message = "=================================================="
            )
        )
        BatchQueueRepository.addLog(
            TerminalLogEntry(
                tag = LogTag.SUCCESS,
                message = "Batch Finished: $completedCount completed, $failedCount failed out of $totalCount total."
            )
        )

        updateNotification(
            title = "Deciflux: Batch Complete",
            message = "$completedCount of $totalCount videos normalized successfully.",
            progress = 100,
            max = 100,
            ongoing = false
        )

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        androidx.core.app.ServiceCompat.stopForeground(this, androidx.core.app.ServiceCompat.STOP_FOREGROUND_DETACH)
    }

    private fun saveVideoToMediaStore(tempFile: File, originalName: String, suffix: String): Uri? {
        val baseName = originalName.substringBeforeLast('.')
        val ext = originalName.substringAfterLast('.', "mp4")
        val outputName = "${baseName}${suffix}.${ext}"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, outputName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Deciflux")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            val success = MediaUriHelper.copyFileToUri(this, tempFile, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
            if (success) {
                MediaScannerConnection.scanFile(this, arrayOf(tempFile.absolutePath), null, null)
                return uri
            }
        }
        return null
    }

    private fun startForegroundWithNotification(title: String, message: String, progress: Int, max: Int) {
        val notification = buildNotification(title, message, progress, max, ongoing = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                DecifluxApp.BATCH_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                DecifluxApp.BATCH_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(DecifluxApp.BATCH_NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(
        title: String,
        message: String,
        progress: Int,
        max: Int,
        ongoing: Boolean = true
    ) {
        val notification = buildNotification(title, message, progress, max, ongoing)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(DecifluxApp.BATCH_NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        title: String,
        message: String,
        progress: Int,
        max: Int,
        ongoing: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, BatchNormalizationService::class.java).apply {
            action = ACTION_CANCEL_BATCH
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, DecifluxApp.BATCH_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(ongoing)
            .setProgress(max, progress, false)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Cancel", cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_BATCH = "com.example.deciflux.START_BATCH"
        const val ACTION_PAUSE_BATCH = "com.example.deciflux.PAUSE_BATCH"
        const val ACTION_RESUME_BATCH = "com.example.deciflux.RESUME_BATCH"
        const val ACTION_SCHEDULE_PAUSE_AFTER_CURRENT = "com.example.deciflux.SCHEDULE_PAUSE_AFTER_CURRENT"
        const val ACTION_CANCEL_SCHEDULED_PAUSE = "com.example.deciflux.CANCEL_SCHEDULED_PAUSE"
        const val ACTION_CANCEL_BATCH = "com.example.deciflux.CANCEL_BATCH"
    }
}
