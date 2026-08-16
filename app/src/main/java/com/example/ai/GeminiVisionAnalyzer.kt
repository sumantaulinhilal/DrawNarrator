package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.model.ContentCategory
import com.example.model.DetailLevel
import com.example.model.SampledFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiVisionAnalyzer(
    private val localFallback: LocalVisionAnalyzer = LocalVisionAnalyzer()
) : VisionAnalyzer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeDrawingProgression(
        keyframes: List<SampledFrame>,
        category: ContentCategory,
        customCategory: String,
        detailLevel: DetailLevel,
        onProgress: (progress: Float) -> Unit
    ): VisionAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // No custom remote API key configured, use the high-fidelity local engine
            return@withContext localFallback.analyzeDrawingProgression(
                keyframes, category, customCategory, detailLevel, onProgress
            )
        }

        try {
            onProgress(0.1f)
            // Limit to max 6 keyframes to avoid oversized payload
            val selectedKeyframes = if (keyframes.size > 6) {
                val step = keyframes.size / 6.0
                (0 until 6).map { keyframes[(it * step).toInt().coerceAtMost(keyframes.size - 1)] }
            } else {
                keyframes
            }

            val partsArray = JSONArray()

            // System prompt
            val promptText = """
                You are an expert drawing instructor analyzing a real-time drawing tutorial video.
                Category: ${if (customCategory.isNotBlank()) customCategory else category.displayName}
                Detail Level: ${detailLevel.displayName}

                Review the ${selectedKeyframes.size} sequential keyframes showing the progression of the artwork.
                For each stage:
                1. Identify the specific subject being drawn (e.g. Sports Car, Eagle, Human Eye).
                2. Explain what changed in each frame relative to previous.
                3. State the action name (e.g. "Establish Basic Proportions", "Draw Main Outline", "Add Wheels", "Refine Contours", "Add Shading").
                4. State the drawn object / element.
                5. Explain the pedagogical purpose of this step.

                Return a JSON object with this EXACT structure:
                {
                  "detectedSubject": "string",
                  "steps": [
                    {
                      "stepIndex": 0,
                      "action": "string",
                      "drawnObject": "string",
                      "visualChange": "string",
                      "purpose": "string",
                      "confidence": 0.95
                    }
                  ]
                }
            """.trimIndent()

            partsArray.put(JSONObject().put("text", promptText))

            // Add images
            for ((idx, frame) in selectedKeyframes.withIndex()) {
                val bmp = frame.bitmap
                if (bmp != null) {
                    val base64 = bitmapToBase64(bmp)
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64)
                    }
                    partsArray.put(JSONObject().put("inlineData", inlineData))
                }
                onProgress(0.1f + (0.4f * (idx + 1) / selectedKeyframes.size))
            }

            val requestBodyJson = JSONObject().apply {
                val contents = JSONArray().put(JSONObject().put("parts", partsArray))
                put("contents", contents)
                val genConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            onProgress(0.8f)

            if (!response.isSuccessful) {
                return@withContext localFallback.analyzeDrawingProgression(
                    keyframes, category, customCategory, detailLevel, onProgress
                )
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val candidateText = jsonResponse
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val parsedResult = JSONObject(candidateText)
            val detectedSubject = parsedResult.optString("detectedSubject", category.displayName)
            val stepsArray = parsedResult.optJSONArray("steps") ?: JSONArray()

            val insights = mutableListOf<VisionStepInsight>()
            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.getJSONObject(i)
                val matchingFrame = selectedKeyframes.getOrNull(i) ?: keyframes.getOrNull(i) ?: keyframes.last()
                insights.add(
                    VisionStepInsight(
                        frameIndex = matchingFrame.frameIndex,
                        timestampMs = matchingFrame.timestampMs,
                        detectedSubject = detectedSubject,
                        actionName = stepObj.optString("action", "Draw element"),
                        drawingObject = stepObj.optString("drawnObject", "drawing element"),
                        visualChangeDescription = stepObj.optString("visualChange", "Element is drawn on canvas"),
                        pedagogicalPurpose = stepObj.optString("purpose", "Develop structure and visual clarity"),
                        confidence = stepObj.optDouble("confidence", 0.92).toFloat(),
                        isSignificantStep = true
                    )
                )
            }

            onProgress(1.0f)
            VisionAnalysisResult(
                detectedSubject = detectedSubject,
                insights = insights,
                overallTechniqueNotes = "Multimodal AI step detection synchronized with drawing keyframes."
            )
        } catch (_: Exception) {
            localFallback.analyzeDrawingProgression(keyframes, category, customCategory, detailLevel, onProgress)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        // Resize for fast API transmission
        val scaled = if (bitmap.width > 512 || bitmap.height > 512) {
            val scale = 512f / Math.max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
