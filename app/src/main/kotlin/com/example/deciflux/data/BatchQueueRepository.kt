package com.example.deciflux.data

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.deciflux.model.BatchItemStatus
import com.example.deciflux.model.LogTag
import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.TerminalLogEntry
import com.example.deciflux.model.VideoBatchItem
import com.example.deciflux.service.BatchNormalizationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object BatchQueueRepository {

    private val _queue = MutableStateFlow<List<VideoBatchItem>>(emptyList())
    val queue: StateFlow<List<VideoBatchItem>> = _queue.asStateFlow()

    private val _config = MutableStateFlow(NormalizationConfig())
    val config: StateFlow<NormalizationConfig> = _config.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _pauseAfterCurrent = MutableStateFlow(false)
    val pauseAfterCurrent: StateFlow<Boolean> = _pauseAfterCurrent.asStateFlow()

    private val _currentItemId = MutableStateFlow<String?>(null)
    val currentItemId: StateFlow<String?> = _currentItemId.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLogEntry>>(
        listOf(
            TerminalLogEntry(
                tag = LogTag.INFO,
                message = "Deciflux Audio Normalization Engine ready. Add videos to begin."
            )
        )
    )
    val terminalLogs: StateFlow<List<TerminalLogEntry>> = _terminalLogs.asStateFlow()

    private val _overallProgress = MutableStateFlow(0.0f)
    val overallProgress: StateFlow<Float> = _overallProgress.asStateFlow()

    private val _currentSpeed = MutableStateFlow("--")
    val currentSpeed: StateFlow<String> = _currentSpeed.asStateFlow()

    private val _currentEta = MutableStateFlow("--:--")
    val currentEta: StateFlow<String> = _currentEta.asStateFlow()

    fun addVideos(items: List<VideoBatchItem>) {
        _queue.update { current ->
            val existingUris = current.map { it.uri }.toSet()
            val newItems = items.distinctBy { it.uri }.filter { it.uri !in existingUris }
            current + newItems
        }
        addLog(
            TerminalLogEntry(
                tag = LogTag.INFO,
                message = "Imported ${items.size} video(s) into queue. Total: ${_queue.value.size}"
            )
        )
    }

    fun removeVideo(id: String) {
        _queue.update { current -> current.filter { it.id != id } }
    }

    fun clearQueue() {
        if (!_isProcessing.value) {
            _queue.value = emptyList()
            _overallProgress.value = 0f
            addLog(TerminalLogEntry(tag = LogTag.INFO, message = "Batch queue cleared."))
        }
    }

    fun clearCompleted() {
        _queue.update { current ->
            current.filter { it.status != BatchItemStatus.COMPLETED }
        }
    }

    fun updateConfig(newConfig: NormalizationConfig) {
        _config.value = newConfig
        val sceneMsg = if (newConfig.sceneLevelingEnabled) {
            "Scene-by-Scene: ON (${newConfig.sceneLevelingIntensity.label})"
        } else {
            "Scene-by-Scene: OFF"
        }
        addLog(
            TerminalLogEntry(
                tag = LogTag.INFO,
                message = "Configuration updated: Target ${newConfig.targetLufs} LUFS, TP ${newConfig.targetTruePeak} dBTP • $sceneMsg"
            )
        )
    }

    fun addLog(entry: TerminalLogEntry) {
        _terminalLogs.update { current ->
            // Keep recent 300 logs in memory for fluid scrolling
            val updated = current + entry
            if (updated.size > 300) updated.takeLast(300) else updated
        }
    }

    fun clearLogs() {
        _terminalLogs.value = listOf(
            TerminalLogEntry(tag = LogTag.INFO, message = "Console logs cleared.")
        )
    }

    fun setProcessingState(processing: Boolean, paused: Boolean = false) {
        _isProcessing.value = processing
        _isPaused.value = paused
        if (!processing) {
            _pauseAfterCurrent.value = false
        }
    }

    fun setPauseAfterCurrent(pause: Boolean) {
        _pauseAfterCurrent.value = pause
    }

    fun setCurrentItem(id: String?) {
        _currentItemId.value = id
    }

    fun setSpeedAndEta(speed: String, eta: String) {
        _currentSpeed.value = speed
        _currentEta.value = eta
    }

    fun updateItem(
        id: String,
        transform: (VideoBatchItem) -> VideoBatchItem
    ) {
        _queue.update { current ->
            current.map { item ->
                if (item.id == id) transform(item) else item
            }
        }
        updateOverallProgress()
    }

    private fun updateOverallProgress() {
        val total = _queue.value.size
        if (total == 0) {
            _overallProgress.value = 0f
            return
        }
        val completed = _queue.value.count { it.status == BatchItemStatus.COMPLETED }
        val currentProcessing = _queue.value.firstOrNull { it.status == BatchItemStatus.PROCESSING }
        val currentProgress = currentProcessing?.progress ?: 0f

        val aggregate = (completed.toFloat() + currentProgress) / total.toFloat()
        _overallProgress.value = aggregate.coerceIn(0f, 1f)
    }

    fun startBatch(context: Context) {
        if (_queue.value.none { it.status == BatchItemStatus.QUEUED }) {
            // Reset any failed or cancelled items to queued
            _queue.update { list ->
                list.map {
                    if (it.status == BatchItemStatus.FAILED || it.status == BatchItemStatus.CANCELLED) {
                        it.copy(status = BatchItemStatus.QUEUED, progress = 0f, statusMessage = "Queued")
                    } else it
                }
            }
        }

        val intent = Intent(context, BatchNormalizationService::class.java).apply {
            action = BatchNormalizationService.ACTION_START_BATCH
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pauseBatch(context: Context) {
        val intent = Intent(context, BatchNormalizationService::class.java).apply {
            action = BatchNormalizationService.ACTION_PAUSE_BATCH
        }
        context.startService(intent)
    }

    fun resumeBatch(context: Context) {
        val intent = Intent(context, BatchNormalizationService::class.java).apply {
            action = BatchNormalizationService.ACTION_RESUME_BATCH
        }
        context.startService(intent)
    }

    fun togglePauseAfterCurrent(context: Context) {
        val willPause = !_pauseAfterCurrent.value
        _pauseAfterCurrent.value = willPause
        val intent = Intent(context, BatchNormalizationService::class.java).apply {
            action = if (willPause) {
                BatchNormalizationService.ACTION_SCHEDULE_PAUSE_AFTER_CURRENT
            } else {
                BatchNormalizationService.ACTION_CANCEL_SCHEDULED_PAUSE
            }
        }
        context.startService(intent)
    }

    fun cancelBatch(context: Context) {
        val intent = Intent(context, BatchNormalizationService::class.java).apply {
            action = BatchNormalizationService.ACTION_CANCEL_BATCH
        }
        context.startService(intent)
    }
}
