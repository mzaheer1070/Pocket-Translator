# 🌐 Translator (Offline Neural Machine Translation)

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%28M3%29-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![ML Kit](https://img.shields.io/badge/AI-Google%20ML%20Kit-EA4335.svg?style=flat&logo=google)](https://developers.google.com/ml-kit)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue.svg)](https://developer.android.com/about/dashboards)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-brightgreen.svg)](https://developer.android.com)

A modern, privacy-focused Android translation app built with **Jetpack Compose**, **Material 3**, and **Google ML Kit**. Delivers instant on-device neural translation, offline language model management, native speech-to-text dictation, and text-to-speech (TTS) audio pronunciation with zero cloud API keys or external server dependencies.

---

## ✨ Features

- **⚡ On-Device Neural Machine Translation**:
  - Powered by Google ML Kit's sequence-to-sequence neural network models (~30 MB per language).
  - Translates in milliseconds with 100% offline capability once models are downloaded.
  - Zero API subscription fees, zero cloud latency, and 100% privacy.

- **📦 Offline Language Model Manager**:
  - Dedicated dialog to view, pre-download, and delete language packs.
  - Monitor local storage usage and reclaim device space anytime.
  - Clear visual indicators showing which languages are downloaded and offline-ready.

- **🔍 Automatic Real-Time Language Detection**:
  - Automatically identifies the typed or spoken language using ML Kit Language Identification.
  - Offers a one-tap smart chip to switch the translation direction dynamically.

- **🔊 Text-to-Speech (TTS) Pronunciation**:
  - Built-in speech engine with native audio pronunciation for both source and translated text.
  - Supports locale-specific accents across 12+ international languages.

- **🎙️ Voice Speech-to-Text Dictation**:
  - Tap the microphone to dictate phrases directly into the translator.
  - Seamlessly handles runtime audio permissions and IME soft keyboard resize.

- **🔄 Bidirectional Quick-Swap (⇄)**:
  - Instant one-tap button to flip source and target languages and invert the text flow.

- **📱 Adaptive & Responsive Design**:
  - **Phone Mode**: Compact single-column card layout with smooth scrolling and keyboard insets (`imePadding`).
  - **Tablet / Foldable Mode**: Dual-pane side-by-side workspace taking full advantage of wide screens.

- **📋 Productivity Utilities**:
  - Quick **Paste**, **Clear**, **Copy to Clipboard**, and **Native Android Share Sheet** integration.

---

## 🌍 Supported Languages

| Language | Flag | ML Kit Code | Offline Support |
| :--- | :---: | :---: | :---: |
| **English** | 🇺🇸 | `en` | Included |
| **Spanish** | 🇪🇸 | `es` | ✅ Downloadable |
| **French** | 🇫🇷 | `fr` | ✅ Downloadable |
| **German** | 🇩🇪 | `de` | ✅ Downloadable |
| **Italian** | 🇮🇹 | `it` | ✅ Downloadable |
| **Chinese** | 🇨🇳 | `zh` | ✅ Downloadable |
| **Japanese** | 🇯🇵 | `ja` | ✅ Downloadable |
| **Arabic** | 🇸🇦 | `ar` | ✅ Downloadable |
| **Hindi** | 🇮🇳 | `hi` | ✅ Downloadable |
| **Korean** | 🇰🇷 | `ko` | ✅ Downloadable |
| **Russian** | 🇷🇺 | `ru` | ✅ Downloadable |
| **Portuguese** | 🇵🇹 | `pt` | ✅ Downloadable |
| **Dutch** | 🇳🇱 | `nl` | ✅ Downloadable |

---

## 🛠️ Tech Stack & Architecture

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material Design 3 (M3)](https://m3.material.io/)
- **Architecture**: MVVM (Model-View-ViewModel) with unidirectional data flow (UDF)
- **State Management**: Kotlin Coroutines & `StateFlow`
- **Machine Learning**:
  - Google ML Kit Translate (`com.google.mlkit:translate`)
  - Google ML Kit Language Identification (`com.google.mlkit:language-id`)
- **Speech & Audio**:
  - Android Speech Recognizer (`RecognizerIntent`)
  - Android `TextToSpeech` Engine
- **Build System**: Gradle Kotlin DSL (`.gradle.kts`) with Version Catalog (`libs.versions.toml`)
- **Unit & UI Testing**: JUnit 4, Robolectric, Roborazzi

---

## 📂 Project Structure

```
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   └── LanguageOption.kt        # Supported languages & locale mapping
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/                   # Material 3 Color, Type, Theme
│   │   │   │   │   ├── TtsManager.kt            # Text-to-Speech pronunciation engine
│   │   │   │   │   ├── TranslationViewModel.kt  # StateFlow, ML Kit calls & model manager
│   │   │   │   │   └── TranslatorScreen.kt      # Adaptive Compose UI (Phone & Tablet)
│   │   │   │   └── MainActivity.kt              # Entry point & edge-to-edge setup
│   │   │   ├── res/                             # Vector drawables, adaptive launcher icons, strings
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/example/
│   │           └── TranslationViewModelTest.kt  # Robolectric unit tests
│   └── build.gradle.kts                         # App-level dependencies & build rules
├── gradle/
│   └── libs.versions.toml                       # Centralized Version Catalog
├── metadata.json                                # Platform metadata
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala / Ladybug or newer recommended)
- **Android SDK**: Min SDK 24, Target SDK 36
- **JDK**: Java 17 or Java 21

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/translator-android.git
   cd translator-android
   ```

2. **Open the project** in Android Studio.

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

5. **Install on device / emulator:**
   ```bash
   ./gradlew installDebug
   ```

---

## 🔒 Privacy & Permissions

- **`android.permission.INTERNET`**: Used strictly for initially downloading requested ML Kit language models from Google's on-device AI model repository. No user translation text is ever sent to any remote server.
- **`android.permission.RECORD_AUDIO`**: Requested at runtime only when the user taps the microphone button to dictate speech. Audio is processed directly by the device's speech engine.

---

## 📄 License

This project is licensed under the [Apache 2.0 License](LICENSE).
