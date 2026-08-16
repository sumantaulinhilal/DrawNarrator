package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiVisionAnalyzer
import com.example.ai.LocalVisionAnalyzer
import com.example.ai.ScriptGenerator
import com.example.ai.StepDetector
import com.example.ai.Translator
import com.example.ai.VisionAnalyzer
import com.example.audio.AudioExporter
import com.example.audio.HighFidelityOnDeviceTtsEngine
import com.example.audio.NarrationPlayer
import com.example.audio.NeuralStudioTtsEngine
import com.example.model.AiMode
import com.example.model.AnalysisResult
import com.example.model.ContentCategory
import com.example.model.DetailLevel
import com.example.model.DrawingStep
import com.example.model.NarrationConfig
import com.example.model.NarrationLanguage
import com.example.model.NarrationStyle
import com.example.model.ProcessingStage
import com.example.model.ProcessingState
import com.example.model.TtsVoiceModel
import com.example.model.VideoMetadata
import com.example.processing.ChangeDetector
import com.example.processing.FrameSampler
import com.example.processing.SampleVideoGenerator
import com.example.processing.VideoProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class NarratorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Core Processing & AI engines
    private val videoProcessor = VideoProcessor(context)
    private val frameSampler = FrameSampler(context)
    private val changeDetector = ChangeDetector()
    private val localVisionAnalyzer = LocalVisionAnalyzer()
    private val geminiVisionAnalyzer = GeminiVisionAnalyzer(localVisionAnalyzer)
    private val stepDetector = StepDetector()
    private val scriptGenerator = ScriptGenerator()
    private val translator = Translator()
    private val localTtsEngine = HighFidelityOnDeviceTtsEngine(context)
    private val ttsEngine = NeuralStudioTtsEngine(localTtsEngine)
    val audioExporter = AudioExporter(context)
    val audioPlayer = NarrationPlayer(context)
    private val sampleVideoGenerator = SampleVideoGenerator(context)

    // Observable UI States
    private val _selectedVideo = MutableStateFlow<VideoMetadata?>(null)
    val selectedVideo = _selectedVideo.asStateFlow()

    private val _config = MutableStateFlow(NarrationConfig())
    val config = _config.asStateFlow()

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState = _processingState.asStateFlow()

    private val _currentAnalysisResult = MutableStateFlow<AnalysisResult?>(null)
    val currentAnalysisResult = _currentAnalysisResult.asStateFlow()

    private var processingJob: Job? = null

    init {
        ttsEngine.initialize { /* Ready */ }
    }

    fun updateVoiceModel(voiceModel: TtsVoiceModel) {
        ttsEngine.setVoiceModel(voiceModel)
        localTtsEngine.setVoiceModel(voiceModel)
        _config.value = _config.value.copy(voiceModel = voiceModel)
    }

    fun testCurrentVoiceSample() {
        val sampleText = when (_config.value.language) {
            NarrationLanguage.INDONESIAN -> "Halo! Saya adalah narator AI gambar Anda. Suara ini terdengar sangat alami dan jernih, bukan?"
            NarrationLanguage.ENGLISH -> "Hello! I am your AI drawing tutor narrator. Ready to guide your drawing tutorial step by step!"
            else -> "Hello! I will be narrating your drawing tutorial."
        }
        ttsEngine.speak(
            text = sampleText,
            rate = _config.value.speechRate,
            pitch = _config.value.speechPitch
        )
    }

    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.PREPARING,
                    progress = 0.05f,
                    detailMessage = "Inspecting video format and duration..."
                )
                val metadata = videoProcessor.extractMetadata(uri)
                _selectedVideo.value = metadata
                _processingState.value = ProcessingState.Idle
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error("Failed to read video file: ${e.localizedMessage}", e)
            }
        }
    }

    fun loadPresetDemoVideo(preset: SampleVideoGenerator.SampleDrawingPreset) {
        viewModelScope.launch {
            try {
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.PREPARING,
                    progress = 0.1f,
                    detailMessage = "Loading demo artwork: ${preset.title}..."
                )
                val metadata = sampleVideoGenerator.createSampleVideo(preset)
                _selectedVideo.value = metadata
                _config.value = _config.value.copy(
                    category = preset.category,
                    customCategory = preset.subject
                )
                _processingState.value = ProcessingState.Idle
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error("Failed to load demo video: ${e.localizedMessage}", e)
            }
        }
    }

    fun updateCategory(category: ContentCategory) {
        _config.value = _config.value.copy(category = category, customCategory = "")
    }

    fun updateCustomCategory(custom: String) {
        _config.value = _config.value.copy(customCategory = custom)
    }

    fun updateLanguage(language: NarrationLanguage) {
        ttsEngine.setLanguage(language)
        _config.value = _config.value.copy(language = language)
    }

    fun updateStyle(style: NarrationStyle) {
        _config.value = _config.value.copy(style = style)
    }

    fun updateDetailLevel(detailLevel: DetailLevel) {
        _config.value = _config.value.copy(detailLevel = detailLevel)
    }

    fun updateAiMode(aiMode: AiMode) {
        _config.value = _config.value.copy(aiMode = aiMode)
    }

    fun updateSpeechRate(rate: Float) {
        _config.value = _config.value.copy(speechRate = rate)
    }

    fun startAnalysis() {
        val video = _selectedVideo.value
        if (video == null) {
            Toast.makeText(context, "Please select a drawing tutorial video first", Toast.LENGTH_SHORT).show()
            return
        }

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            val startTimeMs = System.currentTimeMillis()
            try {
                // Stage 1: Preparing video
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.PREPARING,
                    progress = 0.05f,
                    detailMessage = "Validating video streams and canvas perspective..."
                )

                // Stage 2: Intelligent Frame Sampling
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.SAMPLING_FRAMES,
                    progress = 0.10f,
                    detailMessage = "Extracting preview frames at ${_config.value.sampleIntervalSeconds}s interval..."
                )

                val sampledFrames = frameSampler.sampleFrames(
                    metadata = video,
                    intervalSeconds = _config.value.sampleIntervalSeconds
                ) { count, total, prog ->
                    _processingState.value = ProcessingState.InProgress(
                        stage = ProcessingStage.SAMPLING_FRAMES,
                        progress = 0.10f + (0.20f * prog),
                        detailMessage = "Extracted $count / $total video frames for ink analysis",
                        sampledCount = count
                    )
                }

                // Stage 3: Detecting Drawing Changes
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.DETECTING_CHANGES,
                    progress = 0.35f,
                    detailMessage = "Measuring stroke density, spatial clustering, and noise rejection...",
                    sampledCount = sampledFrames.size
                )

                val analyzedFrames = changeDetector.detectChanges(
                    frames = sampledFrames
                ) { prog ->
                    _processingState.value = ProcessingState.InProgress(
                        stage = ProcessingStage.DETECTING_CHANGES,
                        progress = 0.35f + (0.15f * prog),
                        detailMessage = "Classifying continuous strokes and isolating keyframe stages...",
                        sampledCount = sampledFrames.size
                    )
                }

                val keyframes = analyzedFrames.filter { it.isKeyframe }.ifEmpty { analyzedFrames }

                // Stage 4: Vision Analysis & Step Understanding
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.UNDERSTANDING_STEPS,
                    progress = 0.52f,
                    detailMessage = if (_config.value.aiMode == AiMode.REMOTE_GEMINI) {
                        "Analyzing drawing semantics with Multimodal AI..."
                    } else {
                        "Analyzing stroke progression and anatomy with Local Vision Engine..."
                    },
                    sampledCount = sampledFrames.size,
                    keyframeCount = keyframes.size
                )

                val visionAnalyzer: VisionAnalyzer = if (_config.value.aiMode == AiMode.REMOTE_GEMINI) {
                    geminiVisionAnalyzer
                } else {
                    localVisionAnalyzer
                }

                val visionResult = visionAnalyzer.analyzeDrawingProgression(
                    keyframes = keyframes,
                    category = _config.value.category,
                    customCategory = _config.value.customCategory,
                    detailLevel = _config.value.detailLevel
                ) { prog ->
                    _processingState.value = ProcessingState.InProgress(
                        stage = ProcessingStage.UNDERSTANDING_STEPS,
                        progress = 0.52f + (0.20f * prog),
                        detailMessage = "Categorizing actions: form construction, contours, and shading...",
                        sampledCount = sampledFrames.size,
                        keyframeCount = keyframes.size
                    )
                }

                // Stage 5: Step Detection & Script Generation
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.GENERATING_NARRATION,
                    progress = 0.75f,
                    detailMessage = "Writing synchronized educational narration in ${_config.value.style.displayName} style...",
                    sampledCount = sampledFrames.size,
                    keyframeCount = keyframes.size,
                    stepsCount = visionResult.insights.size
                )

                val initialSteps = stepDetector.createDrawingSteps(video, visionResult, keyframes)
                val scriptedSteps = scriptGenerator.generateScriptForSteps(
                    steps = initialSteps,
                    subject = visionResult.detectedSubject,
                    config = _config.value
                )

                // Translate if target language is not English
                val finalSteps = translator.translateSteps(scriptedSteps, _config.value.language)

                // Build full unified script
                val fullScript = finalSteps.joinToString("\n\n") { "Step ${it.stepNumber}: ${it.narrationText}" }

                // Stage 6: Voice Synthesis
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.SYNTHESIZING_VOICE,
                    progress = 0.88f,
                    detailMessage = "Synthesizing educational spoken audio with voice engine...",
                    sampledCount = sampledFrames.size,
                    keyframeCount = keyframes.size,
                    stepsCount = finalSteps.size
                )

                val audioFile = File(context.cacheDir, "narration_master_${System.currentTimeMillis()}.wav")
                val unifiedSpeechText = finalSteps.joinToString(". ... ") { it.narrationText }

                ttsEngine.synthesizeToFile(
                    text = unifiedSpeechText,
                    outputFile = audioFile,
                    language = _config.value.language,
                    rate = _config.value.speechRate,
                    pitch = _config.value.speechPitch
                )

                // Stage 7: Exporting and finalizing results
                _processingState.value = ProcessingState.InProgress(
                    stage = ProcessingStage.EXPORTING_FILES,
                    progress = 0.98f,
                    detailMessage = "Finalizing subtitles (SRT) and analysis metadata...",
                    sampledCount = sampledFrames.size,
                    keyframeCount = keyframes.size,
                    stepsCount = finalSteps.size
                )

                val srtFile = audioExporter.generateSrtSubtitles(
                    AnalysisResult(
                        videoMetadata = video,
                        config = _config.value,
                        detectedSubject = visionResult.detectedSubject,
                        steps = finalSteps,
                        fullScript = fullScript,
                        srtSubtitles = "",
                        audioFile = audioFile,
                        audioDurationMs = video.durationMs,
                        processingDurationMs = System.currentTimeMillis() - startTimeMs,
                        keyframesCount = keyframes.size,
                        totalFramesSampled = sampledFrames.size
                    )
                )
                val srtContent = srtFile.readText()

                val result = AnalysisResult(
                    videoMetadata = video,
                    config = _config.value,
                    detectedSubject = visionResult.detectedSubject,
                    steps = finalSteps,
                    fullScript = fullScript,
                    srtSubtitles = srtContent,
                    audioFile = audioFile,
                    audioDurationMs = (finalSteps.lastOrNull()?.endTimestampMs ?: video.durationMs),
                    processingDurationMs = System.currentTimeMillis() - startTimeMs,
                    keyframesCount = keyframes.size,
                    totalFramesSampled = sampledFrames.size
                )

                _currentAnalysisResult.value = result
                _processingState.value = ProcessingState.Success(result)

                // Load synthesized audio into narration player for immediate review
                if (audioFile.exists() && audioFile.length() > 0) {
                    audioPlayer.loadAudio(audioFile)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _processingState.value = ProcessingState.Error(
                    errorMessage = "Analysis encountered an issue: ${e.localizedMessage ?: e.javaClass.simpleName}",
                    throwable = e
                )
            }
        }
    }

    fun cancelAnalysis() {
        processingJob?.cancel()
        _processingState.value = ProcessingState.Idle
    }

    fun updateStepNarration(stepNumber: Int, newText: String) {
        val current = _currentAnalysisResult.value ?: return
        val updatedList = current.steps.map {
            if (it.stepNumber == stepNumber) it.copy(narrationText = newText) else it
        }
        val updatedScript = updatedList.joinToString("\n\n") { "Step ${it.stepNumber}: ${it.narrationText}" }
        _currentAnalysisResult.value = current.copy(
            steps = updatedList,
            fullScript = updatedScript
        )
    }

    fun speakStep(step: DrawingStep) {
        ttsEngine.speak(
            text = step.narrationText,
            rate = _config.value.speechRate,
            pitch = _config.value.speechPitch
        )
    }

    fun stopSpeaking() {
        ttsEngine.stop()
    }

    fun resetToNewVideo() {
        audioPlayer.release()
        ttsEngine.stop()
        _currentAnalysisResult.value = null
        _processingState.value = ProcessingState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        ttsEngine.shutdown()
    }
}
