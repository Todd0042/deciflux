package com.example.deciflux.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.example.deciflux.model.VideoBatchItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object MediaUriHelper {
    private const val TAG = "MediaUriHelper"

    fun queryVideoItem(context: Context, uri: Uri): VideoBatchItem {
        var filename = "video_${System.currentTimeMillis()}.mp4"
        var sizeBytes = 0L

        // Query content resolver for display name and size
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor: Cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        filename = SecuritySanitizer.sanitizeFilename(cursor.getString(nameIndex))
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query cursor for uri: $uri", e)
        }

        // Query duration via MediaMetadataRetriever
        var durationMs = 0L
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract duration for uri: $uri", e)
        }

        return VideoBatchItem(
            uri = uri,
            filename = filename,
            durationMs = durationMs,
            sizeBytes = sizeBytes
        )
    }

    /**
     * Resolves the appropriate file extension given the URI scheme, filesystem path, or MIME type.
     */
    fun determineFileExtension(scheme: String?, path: String?, mimeType: String?): String {
        if (scheme == ContentResolver.SCHEME_FILE) {
            return path?.substringAfterLast('.', "mp4") ?: "mp4"
        }
        return when (mimeType) {
            "video/quicktime" -> "mov"
            "video/x-matroska" -> "mkv"
            "video/webm" -> "webm"
            "video/avi" -> "avi"
            else -> "mp4"
        }
    }

    /**
     * Copies an input stream to an output stream with periodic byte progress reporting.
     */
    fun copyStreamWithProgress(
        inStream: java.io.InputStream,
        outStream: java.io.OutputStream,
        onProgress: ((copiedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): Long {
        val buffer = ByteArray(128 * 1024)
        var bytesRead: Int
        var totalCopied = 0L
        while (inStream.read(buffer).also { bytesRead = it } != -1) {
            outStream.write(buffer, 0, bytesRead)
            totalCopied += bytesRead
            onProgress?.invoke(totalCopied, -1L)
        }
        outStream.flush()
        return totalCopied
    }

    /**
     * Prepares an accessible input file for FFmpeg.
     * Copies the content URI stream to a temporary cached file in context.cacheDir
     * ensuring direct POSIX filesystem access for the external FFmpeg child process.
     */
    fun prepareInputFile(
        context: Context,
        uri: Uri,
        onProgress: ((copiedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): File? {
        return try {
            val mime = if (uri.scheme == ContentResolver.SCHEME_FILE) null else context.contentResolver.getType(uri)
            val extension = determineFileExtension(uri.scheme, uri.path, mime)
            val tempFile = File(context.cacheDir, "input_stream_${System.currentTimeMillis()}.$extension")

            context.contentResolver.openInputStream(uri)?.use { inStream ->
                FileOutputStream(tempFile).use { outStream ->
                    copyStreamWithProgress(inStream, outStream, onProgress)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0L) {
                tempFile
            } else {
                tempFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy input URI to cache file: $uri", e)
            null
        }
    }

    /**
     * Copies a source file to an output stream (e.g. MediaStore or SAF DocumentFile).
     */
    fun copyFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy output file to target URI: $targetUri", e)
            false
        }
    }

    /**
     * Checks if internal/cache storage has sufficient free space for the operation.
     * Requires at least the file size plus a 250 MB safety margin.
     */
    fun checkStorageCapacity(context: Context, requiredBytes: Long): Pair<Boolean, Long> {
        return try {
            val stat = android.os.StatFs(context.cacheDir.absolutePath)
            val availableBytes = stat.availableBytes
            val safetyMargin = 250L * 1024L * 1024L // 250 MB safety margin
            val hasEnough = availableBytes > (requiredBytes + safetyMargin)
            Pair(hasEnough, availableBytes)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to inspect storage capacity", e)
            Pair(true, Long.MAX_VALUE)
        }
    }

    /**
     * Safely deletes the original source video file if permission is held (e.g. SAF DocumentFile or MediaStore).
     * Returns true if deleted, false if permission was denied or file could not be removed.
     */
    fun deleteOriginalUri(context: Context, uri: Uri): Boolean {
        return try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                val deletedRows = context.contentResolver.delete(uri, null, null)
                deletedRows > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete original file at $uri (requires explicit delete permission on scoped storage)", e)
            false
        }
    }
}
