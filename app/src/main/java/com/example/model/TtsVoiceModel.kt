package com.example.model

enum class TtsVoiceModel(
    val id: String,
    val displayName: String,
    val engineName: String,
    val description: String,
    val gender: String,
    val accent: String
) {
    // Kokoro-82M Neural On-Device Presets
    KOKORO_HEART("kokoro_af_heart", "Kokoro Heart (Warm)", "Kokoro Neural 82M", "Gentle, studio-quality warm educator voice (On-Device)", "Female", "US Natural"),
    KOKORO_BELLA("kokoro_af_bella", "Kokoro Bella (Crisp)", "Kokoro Neural 82M", "Bright, clear, instructional art tutor voice (On-Device)", "Female", "US Natural"),
    KOKORO_NICOLE("kokoro_af_nicole", "Kokoro Nicole (Calm)", "Kokoro Neural 82M", "Relaxed, paced voice for intricate drawing tutorials", "Female", "US Soft"),
    KOKORO_ADAM("kokoro_am_adam", "Kokoro Adam (Deep)", "Kokoro Neural 82M", "Resonant, structured voice for anatomy & perspective", "Male", "US Deep"),
    KOKORO_MICHAEL("kokoro_am_michael", "Kokoro Michael (Pro)", "Kokoro Neural 82M", "Energetic, engaging art instructor", "Male", "US Dynamic"),
    
    // Voicebox Neural Acoustic Model Presets
    VOICEBOX_STUDIO("voicebox_studio", "Voicebox Studio HD", "Voicebox Flow-Matching", "Zero-shot guided acoustic voice (Zero API required)", "Adaptive", "Studio Neutral"),
    VOICEBOX_ARTISAN("voicebox_artisan", "Voicebox Artisan", "Voicebox Flow-Matching", "Rich expressive cadence with dynamic inflection", "Neutral", "Pro Master"),
    
    // Vibe Voice Emotional & Acoustic Presets
    VIBE_INSPIRATIONAL("vibe_inspire", "Vibe Voice (Inspire)", "Vibe Acoustic Neural", "Expressive, motivating voice for sketching encouragement", "Dynamic", "Expressive"),
    VIBE_ASMR_DRAW("vibe_asmr", "Vibe Voice (Gentle/Chill)", "Vibe Acoustic Neural", "Soothing, whisper-smooth guidance for relaxing drawing", "Soft", "Binaural Soft"),
    
    // System Native Local Engine
    SYSTEM_OFFLINE("system_native", "Android Speech Engine", "Android Local TTS", "Standard on-device system speech synthesizer", "System", "System Default");

    val isNeuralOnDevice: Boolean
        get() = this != SYSTEM_OFFLINE
}
