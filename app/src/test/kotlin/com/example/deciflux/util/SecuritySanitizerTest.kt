package com.example.deciflux.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SecuritySanitizerTest {

    @Test
    fun sanitizeFilename_pathTraversalSequence_stripsEscapes() {
        val malicious = "../../etc/shadow.mp4"
        val sanitized = SecuritySanitizer.sanitizeFilename(malicious)
        assertEquals("shadow.mp4", sanitized)
    }

    @Test
    fun sanitizeFilename_windowsTraversal_stripsBackslashes() {
        val malicious = "..\\..\\Windows\\System32\\cmd.exe.mp4"
        val sanitized = SecuritySanitizer.sanitizeFilename(malicious)
        assertEquals("cmd.exe.mp4", sanitized)
    }

    @Test
    fun sanitizeFilename_illegalCharacters_replacedWithUnderscores() {
        val dirty = "my<cool>:video*file?name|.mp4"
        val sanitized = SecuritySanitizer.sanitizeFilename(dirty)
        assertEquals("my_cool__video_file_name_.mp4", sanitized)
    }

    @Test
    fun isPathWithinDirectory_childFile_returnsTrue() {
        val parent = File("/tmp/deciflux_cache")
        val child = File("/tmp/deciflux_cache/video_123.mp4")
        assertTrue(SecuritySanitizer.isPathWithinDirectory(child, parent))
    }

    @Test
    fun isPathWithinDirectory_escapingPath_returnsFalse() {
        val parent = File("/tmp/deciflux_cache")
        val escape = File("/tmp/deciflux_cache/../../etc/passwd")
        assertFalse(SecuritySanitizer.isPathWithinDirectory(escape, parent))
    }

    @Test
    fun isProcessArgumentSafe_normalArgument_returnsTrue() {
        assertTrue(SecuritySanitizer.isProcessArgumentSafe("-c:v"))
        assertTrue(SecuritySanitizer.isProcessArgumentSafe("copy"))
        assertTrue(SecuritySanitizer.isProcessArgumentSafe("loudnorm=I=-14:TP=-1.0:LRA=11.0"))
    }

    @Test
    fun isProcessArgumentSafe_nullByteOrNewline_returnsFalse() {
        assertFalse(SecuritySanitizer.isProcessArgumentSafe("malicious\u0000arg"))
        assertFalse(SecuritySanitizer.isProcessArgumentSafe("arg1\nrm -rf /"))
    }

    @Test
    fun isSupportedVideoExtension_validExtensions_returnsTrue() {
        assertTrue(SecuritySanitizer.isSupportedVideoExtension("clip.mp4"))
        assertTrue(SecuritySanitizer.isSupportedVideoExtension("movie.mkv"))
        assertTrue(SecuritySanitizer.isSupportedVideoExtension("drone.MOV"))
        assertTrue(SecuritySanitizer.isSupportedVideoExtension("web.webm"))
    }

    @Test
    fun isSupportedVideoExtension_dangerousExtensions_returnsFalse() {
        assertFalse(SecuritySanitizer.isSupportedVideoExtension("malware.exe"))
        assertFalse(SecuritySanitizer.isSupportedVideoExtension("script.sh"))
        assertFalse(SecuritySanitizer.isSupportedVideoExtension("archive.zip"))
    }
}
