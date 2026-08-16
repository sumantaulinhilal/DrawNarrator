# 🎨 Drawing Tutorial Narrator AI

An intelligent Android application built with Jetpack Compose and Gemini AI that analyzes drawing tutorial videos, detects drawing steps, generates educational narration scripts, and produces synchronized audio narration.

---

## ✨ Features

- **Video Ingestion & Frame Analysis**: Extract key drawing progression frames from local videos or built-in demo tutorials.
- **AI-Powered Step Detection**: Automatically identifies drawing techniques (outlines, shading, color layering, details).
- **Educational Narration Generation**: Generates instructional scripts tailored for beginners and art learners.
- **Text-to-Speech (TTS) Integration**: Produces synchronized voice narration with customizable voices, pitch, and speed.
- **Modern Jetpack Compose UI**: Designed with Material 3, fluid animations, and dark/light theme support.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) + StateFlow
- **AI Engine**: Google Gemini API (Multimodal Video & Vision Analysis)
- **Local Persistence**: Jetpack Room Database
- **Media & Audio**: Android TextToSpeech & MediaMetadataRetriever

---

## 🚀 Getting Started

1. Clone this repository:
   ```bash
   git clone https://github.com/sumantaulinhilal/DrawNarrator.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Add your Gemini API key in `BuildConfig` or through the AI Studio Secrets panel.
4. Sync Gradle and run the app on an Android device or emulator (API 26+).
