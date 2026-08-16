package com.example.model

import java.io.File

data class AnalysisResult(
    val videoMetadata: VideoMetadata,
    val config: NarrationConfig,
    val detectedSubject: String,
    val steps: List<DrawingStep>,
    val fullScript: String,
    val srtSubtitles: String,
    val audioFile: File?,
    val audioDurationMs: Long,
    val processingDurationMs: Long,
    val keyframesCount: Int,
    val totalFramesSampled: Int
) {
    val audioDurationFormatted: String
        get() = DrawingStep.formatTime(audioDurationMs)

    val processingDurationFormatted: String
        get() = "${processingDurationMs / 1000}s"
}
