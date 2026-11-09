package com.example.deciflux.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LoudnormParserTest {

    @Test
    fun parseLoudnormJson_validBlock_extractsAllMetrics() {
        val sampleOutput = """
            [Parsed_loudnorm_0 @ 0xb400007835e21310] 
            {
                "input_i" : "-34.02",
                "input_tp" : "-8.91",
                "input_lra" : "22.90",
                "input_thresh" : "-45.12",
                "output_i" : "-14.05",
                "output_tp" : "-1.00",
                "output_lra" : "10.50",
                "output_thresh" : "-24.10",
                "normalization_type" : "dynamic",
                "target_offset" : "0.05"
            }
        """.trimIndent()

        val analysis = LoudnormParser.parseLoudnormJson(sampleOutput)

        assertNotNull(analysis)
        assertEquals(-34.02f, analysis!!.inputI, 0.01f)
        assertEquals(-8.91f, analysis.inputTp, 0.01f)
        assertEquals(22.90f, analysis.inputLra, 0.01f)
        assertEquals(-45.12f, analysis.inputThresh, 0.01f)
        assertEquals(0.05f, analysis.targetOffset, 0.01f)
    }

    @Test
    fun parseLoudnormJson_noJsonPresent_returnsNull() {
        val garbage = "ffmpeg version 6.1 Copyright (c) 2000-2023 the FFmpeg developers"
        val analysis = LoudnormParser.parseLoudnormJson(garbage)
        assertNull(analysis)
    }

    @Test
    fun parseLoudnormJson_malformedJson_returnsNull() {
        val malformed = """
            {
                "input_i" : -34.02,
                "input_tp" : incomplete...
        """.trimIndent()
        val analysis = LoudnormParser.parseLoudnormJson(malformed)
        assertNull(analysis)
    }
}
