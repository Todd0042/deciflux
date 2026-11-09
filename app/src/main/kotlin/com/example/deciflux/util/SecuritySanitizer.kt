package com.example.deciflux.util

import java.io.File

/**
 * Defensive security utilities for path traversal mitigation and parameter sanitation.
 */
object SecuritySanitizer {

    private val ALLOWED_EXTENSIONS = setOf(
        "mp4", "mkv", "mov", "avi", "webm", "flv", "m4v", "3gp", "ts"
    )

    /**
     * Sanitizes filenames to eliminate directory traversal patterns (e.g. "../", "..\\")
     * and invalid filesystem characters.
     */
    fun sanitizeFilename(rawName: String, defaultName: String = "video.mp4"): String {
        val base = rawName.substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = base.replace(Regex("[^a-zA-Z0-9._\\-\\s()]"), "_")
        return if (cleaned.isBlank() || cleaned == "." || cleaned == "..") {
            defaultName
        } else {
            cleaned
        }
    }

    /**
     * Verifies that a target file path resides strictly inside the designated parent directory
     * to eliminate path traversal vulnerabilities.
     */
    fun isPathWithinDirectory(targetFile: File, parentDir: File): Boolean {
        val canonicalTarget = targetFile.canonicalFile
        val canonicalParent = parentDir.canonicalFile
        return canonicalTarget.path.startsWith(canonicalParent.path + File.separator) ||
                canonicalTarget.path == canonicalParent.path
    }

    /**
     * Validates that an argument intended for the native process does not contain
     * dangerous null bytes or newline control sequences.
     */
    fun isProcessArgumentSafe(arg: String): Boolean {
        return !arg.contains('\u0000') && !arg.contains('\r') && !arg.contains('\n')
    }

    /**
     * Verifies whether the video file extension is among safe supported media containers.
     */
    fun isSupportedVideoExtension(filename: String): Boolean {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return ALLOWED_EXTENSIONS.contains(ext)
    }
}
