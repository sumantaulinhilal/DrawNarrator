package com.example.processing

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class VideoProcessor(private val context: Context) {

    suspend fun extractMetadata(uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var fileName = "drawing_tutorial.mp4"
        var fileSize = 0L

        // Query ContentResolver for name and size
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex >= 0) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        if (fileSize == 0L) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    fileSize = pfd.statSize
                }
            } catch (_: Exception) {}
        }

        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            val durationMs = durationStr?.toLongOrNull() ?: 10000L
            var width = widthStr?.toIntOrNull() ?: 1280
            var height = heightStr?.toIntOrNull() ?: 720
            val rotation = rotationStr?.toIntOrNull() ?: 0

            // Swap width & height if rotated 90 or 270 degrees
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            VideoMetadata(
                uri = uri,
                fileName = fileName,
                durationMs = durationMs,
                width = width,
                height = height,
                sizeBytes = fileSize.coerceAtLeast(1024L),
                mimeType = mimeType
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun copyUriToCache(uri: Uri, targetName: String): File = withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, targetName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }
        cacheFile
    }
}
