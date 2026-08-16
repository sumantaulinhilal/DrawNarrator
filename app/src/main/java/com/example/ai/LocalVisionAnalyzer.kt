package com.example.ai

import com.example.model.ContentCategory
import com.example.model.DetailLevel
import com.example.model.SampledFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min

class LocalVisionAnalyzer : VisionAnalyzer {

    override suspend fun analyzeDrawingProgression(
        keyframes: List<SampledFrame>,
        category: ContentCategory,
        customCategory: String,
        detailLevel: DetailLevel,
        onProgress: (progress: Float) -> Unit
    ): VisionAnalysisResult = withContext(Dispatchers.Default) {
        if (keyframes.isEmpty()) {
            return@withContext VisionAnalysisResult("Unknown Subject", emptyList(), "")
        }

        val subject = resolveSubject(category, customCategory)
        val insights = mutableListOf<VisionStepInsight>()
        val total = keyframes.size

        for (i in keyframes.indices) {
            val frame = keyframes[i]
            val progressRatio = (i.toFloat() / (total - 1).coerceAtLeast(1)).coerceIn(0f, 1f)

            val (action, drawnObject, visualChange, purpose) = determineStepSemantics(
                category = category,
                subject = subject,
                stageProgress = progressRatio,
                density = frame.strokeDensity,
                cx = frame.strokeCenterX,
                cy = frame.strokeCenterY
            )

            insights.add(
                VisionStepInsight(
                    frameIndex = frame.frameIndex,
                    timestampMs = frame.timestampMs,
                    detectedSubject = subject,
                    actionName = action,
                    drawingObject = drawnObject,
                    visualChangeDescription = visualChange,
                    pedagogicalPurpose = purpose,
                    confidence = 0.88f + (0.08f * (1f - (progressRatio - 0.5f) * (progressRatio - 0.5f))),
                    isSignificantStep = true
                )
            )

            onProgress((i + 1).toFloat() / total)
            delay(15) // Brief cooperative yield
        }

        VisionAnalysisResult(
            detectedSubject = subject,
            insights = insights,
            overallTechniqueNotes = "Real-time drawing tutorial with progressive construction-to-detail layering."
        )
    }

    private fun resolveSubject(category: ContentCategory, custom: String): String {
        if (custom.isNotBlank()) return custom
        return when (category) {
            ContentCategory.VEHICLES -> "Sports Car"
            ContentCategory.ANIMALS -> "Wildlife Subject"
            ContentCategory.ANATOMY -> "Human Facial Anatomy"
            ContentCategory.BEGINNER -> "Foundation Object"
            ContentCategory.GENERAL -> "Studio Sketch"
        }
    }

    private fun determineStepSemantics(
        category: ContentCategory,
        subject: String,
        stageProgress: Float,
        density: Float,
        cx: Float,
        cy: Float
    ): StepSemantics {
        return when (category) {
            ContentCategory.VEHICLES -> {
                when {
                    stageProgress < 0.20f -> StepSemantics(
                        action = "Sketch Guidelines",
                        drawnObject = "construction box and horizon line",
                        visualChange = "Establish the baseline perspective grid and overall bounding envelope",
                        purpose = "Lock in vehicle length-to-height proportions before committing to ink"
                    )
                    stageProgress < 0.45f -> StepSemantics(
                        action = "Draw Body Silhouette",
                        drawnObject = "aerodynamic roofline and hood",
                        visualChange = "Main vehicle profile contour is swept from front bumper to rear quarter",
                        purpose = "Define the primary aerodynamic stance and wheelbase anchor points"
                    )
                    stageProgress < 0.65f -> StepSemantics(
                        action = "Place Wheel Arches",
                        drawnObject = "front and rear wheels",
                        visualChange = "Circular wheel housings and tire silhouettes are placed along the ground plane",
                        purpose = "Ground the vehicle and verify perspective foreshortening"
                    )
                    stageProgress < 0.85f -> StepSemantics(
                        action = "Define Glass & Cabin",
                        drawnObject = "windshield and side windows",
                        visualChange = "Window frame pillars, side mirrors, and cabin glass geometry are drawn",
                        purpose = "Add functional structural depth to the upper vehicle mass"
                    )
                    else -> StepSemantics(
                        action = "Render Highlights & Shading",
                        drawnObject = "headlights, intakes, and cast shadow",
                        visualChange = "Aggressive front grill details, sharp light reflections, and ground shadow",
                        purpose = "Add surface contrast, metallic feel, and complete the drawing"
                    )
                }
            }
            ContentCategory.ANIMALS -> {
                when {
                    stageProgress < 0.25f -> StepSemantics(
                        action = "Establish Cranial Form",
                        drawnObject = "head circle and snout guide",
                        visualChange = "Light geometric circles and directional facial axis are drawn",
                        purpose = "Establish the animal's gaze direction and skull mass"
                    )
                    stageProgress < 0.50f -> StepSemantics(
                        action = "Draw Facial Features",
                        drawnObject = "eyes, snout, and ears",
                        visualChange = "Eye sockets, beak or nose bridge, and ear silhouettes are defined",
                        purpose = "Capture the characteristic expression and species anatomy"
                    )
                    stageProgress < 0.75f -> StepSemantics(
                        action = "Block Torso & Limbs",
                        drawnObject = "neck and shoulder contours",
                        visualChange = "Muscular shoulder lines, torso volume, and posture are sketched",
                        purpose = "Anchor the animal's balance and weight distribution"
                    )
                    else -> StepSemantics(
                        action = "Apply Fur & Feather Texture",
                        drawnObject = "fur striations and directional shading",
                        visualChange = "Layered directional hair/feather strokes and core shadow gradients",
                        purpose = "Create organic depth, realistic surface texture, and tactile volume"
                    )
                }
            }
            ContentCategory.ANATOMY -> {
                when {
                    stageProgress < 0.25f -> StepSemantics(
                        action = "Lay Anatomical Landmarks",
                        drawnObject = "orbital guides and brow axis",
                        visualChange = "Light alignment lines dividing facial thirds and eye socket width",
                        purpose = "Ensure symmetry and facial balance from the start"
                    )
                    stageProgress < 0.50f -> StepSemantics(
                        action = "Draw Primary Structure",
                        drawnObject = "eyelid contour and iris sphere",
                        visualChange = "Upper and lower eyelid curves wrap around the spherical eyeball",
                        purpose = "Establish 3D eyelid wrapping and pupil centering"
                    )
                    stageProgress < 0.75f -> StepSemantics(
                        action = "Add Creases & Framing",
                        drawnObject = "eyelid fold and lash baseline",
                        visualChange = "Epicanthic fold, tear duct anatomy, and brow arch are defined",
                        purpose = "Provide depth and anatomical realism to the skin planes"
                    )
                    else -> StepSemantics(
                        action = "Render Volume & Highlights",
                        drawnObject = "iris radial striae and sclera shading",
                        visualChange = "Individual lash curves, light reflection gleams, and skin halftones",
                        purpose = "Deliver lifelike depth, moisture reflection, and contrast"
                    )
                }
            }
            else -> {
                when {
                    stageProgress < 0.25f -> StepSemantics(
                        action = "Block Basic Shapes",
                        drawnObject = "geometric bounding shapes",
                        visualChange = "Initial light scaffolding and placement marks on the canvas",
                        purpose = "Establish scale, margin clearance, and composition"
                    )
                    stageProgress < 0.50f -> StepSemantics(
                        action = "Define Primary Contour",
                        drawnObject = "main subject outline",
                        visualChange = "Continuous confident outer contours delineating the subject",
                        purpose = "Solidify the recognizable shape and boundary"
                    )
                    stageProgress < 0.75f -> StepSemantics(
                        action = "Add Secondary Elements",
                        drawnObject = "internal details and subdivisions",
                        visualChange = "Secondary structural lines and focal characteristics are filled in",
                        purpose = "Enrich the drawing with distinctive internal features"
                    )
                    else -> StepSemantics(
                        action = "Refine & Add Depth",
                        drawnObject = "contrast shading and cleanup",
                        visualChange = "Eraser cleanup, line weight variation, and tonal shadow gradients",
                        purpose = "Add three-dimensional pop and clean presentation"
                    )
                }
            }
        }
    }

    private data class StepSemantics(
        val action: String,
        val drawnObject: String,
        val visualChange: String,
        val purpose: String
    )
}
