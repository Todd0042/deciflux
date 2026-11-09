package com.example.deciflux.ui.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deciflux.data.BatchQueueRepository
import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.TerminalLogEntry
import com.example.deciflux.model.VideoBatchItem
import com.example.deciflux.util.MediaUriHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel : ViewModel() {

    val queue: StateFlow<List<VideoBatchItem>> = BatchQueueRepository.queue
    val config: StateFlow<NormalizationConfig> = BatchQueueRepository.config
    val isProcessing: StateFlow<Boolean> = BatchQueueRepository.isProcessing
    val isPaused: StateFlow<Boolean> = BatchQueueRepository.isPaused
    val currentItemId: StateFlow<String?> = BatchQueueRepository.currentItemId
    val terminalLogs: StateFlow<List<TerminalLogEntry>> = BatchQueueRepository.terminalLogs
    val overallProgress: StateFlow<Float> = BatchQueueRepository.overallProgress
    val currentSpeed: StateFlow<String> = BatchQueueRepository.currentSpeed
    val currentEta: StateFlow<String> = BatchQueueRepository.currentEta

    fun addSingleVideo(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = MediaUriHelper.queryVideoItem(context, uri)
            BatchQueueRepository.addVideos(listOf(item))
        }
    }

    fun addMultipleVideos(context: Context, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = uris.map { uri -> MediaUriHelper.queryVideoItem(context, uri) }
            BatchQueueRepository.addVideos(items)
        }
    }

    fun addScannedVideos(items: List<VideoBatchItem>) {
        BatchQueueRepository.addVideos(items)
    }

    fun removeVideo(id: String) {
        BatchQueueRepository.removeVideo(id)
    }

    fun clearQueue() {
        BatchQueueRepository.clearQueue()
    }

    fun clearCompleted() {
        BatchQueueRepository.clearCompleted()
    }

    fun updateConfig(newConfig: NormalizationConfig) {
        BatchQueueRepository.updateConfig(newConfig)
    }

    fun clearLogs() {
        BatchQueueRepository.clearLogs()
    }

    fun startBatch(context: Context) {
        BatchQueueRepository.startBatch(context)
    }

    fun pauseBatch(context: Context) {
        BatchQueueRepository.pauseBatch(context)
    }

    fun resumeBatch(context: Context) {
        BatchQueueRepository.resumeBatch(context)
    }

    fun cancelBatch(context: Context) {
        BatchQueueRepository.cancelBatch(context)
    }
}
