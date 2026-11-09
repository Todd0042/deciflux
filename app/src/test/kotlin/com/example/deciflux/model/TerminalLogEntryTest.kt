package com.example.deciflux.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalLogEntryTest {

    @Test
    fun terminalLogEntry_defaultProperties_assignedProperly() {
        val entry = TerminalLogEntry(
            tag = LogTag.INFO,
            message = "Starting test batch"
        )

        assertNotNull(entry.id)
        assertTrue(entry.timestamp > 0L)
        assertEquals(LogTag.INFO, entry.tag)
        assertEquals("Starting test batch", entry.message)
        assertTrue(entry.formattedTime.matches(Regex("\\d{2}:\\d{2}:\\d{2}")))
    }

    @Test
    fun logTag_displayFormats_haveExpectedBrackets() {
        assertEquals("[INFO]", LogTag.INFO.display)
        assertEquals("[ANALYSIS]", LogTag.ANALYSIS.display)
        assertEquals("[SCENE]", LogTag.SCENE.display)
        assertEquals("[REMUX]", LogTag.REMUX.display)
        assertEquals("[SUCCESS]", LogTag.SUCCESS.display)
        assertEquals("[WARN]", LogTag.WARN.display)
        assertEquals("[ERROR]", LogTag.ERROR.display)
    }

    @Test
    fun terminalLogEntry_customTimestamp_formatsCorrectly() {
        val entry = TerminalLogEntry(
            timestamp = 1700000000000L,
            tag = LogTag.SUCCESS,
            message = "Batch complete"
        )
        assertNotNull(entry.formattedTime)
        assertTrue(entry.formattedTime.length == 8) // HH:mm:ss is 8 chars
    }
}
