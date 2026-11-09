package com.example.deciflux.data

import android.net.Uri
import com.example.deciflux.model.BatchItemStatus
import com.example.deciflux.model.LogTag
import com.example.deciflux.model.TerminalLogEntry
import com.example.deciflux.model.VideoBatchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class BatchQueueRepositoryTest {

    @Before
    fun setUp() {
        BatchQueueRepository.clearQueue()
        BatchQueueRepository.clearLogs()
        BatchQueueRepository.setProcessingState(processing = false, paused = false)
        BatchQueueRepository.setPauseAfterCurrent(false)
    }

    @Test
    fun addVideos_distinctUris_preventsDuplicates() {
        val mockUri = Mockito.mock(Uri::class.java)
        val item1 = VideoBatchItem(id = "1", uri = mockUri, filename = "video.mp4")
        val item2 = VideoBatchItem(id = "2", uri = mockUri, filename = "video.mp4")

        BatchQueueRepository.addVideos(listOf(item1, item2))
        assertEquals(1, BatchQueueRepository.queue.value.size)
    }

    @Test
    fun pauseAndResume_updatesStateFlows() {
        assertFalse(BatchQueueRepository.isPaused.value)
        BatchQueueRepository.setProcessingState(processing = true, paused = true)
        assertTrue(BatchQueueRepository.isPaused.value)
        assertTrue(BatchQueueRepository.isProcessing.value)

        BatchQueueRepository.setProcessingState(processing = true, paused = false)
        assertFalse(BatchQueueRepository.isPaused.value)
    }

    @Test
    fun pauseAfterCurrent_togglesState() {
        assertFalse(BatchQueueRepository.pauseAfterCurrent.value)
        BatchQueueRepository.setPauseAfterCurrent(true)
        assertTrue(BatchQueueRepository.pauseAfterCurrent.value)
    }

    @Test
    fun addLog_appendsLogAndMaintainsBuffer() {
        val entry = TerminalLogEntry(tag = LogTag.ANALYSIS, message = "Pass 1 started")
        BatchQueueRepository.addLog(entry)
        assertTrue(BatchQueueRepository.terminalLogs.value.any { it.tag == LogTag.ANALYSIS && it.message == "Pass 1 started" })
    }

    @Test
    fun updateItem_modifiesSpecificItem() {
        val mockUri = Mockito.mock(Uri::class.java)
        val item = VideoBatchItem(id = "target", uri = mockUri, filename = "target.mp4")
        BatchQueueRepository.addVideos(listOf(item))

        BatchQueueRepository.updateItem("target") {
            it.copy(status = BatchItemStatus.COMPLETED, progress = 1.0f)
        }

        val updated = BatchQueueRepository.queue.value.first()
        assertEquals(BatchItemStatus.COMPLETED, updated.status)
        assertEquals(1.0f, updated.progress, 0.01f)
    }
}
