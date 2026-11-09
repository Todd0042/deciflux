package com.example.deciflux.ui.main

import android.net.Uri
import com.example.deciflux.data.BatchQueueRepository
import com.example.deciflux.model.BatchItemStatus
import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.NormalizationPreset
import com.example.deciflux.model.SceneLevelingIntensity
import com.example.deciflux.model.VideoBatchItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainScreenViewModelTest {

    private lateinit var viewModel: MainScreenViewModel

    @Before
    fun setUp() {
        BatchQueueRepository.clearQueue()
        viewModel = MainScreenViewModel()
    }

    @Test
    fun queue_initiallyEmpty() {
        assertTrue(viewModel.queue.value.isEmpty())
        assertFalse(viewModel.isProcessing.value)
    }

    @Test
    fun updateConfig_enablesSceneLeveling() {
        val newConfig = NormalizationConfig(
            preset = NormalizationPreset.YOUTUBE_SOCIAL,
            targetLufs = -14.0f,
            sceneLevelingEnabled = true,
            sceneLevelingIntensity = SceneLevelingIntensity.BALANCED
        )
        viewModel.updateConfig(newConfig)

        assertEquals(true, viewModel.config.value.sceneLevelingEnabled)
        assertEquals(SceneLevelingIntensity.BALANCED, viewModel.config.value.sceneLevelingIntensity)
        assertEquals(-14.0f, viewModel.config.value.targetLufs, 0.01f)
    }

    @Test
    fun addAndRemoveVideos_updatesQueueCorrectly() {
        val mockUri1 = org.mockito.Mockito.mock(Uri::class.java)
        val mockUri2 = org.mockito.Mockito.mock(Uri::class.java)

        val item1 = VideoBatchItem(
            id = "test-1",
            uri = mockUri1,
            filename = "clip1.mp4",
            durationMs = 60000L
        )
        val item2 = VideoBatchItem(
            id = "test-2",
            uri = mockUri2,
            filename = "clip2.mp4",
            durationMs = 120000L
        )

        viewModel.addScannedVideos(listOf(item1, item2))
        assertEquals(2, viewModel.queue.value.size)

        viewModel.removeVideo("test-1")
        assertEquals(1, viewModel.queue.value.size)
        assertEquals("test-2", viewModel.queue.value.first().id)
    }
}
