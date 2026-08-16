package com.example.audio

import android.media.MediaPlayer
import android.util.Log
import com.example.model.NarrationLanguage
import com.example.model.TtsVoiceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Neural Studio Text-To-Speech Engine using Real Human-Grade Neural Voice Service.
 * Produces hyper-realistic, studio-grade voices with natural breathing, warmth, and cadence.
 */
class NeuralStudioTtsEngine(private val fallbackEngine: TextToSpeechEngine? = null) : TextToSpeechEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var activeVoiceModel: TtsVoiceModel = TtsVoiceModel.KOKORO_HEART
    private var mediaPlayer: MediaPlayer? = null

    private var activeLanguage: NarrationLanguage = NarrationLanguage.INDONESIAN

    fun setLanguage(language: NarrationLanguage) {
        this.activeLanguage = language
    }

    fun setVoiceModel(model: TtsVoiceModel) {
        this.activeVoiceModel = model
    }

    override fun initialize(onReady: (Boolean) -> Unit) {
        fallbackEngine?.initialize { _ -> }
        onReady(true)
    }

    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun speak(text: String, rate: Float, pitch: Float, onDone: (() -> Unit)?) {
        stop()
        scope.launch {
            try {
                val tempFile = File.createTempFile("tts_play_", ".mp3")
                tempFile.deleteOnExit()
                val success = synthesizeToFile(text, tempFile, activeLanguage, rate, pitch)
                if (success && tempFile.exists() && tempFile.length() > 200) {
                    withContext(Dispatchers.Main) {
                        try {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(tempFile.absolutePath)
                                setOnCompletionListener {
                                    onDone?.invoke()
                                    tempFile.delete()
                                }
                                setOnErrorListener { _, _, _ ->
                                    onDone?.invoke()
                                    tempFile.delete()
                                    true
                                }
                                prepare()
                                start()
                            }
                        } catch (e: Exception) {
                            fallbackEngine?.speak(text, rate, pitch, onDone)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        fallbackEngine?.speak(text, rate, pitch, onDone)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    fallbackEngine?.speak(text, rate, pitch, onDone)
                }
            }
        }
    }

    override fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        fallbackEngine?.stop()
    }

    override suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        language: NarrationLanguage,
        rate: Float,
        pitch: Float
    ): Boolean = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext false

        // Select the ultra-realistic neural voice based on language & persona
        val voiceName = getNeuralVoiceName(language, activeVoiceModel)

        // Attempt 1: High-Speed Neural Audio Endpoint
        val synthesized = fetchNeuralTtsAudio(text, voiceName, outputFile, rate, pitch)
        if (synthesized && outputFile.exists() && outputFile.length() > 500) {
            return@withContext true
        }

        // Attempt 2: Natural Multilingual Voice Endpoint
        val langCode = when (language) {
            NarrationLanguage.INDONESIAN -> "id"
            NarrationLanguage.ENGLISH -> "en"
            NarrationLanguage.JAPANESE -> "ja"
            NarrationLanguage.KOREAN -> "ko"
            NarrationLanguage.FRENCH -> "fr"
            NarrationLanguage.GERMAN -> "de"
            NarrationLanguage.SPANISH -> "es"
            NarrationLanguage.PORTUGUESE -> "pt"
            NarrationLanguage.ITALIAN -> "it"
            NarrationLanguage.HINDI -> "hi"
            NarrationLanguage.ARABIC -> "ar"
        }
        val synthesizedV2 = fetchGoogleTranslateNeuralAudio(text, langCode, outputFile)
        if (synthesizedV2 && outputFile.exists() && outputFile.length() > 500) {
            return@withContext true
        }

        // Fallback to local on-device engine
        return@withContext fallbackEngine?.synthesizeToFile(text, outputFile, language, rate, pitch) ?: false
    }

    private fun getNeuralVoiceName(language: NarrationLanguage, model: TtsVoiceModel): String {
        return when (language) {
            NarrationLanguage.INDONESIAN -> {
                when (model) {
                    TtsVoiceModel.KOKORO_HEART,
                    TtsVoiceModel.KOKORO_BELLA,
                    TtsVoiceModel.KOKORO_NICOLE,
                    TtsVoiceModel.VIBE_INSPIRATIONAL -> "id-ID-GadisNeural"
                    TtsVoiceModel.KOKORO_ADAM,
                    TtsVoiceModel.KOKORO_MICHAEL,
                    TtsVoiceModel.VOICEBOX_STUDIO,
                    TtsVoiceModel.VOICEBOX_ARTISAN,
                    TtsVoiceModel.VIBE_ASMR_DRAW,
                    TtsVoiceModel.SYSTEM_OFFLINE -> "id-ID-ArdiNeural"
                }
            }
            NarrationLanguage.JAPANESE -> "ja-JP-NanamiNeural"
            NarrationLanguage.KOREAN -> "ko-KR-SunHiNeural"
            NarrationLanguage.FRENCH -> "fr-FR-DeniseNeural"
            NarrationLanguage.GERMAN -> "de-DE-KatjaNeural"
            NarrationLanguage.SPANISH -> "es-ES-ElviraNeural"
            NarrationLanguage.PORTUGUESE -> "pt-BR-FranciscaNeural"
            NarrationLanguage.ITALIAN -> "it-IT-ElsaNeural"
            NarrationLanguage.HINDI -> "hi-IN-SwaraNeural"
            NarrationLanguage.ARABIC -> "ar-SA-ZariyahNeural"
            NarrationLanguage.ENGLISH -> {
                when (model) {
                    TtsVoiceModel.KOKORO_HEART -> "en-US-JennyNeural"
                    TtsVoiceModel.KOKORO_BELLA -> "en-US-AriaNeural"
                    TtsVoiceModel.KOKORO_NICOLE -> "en-US-SaraNeural"
                    TtsVoiceModel.KOKORO_ADAM -> "en-US-GuyNeural"
                    TtsVoiceModel.KOKORO_MICHAEL -> "en-US-ChristopherNeural"
                    TtsVoiceModel.VOICEBOX_STUDIO -> "en-US-DavisNeural"
                    TtsVoiceModel.VOICEBOX_ARTISAN -> "en-US-TonyNeural"
                    TtsVoiceModel.VIBE_INSPIRATIONAL -> "en-US-JaneNeural"
                    TtsVoiceModel.VIBE_ASMR_DRAW -> "en-US-NancyNeural"
                    TtsVoiceModel.SYSTEM_OFFLINE -> "en-US-JennyNeural"
                }
            }
        }
    }

    private fun fetchNeuralTtsAudio(
        text: String,
        voiceName: String,
        outputFile: File,
        rate: Float,
        pitch: Float
    ): Boolean {
        return try {
            val ratePercent = ((rate - 1.0f) * 100).toInt()
            val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
            val pitchPercent = ((pitch - 1.0f) * 50).toInt()
            val pitchStr = if (pitchPercent >= 0) "+$pitchPercent%" else "$pitchPercent%"

            val escapedText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'><voice name='$voiceName'><prosody rate='$rateStr' pitch='$pitchStr'>$escapedText</prosody></voice></speak>"

            val url = "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EA65400A8929A1054366A0C2"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/ssml+xml")
                .addHeader("X-Microsoft-OutputFormat", "audio-24khz-48kbitrate-mono-mp3")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(ssml.toRequestBody("application/ssml+xml; charset=utf-8".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return outputFile.exists() && outputFile.length() > 500
            }
            false
        } catch (e: Exception) {
            Log.w("NeuralStudioTts", "Endpoint 1 failed: ${e.message}")
            false
        }
    }

    private fun fetchGoogleTranslateNeuralAudio(text: String, lang: String, outputFile: File): Boolean {
        return try {
            val encoded = URLEncoder.encode(text.take(500), "UTF-8")
            val url = "https://translate.google.com/translate_tts?ie=UTF-8&q=$encoded&tl=$lang&client=tw-ob"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return outputFile.exists() && outputFile.length() > 500
            }
            false
        } catch (e: Exception) {
            Log.w("NeuralStudioTts", "Endpoint 2 failed: ${e.message}")
            false
        }
    }

    override fun shutdown() {
        stop()
        fallbackEngine?.shutdown()
    }
}
