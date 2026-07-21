# Googol

Googol is a native Android application built entirely in Jetpack Compose that delivers a high-fidelity, interactive simulation of the Google Search and Google Workspace ecosystem.

It reimagines the familiar desktop and web experience within a mobile-first, fluid, and beautifully designed Material 3 environment, showcasing modern Android development practices, custom layout designs, and responsive interactive elements.

---

## 🎨 Visual Identity & Design Principles

Googol has been designed with strict adherence to professional Android styling guidelines and visual integrity:
- **Material Design 3 (M3) Alignment**: Leverages custom themes, fluid dynamic elements, consistent spacing, and generous padding.
- **Micro-Interactions**: All buttons, cards, and inputs feature custom-tailored feedback, smooth animations, and interactive Material Ripples.
- **No Design Slop**: Built using bespoke geometries, clean typographic scales, and carefully tuned color palettes to ensure an authentic premium feeling.

---

## 🚀 Key Features

### 1. Googol Search Engine Hub
* **Signature Logo Animation**: Beautifully rendered custom letters spelling out the classic logo.
* **Interactive Search**: An elegant search bar with suggestions, filterable tab buttons (All, Images, News, Shopping), and full navigation routing.
* **Voice Search Simulator**: An immersive voice input screen backed by a responsive custom frequency-wave `VoiceVisualizer`.
* **Discover Feed**: A stylized information hub containing content-filled cards and horizontal categories to explore.

### 2. Google Workspace Simulators
An integrated launcher (`WorkspaceScreen`) hosting high-fidelity, interactive representations of key G-Suite productivity apps:
* **📧 Gmail**: Send and read modeled emails with custom category filter bars and full-screen detail views.
* **📁 Drive**: Navigate files and folder systems with tabbed organization and real-time creation simulators.
* **📝 Docs / 📊 Sheets**: Interactive word-processing and spreadsheet grid structures with simulated cells, search filters, and content-creation modals.
* **📅 Calendar**: Custom schedule view displaying simulated daily, weekly, or monthly appointments.
* **📹 Meet**: High-fidelity meeting simulation featuring custom active call controls and animated meeting wave patterns (`MeetingWaves`).
* **🗺️ Maps**: High-fidelity map interface with address searches, directional mock overlays, and search categories.

### 3. Developer & Diagnostics Suite
* **Diagnostic Console**: Real-time state tracing, activity logging, and health checking.
* **Code Viewer**: A dedicated tab containing live previews of critical application components and configurations.
* **Settings & Customization**: Configure layout preferences, toggle dark/light theme properties, and access developer metrics.

---

## 📁 Project Directory Layout

The codebase is organized into a clean and structured modular Android hierarchy:

```
Googol/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   └── MainActivity.kt       # Unified entry point, ViewModels, and UI components
│   │   │   └── res/                      # Layout resources, drawables, and strings
│   │   └── build.gradle.kts              # App-level dependencies and build configs
├── gradle/                               # Gradle Wrapper files & configs
├── build.gradle.kts                      # Root Gradle build script
├── settings.gradle.kts                   # Project settings and module naming
└── README.md                             # Project documentation
```

---

## 🛠️ Getting Started & Compiling

To build the application locally:

### Compile the APK
Using Gradle, run the following task to compile the debug build variant:
```bash
gradle :app:assembleDebug
```

The compiled Android application package (`.apk`) can then be found inside:
`app/build/outputs/apk/debug/`

---

## 🔒 Security & API Integrations
Googol manages API secrets and configurations securely via runtime properties and `BuildConfig`. Hardcoded credentials are strictly avoided. Ensure appropriate keys are set in the `.env` context or the platform's secrets console when running connected workflows.
