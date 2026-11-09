package com.example.deciflux.util

import android.content.ContentResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MediaUriHelperTest {

    @Test
    fun determineFileExtension_fileScheme_extractsPathExtension() {
        val ext = MediaUriHelper.determineFileExtension(
            scheme = ContentResolver.SCHEME_FILE,
            path = "/storage/emulated/0/Movies/test.mkv",
            mimeType = null
        )
        assertEquals("mkv", ext)
    }

    @Test
    fun determineFileExtension_fileSchemeWithoutExtension_defaultsToMp4() {
        val ext = MediaUriHelper.determineFileExtension(
            scheme = ContentResolver.SCHEME_FILE,
            path = "/storage/emulated/0/Movies/test",
            mimeType = null
        )
        assertEquals("mp4", ext)
    }

    @Test
    fun determineFileExtension_contentScheme_resolvesKnownMimes() {
        assertEquals("mov", MediaUriHelper.determineFileExtension(scheme = "content", path = null, mimeType = "video/quicktime"))
        assertEquals("mkv", MediaUriHelper.determineFileExtension(scheme = "content", path = null, mimeType = "video/x-matroska"))
        assertEquals("webm", MediaUriHelper.determineFileExtension(scheme = "content", path = null, mimeType = "video/webm"))
        assertEquals("avi", MediaUriHelper.determineFileExtension(scheme = "content", path = null, mimeType = "video/avi"))
        assertEquals("mp4", MediaUriHelper.determineFileExtension(scheme = "content", path = null, mimeType = "video/mp4"))
        assertEquals("mp4", MediaUriHelper.determineFileExtension(scheme = "content", path = null, mimeType = "application/octet-stream"))
    }

    @Test
    fun copyStreamWithProgress_copiesAllBytesAndReportsProgress() {
        val data = "Deciflux audio normalization sample test data stream".toByteArray()
        val inStream = ByteArrayInputStream(data)
        val outStream = ByteArrayOutputStream()

        var progressCalls = 0
        var lastCopied = 0L

        val copiedBytes = MediaUriHelper.copyStreamWithProgress(inStream, outStream) { copied, _ ->
            progressCalls++
            lastCopied = copied
        }

        assertEquals(data.size.toLong(), copiedBytes)
        assertEquals(data.size.toLong(), lastCopied)
        assertTrue(progressCalls >= 1)
        assertEquals("Deciflux audio normalization sample test data stream", outStream.toString())
    }
}
