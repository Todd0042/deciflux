package com.example.deciflux.model

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class VideoBatchItemTest {

    @Test
    fun formattedDuration_underOneHour_formatsMinutesSeconds() {
        val mockUri = Mockito.mock(Uri::class.java)
        val item = VideoBatchItem(
            id = "1",
            uri = mockUri,
            filename = "clip.mp4",
            durationMs = 125000L // 2 min 5 sec
        )
        assertEquals("02:05", item.formattedDuration)
    }

    @Test
    fun formattedDuration_overOneHour_formatsHoursMinutesSeconds() {
        val mockUri = Mockito.mock(Uri::class.java)
        val item = VideoBatchItem(
            id = "2",
            uri = mockUri,
            filename = "movie.mp4",
            durationMs = 5400000L // 1 hr 30 min 00 sec
        )
        assertEquals("1:30:00", item.formattedDuration)
    }

    @Test
    fun formattedSize_variousByteSizes_formatsAccurately() {
        val mockUri = Mockito.mock(Uri::class.java)
        val small = VideoBatchItem(id = "1", uri = mockUri, filename = "1.mp4", sizeBytes = 500 * 1024L)
        assertEquals("0.5 MB", small.formattedSize)

        val medium = VideoBatchItem(id = "2", uri = mockUri, filename = "2.mp4", sizeBytes = 55 * 1024 * 1024L)
        assertEquals("55.0 MB", medium.formattedSize)

        val large = VideoBatchItem(id = "3", uri = mockUri, filename = "3.mp4", sizeBytes = 2500L * 1024 * 1024)
        assertEquals("2.44 GB", large.formattedSize)
    }

    @Test
    fun batchItemStatus_allStatusesAccessible() {
        val statuses = BatchItemStatus.values()
        assertEquals(6, statuses.size)
    }
}
