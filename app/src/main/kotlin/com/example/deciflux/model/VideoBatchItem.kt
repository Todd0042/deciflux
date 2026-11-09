package com.example.deciflux.model

import android.net.Uri
import java.util.UUID

enum class BatchItemStatus {
    QUEUED,
    ANALYZING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class VideoBatchItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val filename: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val status: BatchItemStatus = BatchItemStatus.QUEUED,
    val progress: Float = 0.0f,
    val statusMessage: String = "Queued",
    val initialLufs: Float? = null,
    val initialPeak: Float? = null,
    val normalizedLufs: Float? = null,
    val detectedScenes: Int? = null,
    val outputUri: Uri? = null,
    val outputPath: String? = null,
    val errorReason: String? = null
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "-- MB"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
