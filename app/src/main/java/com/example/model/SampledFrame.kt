package com.example.model

import android.graphics.Bitmap
import java.io.File

data class SampledFrame(
    val frameIndex: Int,
    val timestampMs: Long,
    val bitmap: Bitmap? = null,
    val cachedFile: File? = null,
    val changeScore: Float = 0.0f,
    val isKeyframe: Boolean = false,
    val strokeDensity: Float = 0.0f,
    val strokeCenterY: Float = 0.5f,
    val strokeCenterX: Float = 0.5f
) {
    val timestampFormatted: String
        get() {
            val totalSeconds = timestampMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
