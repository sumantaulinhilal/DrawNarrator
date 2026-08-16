package com.example.ai

import com.example.model.DrawingStep
import com.example.model.SampledFrame
import com.example.model.VideoMetadata

class StepDetector {

    /**
     * Converts raw vision insights and sampled frames into structured, logically timed DrawingStep objects.
     */
    fun createDrawingSteps(
        metadata: VideoMetadata,
        visionResult: VisionAnalysisResult,
        keyframes: List<SampledFrame>
    ): List<DrawingStep> {
        val insights = visionResult.insights
        if (insights.isEmpty()) return emptyList()

        val steps = mutableListOf<DrawingStep>()
        val totalDurationMs = metadata.durationMs

        for (i in insights.indices) {
            val insight = insights[i]
            val keyframe = keyframes.getOrNull(i) ?: keyframes.lastOrNull()

            // Calculate start and end times for each step
            val startTimeMs = if (i == 0) {
                0L
            } else {
                insights[i - 1].timestampMs
            }

            val endTimeMs = if (i == insights.size - 1) {
                totalDurationMs
            } else {
                insights[i].timestampMs
            }

            // Ensure start < end with at least 2.5s minimum window
            val effectiveStart = startTimeMs.coerceAtMost(totalDurationMs - 1000L)
            val effectiveEnd = endTimeMs.coerceAtLeast(effectiveStart + 2500L).coerceAtMost(totalDurationMs)

            steps.add(
                DrawingStep(
                    stepNumber = i + 1,
                    startTimestampMs = effectiveStart,
                    endTimestampMs = effectiveEnd,
                    action = insight.actionName,
                    drawingObject = insight.drawingObject,
                    visualChange = insight.visualChangeDescription,
                    purpose = insight.pedagogicalPurpose,
                    confidence = insight.confidence,
                    keyframeFile = keyframe?.cachedFile
                )
            )
        }

        return steps
    }
}
