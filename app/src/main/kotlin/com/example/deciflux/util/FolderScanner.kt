package com.example.deciflux.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.deciflux.model.VideoBatchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FolderScanner {

    private val SUPPORTED_VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "mov", "webm", "avi", "ts", "3gp", "m4v"
    )

    suspend fun scanDocumentTree(
        context: Context,
        treeUri: Uri,
        onProgress: (scannedCount: Int, foundCount: Int) -> Unit
    ): List<VideoBatchItem> = withContext(Dispatchers.IO) {
        // Persist permissions so app can access folder later
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (_: Exception) {
            // Some providers don't support persistable flags; continue anyway
        }

        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val foundItems = mutableListOf<VideoBatchItem>()
        var scannedCount = 0

        fun traverse(dir: DocumentFile) {
            val files = dir.listFiles()
            for (file in files) {
                scannedCount++
                if (file.isDirectory) {
                    traverse(file)
                } else if (file.isFile && isVideoFile(file)) {
                    val item = MediaUriHelper.queryVideoItem(context, file.uri)
                    foundItems.add(item)
                    onProgress(scannedCount, foundItems.size)
                }
            }
        }

        traverse(rootDoc)
        foundItems
    }

    private fun isVideoFile(file: DocumentFile): Boolean {
        val type = file.type
        if (type != null && type.startsWith("video/", ignoreCase = true)) {
            return true
        }
        val name = file.name ?: return false
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in SUPPORTED_VIDEO_EXTENSIONS
    }
}
