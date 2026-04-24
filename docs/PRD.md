# Product Requirements Document - Open Jarvis

## 1. Product Name

Open Jarvis

## 2. Vision

An open-source, lightweight Android AI agent that lets any user control their Android device using voice or text commands. No root. No subscriptions. Pluggable with any LLM API (Groq, Gemini, OpenRouter, Ollama, LM Studio, Anthropic, OpenAI, or any OpenAI-compatible endpoint). Powered by Graphify for graph-based persistent memory.

## 3. Target Device

- Android 8.0+ (minSDK 26)
- 4GB RAM minimum
- Works on budget phones
- APK distributed via GitHub Releases

## 4. Core Capabilities

1. **Voice command input** (Whisper tiny, offline)
2. **Text command input** (floating overlay UI)
3. **Open any app** by name or intent
4. **Tap, swipe, scroll, type** on any screen
5. **Read screen content** via OCR (MLKit)
6. **Send to LLM** for decision making
7. **Graphify memory** — learns patterns, remembers tasks, contacts, apps
8. **Companion CLI bridge** via Unix socket (Termux compatible)
9. **Local LLM support** (llama.cpp, Phi-3 mini)
10. **Universal LLM adapter** — one interface, all providers

## 5. App Flow

```
USER INPUT (voice or text)
    │
    ▼
GRAPHIFY MEMORY QUERY
→ retrieve relevant past tasks
→ retrieve known app nodes
→ retrieve contact nodes
→ retrieve pattern edges
    │
    ▼
LLM BRAIN (selected provider)
→ receives: user command + memory context + current screen OCR
→ outputs: JSON action plan
    │
    ▼
ACTION EXECUTOR (AccessibilityService)
→ step 1: execute action
→ screenshot → OCR → verify expected state
→ if mismatch: retry or ask LLM for correction
→ step 2: next action
→ ... repeat until task done
    │
    ▼
RESULT
→ speak result via TTS
→ store task + outcome into Graphify
→ update pattern edges
```

## 6. JSON Action Format

```json
[
  {"action": "open_app", "package": "com.whatsapp", "label": "WhatsApp"},
  {"action": "wait_for", "text": "Chats", "timeout_ms": 3000},
  {"action": "tap", "text": "Search"},
  {"action": "type", "value": "John"},
  {"action": "tap", "text": "John Doe"},
  {"action": "tap", "hint": "message"},
  {"action": "type", "value": "Hello"},
  {"action": "tap", "text": "Send"}
]
```

## 7. Supported Actions

- `open_app` — by package or label
- `tap` — by text, content-desc, or coordinates
- `long_press`
- `type` — into focused field
- `swipe` — direction + distance
- `scroll` — up/down/left/right
- `press_back`
- `press_home`
- `press_recents`
- `wait_for` — text to appear, with timeout
- `screenshot` — capture + return OCR text
- `read_screen` — return full UI tree as text

## 8. LLM Provider System

All providers implement one interface. User enters Base URL + API Key in settings. Works with:

- Groq
- Google Gemini
- OpenRouter
- Anthropic Claude
- OpenAI
- Ollama (local)
- LM Studio (local)
- Any OpenAI-compatible API (custom URL)
- llama.cpp server mode

## 9. Graphify Memory

Graph stored in SQLite via Room DB. No server.

**Node types:**
- AppNode
- TaskNode
- ContactNode
- PatternNode
- ProviderNode

**Edge types:**
- UsedBy
- OpenedAfter
- ResultedIn
- MentionedIn
- LearnedFrom

## 10. Performance Targets

- Cold start: under 1.5 seconds
- Task initiation voice→action: under 3 seconds (cloud LLM)
- Idle RAM: under 60MB
- APK size: under 25MB base (models downloaded separately)
- minSDK: 26 (Android 8.0)

## 11. Repo Structure

```
open-jarvis/
├─�� app/
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/openjarvis/
│           │   ├── MainActivity.kt
│           │   ├── accessibility/
│           │   │   ├── JarvisAccessibilityService.kt
│           │   │   ├── ActionExecutor.kt
│           │   │   └── ScreenReader.kt
│           │   ├── agent/
│           │   │   ├── AgentCore.kt
│           │   │   └── ActionPlan.kt
│           │   ├── llm/
│           │   │   ├── LLMProvider.kt
│           │   │   ├── UniversalAdapter.kt
│           │   │   └── providers/
│           │   │       ├── GroqProvider.kt
│           │   │       ├── GeminiProvider.kt
│           │   │       └── CustomProvider.kt
│           │   ├── graphify/
│           │   │   ├── GraphifyDB.kt
│           │   │   ├── nodes/
│           │   │   │   ├── AppNode.kt
│           │   │   │   └── TaskNode.kt
│           │   │   └── GraphifyRepository.kt
│           │   └── ui/
│           │       ├── OverlayService.kt
│           │       └── SettingsActivity.kt
│           └── res/
│               ├── layout/
│               ├── xml/
│               │   └── accessibility_service_config.xml
│               └── values/
├── docs/
│   ├── PRD.md
│   ├── APP_FLOW.md
│   ├── PROVIDERS.md
│   ├── GRAPHIFY.md
│   └── MILESTONES.md
├── .github/
│   └── ISSUE_TEMPLATE/
│       └── bug_report.md
├── build.gradle (project level)
├── app/build.gradle
├── settings.gradle
├── .gitignore
└── README.md
```