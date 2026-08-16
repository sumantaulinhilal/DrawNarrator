package com.example.model

sealed interface ProcessingState {
    data object Idle : ProcessingState

    data class InProgress(
        val stage: ProcessingStage,
        val progress: Float, // 0.0 to 1.0
        val detailMessage: String,
        val sampledCount: Int = 0,
        val keyframeCount: Int = 0,
        val stepsCount: Int = 0
    ) : ProcessingState

    data class Success(val result: AnalysisResult) : ProcessingState

    data class Error(val errorMessage: String, val throwable: Throwable? = null) : ProcessingState
}

enum class ProcessingStage(val title: String, val description: String) {
    PREPARING("Preparing Video", "Reading file metadata and validating media tracks"),
    SAMPLING_FRAMES("Sampling Frames", "Extracting high-efficiency preview frames at intelligent intervals"),
    DETECTING_CHANGES("Detecting Drawing Changes", "Computing perceptual difference and stroke density deltas"),
    UNDERSTANDING_STEPS("Understanding Drawing Steps", "Classifying drawing phases, actions, and subject anatomy"),
    GENERATING_NARRATION("Generating Educational Script", "Crafting synchronized human-like drawing commentary"),
    SYNTHESIZING_VOICE("Generating Voice Audio", "Synthesizing spoken narration with the audio engine"),
    EXPORTING_FILES("Exporting Narration", "Assembling audio master and subtitle packages")
}
