package com.example.processing

import android.graphics.Bitmap
import com.example.model.SampledFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

class ChangeDetector {

    /**
     * Analyzes a list of sampled frames, calculates change scores,
     * groups consecutive similar frames, and flags significant keyframes.
     */
    suspend fun detectChanges(
        frames: List<SampledFrame>,
        changeThreshold: Float = 0.08f,
        onProgress: (progress: Float) -> Unit
    ): List<SampledFrame> = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) return@withContext emptyList()
        if (frames.size == 1) {
            return@withContext listOf(frames[0].copy(isKeyframe = true, changeScore = 1.0f))
        }

        val enriched = mutableListOf<SampledFrame>()

        // The first frame is always an anchor/keyframe
        enriched.add(frames[0].copy(isKeyframe = true, changeScore = 1.0f))

        var lastKeyframe = frames[0]
        var cumulativeChange = 0.0f

        for (i in 1 until frames.size) {
            val current = frames[i]
            val prev = frames[i - 1]

            // 1. Calculate frame-to-frame difference
            val stepDiff = computeFrameDifference(prev.bitmap, current.bitmap)

            // 2. Calculate stroke density growth
            val densityDiff = current.strokeDensity - prev.strokeDensity

            // 3. Positional center shift (movement of drawing focus area)
            val posShift = abs(current.strokeCenterX - prev.strokeCenterX) +
                    abs(current.strokeCenterY - prev.strokeCenterY)

            // Filter out transient hand occlusions (sudden spike followed by immediate recovery)
            val changeScore = (stepDiff * 0.5f) + (abs(densityDiff) * 0.35f) + (posShift * 0.15f)
            cumulativeChange += changeScore

            // Compare with the last accepted keyframe to detect when a meaningful drawing step accumulated
            val diffFromLastKeyframe = computeFrameDifference(lastKeyframe.bitmap, current.bitmap)
            val timeSinceLastKeyframeMs = current.timestampMs - lastKeyframe.timestampMs

            // Keyframe trigger conditions:
            // 1) Substantial visual ink change occurred
            // 2) Or accumulated changes crossed threshold with at least 3 seconds separation
            // 3) Or maximum time window elapsed (e.g. 20s without a keyframe)
            val isKeyframe = (diffFromLastKeyframe >= changeThreshold && timeSinceLastKeyframeMs >= 3000L) ||
                    (cumulativeChange >= 0.30f && timeSinceLastKeyframeMs >= 2500L) ||
                    (timeSinceLastKeyframeMs >= 25000L) ||
                    (i == frames.size - 1) // Always include final completed drawing

            val updatedFrame = current.copy(
                changeScore = changeScore,
                isKeyframe = isKeyframe
            )
            enriched.add(updatedFrame)

            if (isKeyframe) {
                lastKeyframe = updatedFrame
                cumulativeChange = 0.0f
            }

            onProgress(i.toFloat() / frames.size)
        }

        enriched
    }

    /**
     * Computes perceptual pixel difference between two bitmaps.
     * Normalized between 0.0 (identical) and 1.0 (completely different).
     */
    private fun computeFrameDifference(bmpA: Bitmap?, bmpB: Bitmap?): Float {
        if (bmpA == null || bmpB == null) return 0f
        if (bmpA.width != bmpB.width || bmpA.height != bmpB.height) return 0.5f

        val width = bmpA.width
        val height = bmpA.height
        val sampleStep = 6 // Fast sampling grid

        val pixelsA = IntArray(width * height)
        val pixelsB = IntArray(width * height)
        bmpA.getPixels(pixelsA, 0, width, 0, 0, width, height)
        bmpB.getPixels(pixelsB, 0, width, 0, 0, width, height)

        var totalDelta = 0L
        var sampleCount = 0

        for (y in 0 until height step sampleStep) {
            val rowOffset = y * width
            for (x in 0 until width step sampleStep) {
                val idx = rowOffset + x
                val pA = pixelsA[idx]
                val pB = pixelsB[idx]

                val lumA = ((pA shr 16 and 0xFF) * 299 + (pA shr 8 and 0xFF) * 587 + (pA and 0xFF) * 114) / 1000
                val lumB = ((pB shr 16 and 0xFF) * 299 + (pB shr 8 and 0xFF) * 587 + (pB and 0xFF) * 114) / 1000

                totalDelta += abs(lumA - lumB)
                sampleCount++
            }
        }

        if (sampleCount == 0) return 0f
        val avgPixelDelta = totalDelta.toFloat() / sampleCount
        return (avgPixelDelta / 255.0f).coerceIn(0f, 1f)
    }
}
