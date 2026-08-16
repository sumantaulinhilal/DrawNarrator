package com.example.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.AnalysisResult
import com.example.model.DrawingStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class AudioExporter(private val context: Context) {

    /**
     * Exports narration script to formatted plain text (.txt)
     */
    suspend fun generateScriptTxt(result: AnalysisResult): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "narration_script_${System.currentTimeMillis()}.txt")

        val sb = StringBuilder()
        sb.append("========================================\n")
        sb.append(" DRAWING TUTORIAL NARRATION SCRIPT\n")
        sb.append("========================================\n\n")
        sb.append("Subject: ${result.detectedSubject}\n")
        sb.append("Video: ${result.videoMetadata.fileName} (${result.videoMetadata.durationFormatted})\n")
        sb.append("Category: ${result.config.effectiveCategoryName}\n")
        sb.append("Narration Style: ${result.config.style.displayName}\n")
        sb.append("Language: ${result.config.language.displayName}\n")
        sb.append("Total Steps: ${result.steps.size}\n\n")
        sb.append("----------------------------------------\n\n")

        for (step in result.steps) {
            sb.append("STEP ${String.format("%02d", step.stepNumber)} [${step.timeRangeFormatted}]\n")
            sb.append("Action: ${step.action} (${step.drawingObject})\n")
            sb.append("Visual Focus: ${step.visualChange}\n")
            sb.append("Pedagogical Purpose: ${step.purpose}\n\n")
            sb.append("NARRATION:\n\"${step.narrationText}\"\n\n")
            sb.append("----------------------------------------\n\n")
        }

        FileOutputStream(file).use { it.write(sb.toString().toByteArray()) }
        file
    }

    /**
     * Exports subtitles in standard SubRip (.srt) format with millisecond precision
     */
    suspend fun generateSrtSubtitles(result: AnalysisResult): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "narration_${System.currentTimeMillis()}.srt")

        val sb = StringBuilder()
        for (step in result.steps) {
            sb.append("${step.stepNumber}\n")
            val startSrt = formatSrtTimestamp(step.startTimestampMs)
            val endSrt = formatSrtTimestamp(step.endTimestampMs)
            sb.append("$startSrt --> $endSrt\n")
            sb.append("${step.narrationText}\n\n")
        }

        FileOutputStream(file).use { it.write(sb.toString().toByteArray()) }
        file
    }

    /**
     * Exports complete step analysis to structured JSON (.json)
     */
    suspend fun generateAnalysisJson(result: AnalysisResult): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "analysis_${System.currentTimeMillis()}.json")

        val root = JSONObject().apply {
            put("app", "Drawing Tutorial Narrator AI")
            put("version", "1.0")
            put("detected_subject", result.detectedSubject)
            put("category", result.config.effectiveCategoryName)
            put("narration_style", result.config.style.displayName)
            put("language", result.config.language.code)
            put("video_duration_ms", result.videoMetadata.durationMs)
            put("audio_duration_ms", result.audioDurationMs)

            val stepsArray = JSONArray()
            for (s in result.steps) {
                val stepObj = JSONObject().apply {
                    put("step_number", s.stepNumber)
                    put("start_timestamp_ms", s.startTimestampMs)
                    put("end_timestamp_ms", s.endTimestampMs)
                    put("start_timestamp", s.startFormatted)
                    put("end_timestamp", s.endFormatted)
                    put("action", s.action)
                    put("drawing_object", s.drawingObject)
                    put("visual_change", s.visualChange)
                    put("purpose", s.purpose)
                    put("confidence", s.confidence.toDouble())
                    put("narration_text", s.narrationText)
                }
                stepsArray.put(stepObj)
            }
            put("steps", stepsArray)
        }

        FileOutputStream(file).use { it.write(root.toString(2).toByteArray()) }
        file
    }

    fun shareFile(file: File, mimeType: String, chooserTitle: String) {
        try {
            val uri: Uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatSrtTimestamp(millis: Long): String {
        val hours = millis / (3600 * 1000)
        val rem1 = millis % (3600 * 1000)
        val minutes = rem1 / (60 * 1000)
        val rem2 = rem1 % (60 * 1000)
        val seconds = rem2 / 1000
        val ms = rem2 % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, ms)
    }
}
