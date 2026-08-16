package com.example.model

enum class ContentCategory(val displayName: String, val description: String) {
    VEHICLES("Vehicles", "Cars, trucks, planes, bikes, and mechanical designs"),
    ANIMALS("Animals", "Mammals, birds, reptiles, and wildlife anatomy"),
    ANATOMY("Drawing Anatomy", "Human figures, proportions, faces, hands, and poses"),
    BEGINNER("Beginner Drawing", "Basic shapes, simple sketches, and foundation skills"),
    GENERAL("General Art", "Landscapes, architecture, objects, and still life");

    companion object {
        fun fromDisplayName(name: String): ContentCategory =
            entries.find { it.displayName.equals(name, ignoreCase = true) } ?: GENERAL
    }
}

enum class NarrationLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    SPANISH("es", "Spanish", "Español"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    ITALIAN("it", "Italian", "Italiano"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    HINDI("hi", "Hindi", "हिन्दी"),
    ARABIC("ar", "Arabic", "العربية")
}

enum class NarrationStyle(val displayName: String, val description: String) {
    EDUCATIONAL("Educational", "Pedagogical, clear instruction explaining the 'why' and 'how'"),
    FRIENDLY("Friendly", "Warm, encouraging, and approachable studio tone"),
    PROFESSIONAL("Professional", "Precise, structured, and technically accurate"),
    CONCISE("Concise", "Direct, punchy steps with zero unnecessary filler")
}

enum class DetailLevel(val displayName: String, val description: String) {
    BRIEF("Brief", "High-level summary of major movements"),
    MEDIUM("Medium", "Balanced pacing with helpful drawing tips"),
    COMPREHENSIVE("Comprehensive", "In-depth breakdown with perspective and pencil technique notes")
}

enum class AiMode(val displayName: String, val description: String) {
    LOCAL_VISION("Local AI / Computer Vision", "Fast, private on-device stroke analysis (No API key needed)"),
    REMOTE_GEMINI("Remote Multimodal AI (Gemini)", "Deep multimodal semantic understanding with Gemini API")
}

data class NarrationConfig(
    val category: ContentCategory = ContentCategory.VEHICLES,
    val customCategory: String = "",
    val language: NarrationLanguage = NarrationLanguage.ENGLISH,
    val style: NarrationStyle = NarrationStyle.EDUCATIONAL,
    val detailLevel: DetailLevel = DetailLevel.MEDIUM,
    val aiMode: AiMode = AiMode.LOCAL_VISION,
    val voiceModel: TtsVoiceModel = TtsVoiceModel.KOKORO_HEART,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val sampleIntervalSeconds: Float = 1.0f
) {
    val effectiveCategoryName: String
        get() = if (customCategory.isNotBlank()) customCategory else category.displayName
}
