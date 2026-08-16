package com.example.ai

import com.example.model.ContentCategory
import com.example.model.DetailLevel
import com.example.model.SampledFrame

data class VisionStepInsight(
    val frameIndex: Int,
    val timestampMs: Long,
    val detectedSubject: String,
    val actionName: String,
    val drawingObject: String,
    val visualChangeDescription: String,
    val pedagogicalPurpose: String,
    val confidence: Float,
    val isSignificantStep: Boolean
)

data class VisionAnalysisResult(
    val detectedSubject: String,
    val insights: List<VisionStepInsight>,
    val overallTechniqueNotes: String
)

interface VisionAnalyzer {
    suspend fun analyzeDrawingProgression(
        keyframes: List<SampledFrame>,
        category: ContentCategory,
        customCategory: String,
        detailLevel: DetailLevel,
        onProgress: (progress: Float) -> Unit
    ): VisionAnalysisResult
}
