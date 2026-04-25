# App Builder — Open Jarvis

## Overview

App Builder Mode lets users generate simple Android apps from natural language descriptions. Say "build me a [type] app" and Jarvis creates a complete project.

---

## How It Works

1. User describes desired app
2. Jarvis analyzes requirements
3. Generates Android project structure
4. Creates all necessary files
5. Provides build instructions

---

## Requirements

- Termux installed with Gradle
- ~2GB free storage
- Android SDK (via Termux)

---

## Usage

### From Overlay

```
User: "build me a calculator app"
Jarvis: "What features should the calculator have?"
User: "Basic math, plus minus multiply divide"
Jarvis: [Generates project]
```

### From Termux CLI

```bash
jarvis build "flashlight app with sos pattern"
```

---

## Generated Project Structure

```
MyApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/myapp/
│   │   │   ├── MainActivity.kt
│   │   │   └── ...
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── values/
│   │   │   └── drawable/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

---

## Supported App Types

| Type | Features |
|------|--------|
| Calculator | Buttons, operations, display |
| Flashlight | Camera torch, SOS pattern |
| Notes | CRUD notes, local storage |
| Timer | Countdown, alarm |
| Converter | Unit conversions |
| Quiz | Questions, scoring |

---

## Build APK

After generation:

```bash
cd ~/jarvis-builds/myapp
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

---

## Customization

Generated apps include:
- Dark theme (VOID-inspired)
- Material 3 components
- SharedPreferences for settings

Users can edit the generated code to add features.

---

## Limitations

- No backend/server features
- Simple UI only (no complex animations)
- Local storage only (no database)
- Maximum ~10 screens

---

## Tips for Better Results

- Be specific about features
- Mention color preferences
- Specify target Android version
- Describe expected user flow

---

## Examples

### Calculator App
```
"build me a calculator with basic math operations, dark theme"
```

### Flashlight App
```
"create a flashlight app with sos blink pattern, bright white light"
```

### Notes App
```
"make a simple notes app that saves locally"
```