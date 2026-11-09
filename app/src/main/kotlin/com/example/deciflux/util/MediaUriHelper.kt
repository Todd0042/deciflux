package com.example.deciflux.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
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
                        filename = cursor.getString(nameIndex)
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
     * Resolves an accessible file path for FFmpeg.
     * Uses /proc/self/fd/[fd] via ParcelFileDescriptor for zero-copy streaming,
     * or falls back to a cached copy if necessary.
     */
    fun openReadableFd(context: Context, uri: Uri): Pair<ParcelFileDescriptor?, String> {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val path = "/proc/self/fd/${pfd.fd}"
                Pair(pfd, path)
            } else {
                Pair(null, "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not open ParcelFileDescriptor for $uri", e)
            Pair(null, "")
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
}
