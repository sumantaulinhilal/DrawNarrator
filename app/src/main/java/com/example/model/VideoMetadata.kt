package com.example.model

import android.net.Uri

data class VideoMetadata(
    val uri: Uri,
    val fileName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String = "video/mp4",
    val fps: Float = 30f
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (durationMs % 1000) / 100
            return String.format("%02d:%02d", minutes, seconds)
        }

    val resolutionFormatted: String
        get() = "${width}x${height}"

    val sizeFormatted: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.1f KB", kb)
            }
        }
}
