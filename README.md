<div align="center">

# 🧠 PocketAI

**AI-Powered Receipt Scanner & Expense Tracker**

*Scan receipts. Extract data with on-device AI. Track spending — all without cloud dependency.*

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 📸 **Smart Receipt Scanner** | ML Kit Document Scanner with auto edge-detection and perspective correction |
| 🤖 **On-Device AI** | Qwen3.5-VL 0.8B multimodal LLM runs entirely on your phone via llama.cpp |
| 📊 **Expense Analytics** | Interactive charts — weekly trends, category breakdowns, budget tracking |
| 🏷️ **Auto-Categorization** | AI extracts store name, items, total, and assigns spending categories |
| 🔒 **Privacy-First** | Zero cloud sync. No accounts. All data stays on your device |
| 🎨 **Premium Dark UI** | OLED-optimized Material 3 design with smooth animations |
| 📱 **Digital Receipts** | Beautiful receipt cards you can revisit anytime |
| 🔄 **OTA Updates** | Self-updating via Firebase Hosting |

## 🏗️ Architecture

```
PocketAI
├── presentation/       # Jetpack Compose UI (MVVM)
│   ├── home/           # Dashboard with spending overview
│   ├── receipts/       # Expense history with brand logos
│   ├── analytics/      # Charts and budget insights
│   ├── add/            # Manual + AI-assisted expense entry
│   ├── download/       # First-launch model download screen
│   └── settings/       # Theme, budget, profile settings
├── viewmodel/          # ViewModels with StateFlow
├── data/
│   ├── model/          # Room entities, ReceiptData
│   ├── database/       # Room database + DAOs
│   ├── repository/     # Data access layer
│   └── preferences/    # DataStore preferences
├── services/
│   ├── LlamaCppService # JNI bridge to llama.cpp
│   ├── ModelDownloadManager # On-demand model fetching
│   └── UpdateManager   # OTA update system
├── pipeline/           # Receipt processing pipeline
│   ├── ReceiptPipeline # Orchestrates scan → AI → save
│   └── ValidationStage # LLM output validation
└── cpp/                # Native C++ (llama.cpp + Vulkan GPU)
```

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Hilt DI |
| **Database** | Room (SQLite) |
| **AI Inference** | llama.cpp (C++ via JNI) |
| **Model** | Qwen3.5-VL 0.8B (Q4_K_M quantized) |
| **GPU Acceleration** | Vulkan compute shaders |
| **Scanner** | Google ML Kit Document Scanner |
| **Image Loading** | Coil |
| **Networking** | OkHttp |
| **Preferences** | Jetpack DataStore |

## 📦 Setup

### Prerequisites
- Android Studio Ladybug or later
- JDK 17
- NDK 26.1.10909125
- CMake 3.22.1

### Build

```bash
# Clone
git clone https://github.com/YourUsername/PocketAI.git
cd PocketAI

# Add your keys to local.properties
echo "LOGO_DEV_TOKEN=your_logo_dev_token" >> local.properties
echo "FIREBASE_HOSTING_URL=https://your-project.web.app" >> local.properties
echo "STORE_FILE=../keystore.jks" >> local.properties
echo "STORE_PASSWORD=your_password" >> local.properties
echo "KEY_ALIAS=your_alias" >> local.properties
echo "KEY_PASSWORD=your_password" >> local.properties

# Build
./gradlew assembleRelease
```

### First Launch
On first launch after onboarding, PocketAI downloads the AI model files (~700MB) from Firebase Hosting. This is a one-time download.

## 📱 Screenshots

*Coming soon*

## 🔐 Security

- All AI processing happens **on-device** — no data leaves your phone
- Signing credentials are stored in `local.properties` (git-ignored)
- API keys are injected via `BuildConfig` at compile time
- No analytics, tracking, or telemetry

## 📄 License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">
  <sub>Built with ❤️ and on-device AI</sub>
</div>
