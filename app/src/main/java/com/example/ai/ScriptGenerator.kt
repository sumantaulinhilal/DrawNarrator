package com.example.ai

import com.example.model.DetailLevel
import com.example.model.DrawingStep
import com.example.model.NarrationConfig
import com.example.model.NarrationStyle

class ScriptGenerator {

    /**
     * Generates a synchronized human-like drawing tutorial script for each step.
     */
    fun generateScriptForSteps(
        steps: List<DrawingStep>,
        subject: String,
        config: NarrationConfig
    ): List<DrawingStep> {
        val totalSteps = steps.size
        return steps.mapIndexed { index, step ->
            val narration = createStepNarration(
                step = step,
                stepIndex = index,
                totalSteps = totalSteps,
                subject = subject,
                style = config.style,
                detailLevel = config.detailLevel
            )
            step.copy(narrationText = narration)
        }
    }

    private fun createStepNarration(
        step: DrawingStep,
        stepIndex: Int,
        totalSteps: Int,
        subject: String,
        style: NarrationStyle,
        detailLevel: DetailLevel
    ): String {
        val durationSec = step.durationSeconds
        val isFirst = stepIndex == 0
        val isLast = stepIndex == totalSteps - 1

        val opener = getNaturalTransitionOpener(stepIndex, totalSteps, style)

        return when (style) {
            NarrationStyle.EDUCATIONAL -> {
                buildEducationalSentence(opener, step, subject, isFirst, isLast, durationSec, detailLevel)
            }
            NarrationStyle.FRIENDLY -> {
                buildFriendlySentence(opener, step, subject, isFirst, isLast, durationSec, detailLevel)
            }
            NarrationStyle.PROFESSIONAL -> {
                buildProfessionalSentence(opener, step, subject, isFirst, isLast, durationSec, detailLevel)
            }
            NarrationStyle.CONCISE -> {
                buildConciseSentence(step, subject, isFirst, isLast)
            }
        }
    }

    private fun getNaturalTransitionOpener(index: Int, total: Int, style: NarrationStyle): String {
        if (index == 0) {
            return when (style) {
                NarrationStyle.FRIENDLY -> "Let's kick things off by"
                NarrationStyle.PROFESSIONAL -> "Begin by"
                NarrationStyle.CONCISE -> "First,"
                NarrationStyle.EDUCATIONAL -> "Let's begin by"
            }
        }
        if (index == total - 1) {
            return when (style) {
                NarrationStyle.FRIENDLY -> "To wrap up this drawing,"
                NarrationStyle.PROFESSIONAL -> "Finally, finalize the composition by"
                NarrationStyle.CONCISE -> "Finally,"
                NarrationStyle.EDUCATIONAL -> "To finish up,"
            }
        }

        val educationalOpeners = listOf(
            "Moving forward,",
            "With the foundation set,",
            "Now focus on",
            "Working into the form,",
            "Notice how the artist",
            "Carefully",
            "Building directly on that,"
        )
        return educationalOpeners[index % educationalOpeners.size]
    }

    private fun buildEducationalSentence(
        opener: String,
        step: DrawingStep,
        subject: String,
        isFirst: Boolean,
        isLast: Boolean,
        durationSec: Float,
        detailLevel: DetailLevel
    ): String {
        val action = step.action.lowercase()
        val drawnObj = step.drawingObject
        val purpose = step.purpose.lowercase()

        val base = if (isFirst) {
            "$opener sketching ${step.drawingObject} to $purpose. Keep your pencil pressure light so adjustments remain effortless."
        } else if (isLast) {
            "$opener add ${step.drawingObject}. Use directional contrast to $purpose and give the $subject a striking final finish."
        } else {
            "$opener ${action.replace("draw", "sketch").replace("place", "position")} ${drawnObj}. This step is crucial to $purpose."
        }

        // If the step is longer in duration or high detail requested, expand with pedagogical guidance
        if (durationSec > 10f || detailLevel == DetailLevel.COMPREHENSIVE) {
            val guidanceTip = getPedagogicalTip(step.action, drawnObj)
            return "$base $guidanceTip"
        }

        return base
    }

    private fun buildFriendlySentence(
        opener: String,
        step: DrawingStep,
        subject: String,
        isFirst: Boolean,
        isLast: Boolean,
        durationSec: Float,
        detailLevel: DetailLevel
    ): String {
        val drawnObj = step.drawingObject
        return if (isFirst) {
            "$opener laying down a gentle guide for the $drawnObj. Don't worry about perfection just yet—we're capturing the energy and shape."
        } else if (isLast) {
            "$opener bringing in some crisp details on the $drawnObj. Take a step back and admire how the $subject has come together!"
        } else {
            "$opener working in the $drawnObj. See how nicely this anchors into our previous lines to ${step.purpose.lowercase()}."
        }
    }

    private fun buildProfessionalSentence(
        opener: String,
        step: DrawingStep,
        subject: String,
        isFirst: Boolean,
        isLast: Boolean,
        durationSec: Float,
        detailLevel: DetailLevel
    ): String {
        val drawnObj = step.drawingObject
        return if (isFirst) {
            "$opener establishing primary construction axes for the $drawnObj to ${step.purpose.lowercase()}."
        } else if (isLast) {
            "$opener executing final surface rendering across the $drawnObj, establishing tonal value hierarchy."
        } else {
            "$opener delineating the $drawnObj, ensuring planar alignment to ${step.purpose.lowercase()}."
        }
    }

    private fun buildConciseSentence(
        step: DrawingStep,
        subject: String,
        isFirst: Boolean,
        isLast: Boolean
    ): String {
        return "${step.action} for the ${step.drawingObject} to ${step.purpose.lowercase()}."
    }

    private fun getPedagogicalTip(action: String, drawingObject: String): String {
        val lower = (action + " " + drawingObject).lowercase()
        return when {
            "wheel" in lower || "circle" in lower ->
                "Pay close attention to the ellipse angle so the wheels stay grounded on the same perspective plane."
            "line" in lower || "guide" in lower || "proportion" in lower ->
                "Take your time measuring proportions with your eye before darkening any contours."
            "window" in lower || "glass" in lower || "cabin" in lower ->
                "Follow the slant of the roofline to keep the windshield and side pillars in proper scale."
            "shadow" in lower || "shading" in lower || "highlight" in lower ->
                "Vary your line weight here—thicker lines on the underside will instantly create weight and depth."
            "eye" in lower || "face" in lower || "head" in lower ->
                "Check the curvature of the brow and eye axis to maintain natural symmetry."
            "feather" in lower || "fur" in lower || "texture" in lower ->
                "Follow the anatomical flow with quick, rhythmic strokes rather than rigid straight lines."
            else ->
                "Maintain steady, relaxed wrist movements for smooth and confident line quality."
        }
    }
}
