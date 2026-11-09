package com.example.deciflux.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderScannerTest {

    @Test
    fun isSupportedVideoExtension_supportedFormats_returnsTrue() {
        val formats = listOf("mp4", "MP4", "mkv", "MKV", "mov", "MOV", "webm", "WEBM", "avi", "ts", "3gp", "m4v")
        for (ext in formats) {
            assertTrue("Expected $ext to be supported", FolderScanner.isSupportedVideoExtension(ext))
        }
    }

    @Test
    fun isSupportedVideoExtension_unsupportedFormats_returnsFalse() {
        val formats = listOf("mp3", "wav", "flac", "jpg", "png", "txt", "pdf", "exe", "apk")
        for (ext in formats) {
            assertFalse("Expected $ext to be unsupported", FolderScanner.isSupportedVideoExtension(ext))
        }
    }

    @Test
    fun isSupportedVideoFilename_validFilenames_returnsTrue() {
        assertTrue(FolderScanner.isSupportedVideoFilename("vacation_video.mp4"))
        assertTrue(FolderScanner.isSupportedVideoFilename("CONCERT_RECORDING.MKV"))
        assertTrue(FolderScanner.isSupportedVideoFilename("nested.dots.in.filename.mov"))
        assertTrue(FolderScanner.isSupportedVideoFilename("presentation.webm"))
    }

    @Test
    fun isSupportedVideoFilename_invalidFilenames_returnsFalse() {
        assertFalse(FolderScanner.isSupportedVideoFilename("document.pdf"))
        assertFalse(FolderScanner.isSupportedVideoFilename("audio_track.mp3"))
        assertFalse(FolderScanner.isSupportedVideoFilename("no_extension_file"))
        assertFalse(FolderScanner.isSupportedVideoFilename(".gitignore"))
    }
}
