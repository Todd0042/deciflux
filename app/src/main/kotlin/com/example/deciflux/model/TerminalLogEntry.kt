package com.example.deciflux.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogTag(val display: String) {
    INFO("[INFO]"),
    ANALYSIS("[ANALYSIS]"),
    SCENE("[SCENE]"),
    REMUX("[REMUX]"),
    SUCCESS("[SUCCESS]"),
    WARN("[WARN]"),
    ERROR("[ERROR]")
}

data class TerminalLogEntry(
    val id: Long = idGenerator.incrementAndGet(),
    val timestamp: Long = System.currentTimeMillis(),
    val tag: LogTag,
    val message: String
) {
    val formattedTime: String
        get() = timeFormatter.format(Date(timestamp))

    companion object {
        private val idGenerator = AtomicLong(0)
        private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    }
}
