package com.example.model

import java.io.File

data class DrawingStep(
    val stepNumber: Int,
    val startTimestampMs: Long,
    val endTimestampMs: Long,
    val action: String,
    val drawingObject: String,
    val visualChange: String,
    val purpose: String,
    val confidence: Float,
    var narrationText: String = "",
    val keyframeFile: File? = null
) {
    val startFormatted: String
        get() = formatTime(startTimestampMs)

    val endFormatted: String
        get() = formatTime(endTimestampMs)

    val durationSeconds: Float
        get() = ((endTimestampMs - startTimestampMs) / 1000f).coerceAtLeast(1.0f)

    val timeRangeFormatted: String
        get() = "$startFormatted–$endFormatted"

    companion object {
        fun formatTime(millis: Long): String {
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
}
