package com.example.audio

import com.example.model.NarrationLanguage
import java.io.File

interface TextToSpeechEngine {
    fun initialize(onReady: (Boolean) -> Unit)
    fun speak(text: String, rate: Float = 1.0f, pitch: Float = 1.0f, onDone: (() -> Unit)? = null)
    fun stop()
    suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        language: NarrationLanguage,
        rate: Float = 1.0f,
        pitch: Float = 1.0f
    ): Boolean
    fun shutdown()
}
