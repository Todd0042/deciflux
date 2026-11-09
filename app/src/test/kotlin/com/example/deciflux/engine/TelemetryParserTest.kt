package com.example.deciflux.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TelemetryParserTest {

    @Test
    fun parseTimeMs_validStatusLine_returnsMilliseconds() {
        val line = "frame=  325 fps=0.0 q=-1.0 size= 768KiB time=00:01:23.45 bitrate= 669.5kbits/s speed=18.8x"
        val timeMs = TelemetryParser.parseTimeMs(line)

        assertNotNull(timeMs)
        // 1 min * 60000 + 23.45 * 1000 = 83450 ms
        assertEquals(83450L, timeMs)
    }

    @Test
    fun parseSpeed_validStatusLine_returnsSpeedString() {
        val line = "frame= 1273 fps=636 q=-1.0 size= 5120KiB time=00:00:47.78 bitrate= 877.7kbits/s speed=23.9x"
        val speed = TelemetryParser.parseSpeed(line)
        assertEquals("23.9x", speed)
    }

    @Test
    fun parseSpeed_noSpeed_returnsDashes() {
        val line = "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'video.mp4':"
        val speed = TelemetryParser.parseSpeed(line)
        assertEquals("--", speed)
    }

    @Test
    fun calculateEta_validSpeedAndRemaining_returnsFormattedMinutesSeconds() {
        // Current: 30,000 ms, Total: 90,000 ms -> Remaining: 60,000 ms
        // Speed: 2.0x -> Real remaining: 30 seconds -> 00:30
        val line = "speed=2.0x"
        val eta = TelemetryParser.calculateEta(currentMs = 30000L, totalMs = 90000L, line = line)
        assertEquals("00:30", eta)
    }

    @Test
    fun calculateEta_zeroTotalDuration_returnsDashes() {
        val line = "speed=20.0x"
        val eta = TelemetryParser.calculateEta(currentMs = 5000L, totalMs = 0L, line = line)
        assertEquals("--:--", eta)
    }

    @Test
    fun parseProgress_completeLine_buildsTelemetryObject() {
        val line = "frame= 15015 fps=749 q=-1.0 size= 65071KiB time=00:10:00.60 bitrate= 887.5kbits/s speed= 30.0x"
        val totalMs = 600000L // 10 minutes

        val telemetry = TelemetryParser.parseProgress(line, totalMs)
        assertNotNull(telemetry)
        assertEquals(1.0f, telemetry!!.progressFraction, 0.05f)
        assertEquals("30.0x", telemetry.speedMultiplier)
    }
}
