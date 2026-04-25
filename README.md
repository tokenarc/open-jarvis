# OPEN JARVIS

```
██████╗ ███████╗██╗   ██╗
██╔══██╗██╔════╝██║   ██║
██║  ██║█████╗  ██║   ██║
██║  ██║██╔══╝  ╚██╗ ██╔╝
██████╔╝██║      ╚████╔╝ 
╚═════╝ ╚═╝       ╚═══╝  
```

## What Is This

Open Jarvis is an open-source Android AI agent that controls your device via voice or text commands. No root required. No subscriptions. Uses pluggable LLM providers. Learns your habits over time. Chains AI apps together for complex multi-step tasks. Lightweight enough to run on 4GB RAM devices.

[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/tokenarc/open-jarvis)](https://github.com/tokenarc/open-jarvis/stargazers)

---

## Feature Status

| Feature | Status | Milestone |
|---|---|---|
| Accessibility Service + App Launcher | ✅ Live | M1 |
| Premium VOID UI (Compose, animated) | ✅ Live | M1.5 |
| LLM Wiring (Groq, Gemini, Anthropic, OpenRouter, Custom) | ✅ Live | M2 |
| OCR + Screen Vision | ✅ Live | M2 |
| Voice Input (STT) + TTS | ✅ Live | M3 |
| Graphify Memory Graph | ✅ Live | M4 |
| Pattern Learning + Smart Suggestions | ✅ Live | M4 |
| Termux CLI Bridge | ✅ Live | M5 |
| App Intelligence + AI App Chaining | ✅ Live | M6-AI |
| Local Model (offline) | 🔨 Building | M6 |
| Skills System | 📋 Planned | M6.5 |
| MCP Server Support | 📋 Planned | M6.5 |
| Screen Watch Mode | 📋 Planned | M6.5 |
| Automation Scheduler | 📋 Planned | M6.5 |
| App Builder Mode | 📋 Planned | M7 |
| Optimization + Public Release | 📋 Planned | M8 |

---

## How It Works

### The Core Loop

```
┌─────────────┐    ┌───────────────┐    ┌─────────────┐    ┌──────────┐
│   USER    │───▶│  GRAPHIFY  │───▶│ TASK     │───▶│   LLM   │
│  INPUT   │    │  MEMORY   │    │ ROUTER   │    │  BRAIN  │
└─────────────┘    └───────────────┘    └─────────────┘    └────┬────┘
                                                      │
                                                      ▼
┌─────────────┐    ┌───────────────┐    ┌──────────┐    ┌──────────┐
│  SPEAK    │◀───│   STORE   │◀───│ VERIFY  │◀───│ ACTION  │
│  RESULT   │    │  MEMORY   │    │SCREEN  │    │  PLAN   │
└─────────────┘    └───────────────┘    └──────────┘    └──────────┘
```

1. **User Input** — Voice (push-to-talk or VAD) or text via floating overlay
2. **Graphify Memory Query** — Retrieves recent tasks, patterns, contacts, provider stats
3. **Task Router** — Analyzes command, picks best installed app automatically
4. **LLM Brain** — Generates action plan using system prompt + context
5. **Action Plan** — JSON array of atomic actions (open_app, tap, type, etc.)
6. **Accessibility Executor** — Executes actions via Android AccessibilityService
7. **Screen Verify** — Reads screen after each action to confirm success
8. **Speak + Store** — TTS reads result, logs to Graphify memory

---

### App Intelligence

Jarvis analyzes every installed app on your device:

- **Categories**: Productivity, Communication, Social, Entertainment, Browser, etc.
- **Capabilities**: Search, AI Reasoning, Messaging, File I/O, Navigation, etc.
- **Trust Scores**: Tracks success/failure per app, adjusts behavior automatically
- **AI App Detection**: Recognizes ChatGPT, Gemini, Claude, Perplexity, Copilot

When you give Jarvis a task, it picks the optimal app automatically using scoring:

```
score = (trustScore × 0.4) + (capabilityMatch × 0.4) + (recency × 0.2)
```

Can chain multiple apps together for complex tasks:

- **"Summarize this article and save to notes"**
  → Chrome opens article → OCR extracts text → Gemini summarizes → Samsung Notes saves

- **"Research iPhones and compare prices"**
  → Perplexity searches → extracts results → reads response aloud

---

### AI App Chaining

The `ai_prompt` action delegates to installed AI apps:

```json
{"action":"ai_prompt","package":"com.openai.chatgpt","prompt":"summarize this","outputKey":"summary"}
```

Jarvis opens the app, types the prompt, waits for response stability (2 identical reads), extracts result into working memory.

---

### Memory System (Graphify)

Jarvis maintains a graph database with these node types:

| Node | Stores |
|---|---|
| AppNode | Package, label, use count, last used |
| TaskNode | Command, result, provider, latency |
| ContactNode | Name, phone, contact count, method |
| PatternNode | Sequence hash, confidence, time masks |
| ProviderNode | Name, model, success rate, latency |
| EdgeEntity | Relationships between nodes |

**Pattern Learning**: After 3 identical command sequences, Jarvis learns a pattern. Shows as suggestion chips based on time of day + history.

**Smart Suggestions**: 3 chips appear above input when idle, animated in with stagger:

```
┌─────────┐ ┌─────────┐ ┌─────────┐
│call john│ │open wa  │ │search  │
└─────────┘ └─────────┘ └─────────┘
```

---

## Quick Start

### 1. Install

Download latest APK from GitHub Releases. Install → grant Accessibility Service → grant Overlay permission.

### 2. Add Your LLM

Open Settings → AI Provider → Pick: Groq (free, fast) / Gemini / OpenRouter / Custom URL → Paste API key → Done.

### 3. Give It a Command

Tap the floating Jarvis pill → type or speak your command.

Examples:
- "Open WhatsApp and text John I'm on my way"
- "Search for flights to Dubai next week"
- "Open Gemini and ask it to write me a poem"
- "Set an alarm for 7am"
- "Take a screenshot and save it to notes"

### 4. Termux CLI (optional)

```bash
cp scripts/jarvis.sh $PREFIX/bin/jarvis && chmod +x $PREFIX/bin/jarvis
jarvis "open youtube"
jarvis status
jarvis history
```

---

## Choosing Your LLM

| Use Case | Model | Provider | Notes |
|---|---|---|---|
| Daily device control | llama3-8b-8192 | Groq | Free, sub-second |
| Complex reasoning | claude-haiku-4-5-20251001 | Anthropic | Best accuracy |
| Code / App building | claude-sonnet-4-6 | Anthropic | Top quality |
| Privacy first | Phi-3 Mini Q4 | Local (M6) | Zero cloud |
| Free + capable | gemini-1.5-flash | Google | Generous free tier |
| Self hosted | any GGUF | Ollama / Custom | Full control |
| No API key | llama-3-8b:free | OpenRouter | Works immediately |
| Offline budget | Gemma-2 2B Q5 | Local (M6) | 1.8GB, fast |

---

## Supported LLM Providers

- **Groq** — Ultra-fast inference, llama/flux models
- **Google Gemini** — gemini-1.5-flash
- **Anthropic Claude** — claude-haiku, claude-sonnet, claude-opus
- **OpenAI** — gpt-4o, gpt-4o-mini
- **OpenRouter** — Aggregator, 100+ models including free tiers
- **Ollama** — Local server (localhost:11434)
- **LM Studio** — Local GGUF models
- **Custom** — Any OpenAI-compatible API

All keys stored in EncryptedSharedPreferences.

---

## App Intelligence

Jarvis indexes every app on your device:

- Learns what each app is good at (capabilities)
- Tracks which apps you use most (trust score)
- Automatically selects the best app for each task
- Chains multiple apps together for complex tasks
- Detects AI apps and knows how to prompt them

**Detected AI apps**: ChatGPT, Gemini, Claude, Perplexity, Copilot, Mistral, Character AI, Kakao i

**Trust Score Updates**:
- Success: +0.02 (caps at 1.0)
- Failure: -0.05 (floors at 0.1)

---

## Graphify Memory

Jarvis remembers:

- **Every task** you've run and its outcome
- **Which apps** you open and when
- **People** you mention and which app you contact them through
- **Patterns** in your behavior (opens WhatsApp every morning)
- **Which LLM** provider gave the best results

Memory improves suggestions. After a week of use, Jarvis starts predicting what you need before you ask.

---

## CLI Reference (Termux)

```bash
jarvis "command"         # run any command
jarvis status          # service status + current provider
jarvis history         # last 10 tasks
jarvis providers       # provider stats + success rates
jarvis memory         # current memory context summary
jarvis --help         # full usage
```

See docs/BRIDGE.md for full protocol documentation.

---

## Project Structure

```
open-jarvis/
├── app/src/main/java/com/openjarvis/
│   ├── agent/
│   │   ├── AgentCore.kt        # Main execution engine
│   │   ├── ActionPlan.kt     # Action types + parser
│   │   └── ActionExecutor.kt # Tap/type/scroll execution
│   ├── accessibility/
│   │   ├── JarvisAccessibilityService.kt
│   │   ├── ScreenReader.kt
│   │   └── ActionExecutor.kt
│   ├── bridge/
│   │   └── SocketServer.kt  # TCP socket for CLI
│   ├── graphify/
│   │   ├── GraphifyDB.kt     # Room database
│   │   ├── GraphifyRepository.kt
│   │   ├── AnalysisEngine.kt
│   │   └── nodes/           # All node types
│   ├── intelligence/
│   │   ├── AppAnalyzer.kt   # App indexing
│   │   ├── TaskRouter.kt   # App selection
│   │   ├── AIAppInteractor.kt
│   │   └── ExecutionPlan.kt
│   ├── llm/
│   │   ├── LLMProvider.kt  # Interface
│   │   ├── UniversalAdapter.kt
│   │   ├── HttpClient.kt
│   │   └── providers/       # All 8 providers
│   ├── voice/
│   │   ├── VoiceManager.kt
│   │   ├── STTEngine.kt    # Interface
│   │   ├── AndroidSTTEngine.kt
│   │   └── AndroidTTSEngine.kt
│   ├── vision/
│   │   ├── ScreenshotCapture.kt
│   │   └── VisionModule.kt  # MLKit OCR
│   └── ui/
│       ├── MainActivity.kt
│       ├── OverlayService.kt
│       ├── SettingsActivity.kt
│       ├── overlay/FloatingOverlayWidget.kt
│       ├── dashboard/DashboardScreen.kt
│       └── settings/SettingsScreen.kt
├── scripts/
│   └── jarvis.sh          # Termux CLI client
├── docs/
│   ├── BRIDGE.md
│   ├── GRAPHIFY.md
│   ├── MILESTONES.md
│   └── PROVIDERS.md
└── README.md
```

---

## Architecture

```
┌───────────────────────────────────────────────────────────────��─��
│                        UI LAYER                            │
│  FloatingOverlayWidget │ Dashboard │ Settings │ OverlayService │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AGENT CORE                                │
│           executeTask() → Task Router → LLM Brain              │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   GRAPHIFY  │    │  APP      │    │   LLM      │
│   MEMORY   │    │ INTELLIGENCE│    │ PROVIDERS  │
│            │    │           │    │           │
│ - TaskNode │    │ - AppAnalyzer│  │ - Groq    │
│ - AppNode │    │ - TaskRouter│   │ - Gemini  │
│ - Contact │    │ - AIApp   │    │ - Claude  │
│ - Pattern│    │   Interact │  │ - OpenAI  │
│ - Edge   │    │ - TrustSc │    │ - Ollama  │
└──────────────┘    └──────────────┘    └──────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   ROOM     │    │ ACCESSIBILI│    │   HTTP    │
│  DATABASE  │    │ TY SVC   │    │  CLIENT   │
└──────────────┘    └──────────────┘    └──────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   ACCESSIBILITY SERVICE                         │
│    Tap │ Type │ Scroll │ Screenshot │ Voice Pipeline (STT + TTS)    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Milestones

| Milestone | Goal | Status | Description |
|---|---|---|---|
| M1 | Accessibility Service + App Launcher | ✅ Complete | Foundation, basic overlay |
| M1.5 | Premium VOID UI | ✅ Complete | Compose animations, theme |
| M2 | Screenshot + OCR + LLM | ✅ Complete | Vision + 8 providers |
| M3 | Voice Input (STT + TTS | ✅ Complete | Push-to-talk, VAD |
| M4 | Graphify Memory | ✅ Complete | Pattern learning |
| M5 | Termux CLI Bridge | ✅ Complete | TCP socket + jarvis.sh |
| M6-AI | App Intelligence | ✅ Complete | Auto app selection |
| M6 | Local Model Support | 🔨 Building | llama.cpp bridge |
| M6.5 | Skills System | 📋 Planned | Skill files |
| M6.5 | MCP Server | 📋 Planned | Model Context Protocol |
| M7 | App Builder Mode | 📋 Planned | No-code agent creation |
| M8 | Optimization + Release | 📋 Planned | Public launch |

---

## Contributing

Contributions welcome! Please read [docs/PROVIDERS.md](docs/PROVIDERS.md) for adding new LLM providers. See [docs/BRIDGE.md](docs/BRIDGE.md) for CLI documentation.

Skills system coming in M6.5 — contribute skill files.

**To add a new LLM provider:**
1. Implement `LLMProvider` interface in `llm/`
2. Add to `UniversalAdapter.AVAILABLE_PROVIDERS`
3. Add model defaults in `UniversalAdapter.getDefaultModel()`

---

## Performance

| Mode | RAM Usage | Response Time |
|---|---|---|
| Cloud LLM (idle) | ~60MB | — |
| Cloud LLM (active) | ~180MB | 1-3 seconds |
| Voice active | ~320MB | 2-4 seconds |
| Local model loaded | ~2.1GB | 5-10 seconds |

Tested on: Android 8.0+, 4GB RAM minimum recommended

---

## Built By

tokenarc — built entirely on Android using Termux.

GitHub: https://github.com/tokenarc/open-jarvis

---

## License

MIT