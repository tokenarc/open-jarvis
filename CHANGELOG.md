# Open Jarvis — Changelog

## v1.0.0-beta (Current)

### M1 — Foundation ✅
- Accessibility Service with full device control
- App launching by name
- Graphify memory system initialized

### M1.5 — VOID UI ✅
- Premium dark interface (Space Grotesk + JetBrains Mono)
- Animated floating overlay widget
- Dashboard, Settings screens
- Spring physics animations throughout

### M2 — AI Brain ✅
- Universal LLM adapter (8+ providers)
- Groq, Gemini, Anthropic, OpenAI, OpenRouter, Ollama, Custom URL
- Screen OCR via ML Kit
- Full action executor (tap, type, swipe, scroll)

### M3 — Voice ✅
- Push-to-talk + VAD auto-stop modes
- Android SpeechRecognizer STT
- Android TTS with personality settings
- Voice model download system

### M4 — Memory ✅
- Graphify graph memory (App, Task, Contact, Pattern nodes)
- Pattern learning from repeated tasks
- Time-of-day correlation
- Smart suggestions in overlay

### M5 — CLI Bridge ✅
- Unix socket server inside APK
- jarvis.sh companion script for Termux
- Commands: status, history, providers, memory
- ADB shell support

### M6 — Local Model ✅
- llama.cpp JNI bridge
- Three model tiers: Minimum / Balanced / Power
- Resumable download with progress
- 5-minute RAM unload timer
- RAM safety guard before loading

### M6.5 — Extension Layer ✅
- Skills system with 7 built-in skills
- MCP server support (Home Assistant, Notion, GitHub)
- Screen Watch Mode with built-in ad-skip rules
- Automation Scheduler via WorkManager

### M6.8 — Autonomous Agent ✅
- Multi-phase prompt engine
- Self-healing execution loop
- Conversation context (pronoun resolution)
- Notification Intelligence (read + reply)
- Tutorial Mode (guided + observe)
- Risky action confirmation
- Clipboard intelligence
- 5-screen onboarding flow
- Prompt library with examples

### App Intelligence ✅
- Full device app analysis and indexing
- Trust score system (per-app reliability)
- AI app chaining (ChatGPT, Gemini, Claude, Perplexity)
- Task working memory across multi-app flows
- Auto-detection of AI apps and their prompt interfaces

### M7 — Security + App Builder ✅
- Full security audit and hardening
- Prompt injection guard
- Provider fallback chain
- Race condition prevention (mutex)
- Smart error messages with recovery suggestions
- Adaptive timing (learns your device speed)
- App Builder Mode (generate Android apps from prompt)
- Built app registry and management

### M8 — Release ✅
- R8 full mode optimization
- Sub-1.5s cold start
- Battery-aware Watch Mode
- Memory pressure handling
- Connection pooling
- Database indexes

---

## v0.9.0 (Alpha)

### First Public Alpha
- Basic accessibility service
- App launcher
- Floating overlay
- Groq integration only

---

## Security History

| Version | Security Fixes |
|---------|---------------|
| 1.0.0-beta | PromptSanitizer, SocketAuth, NotificationPrivacy |
| 0.9.0 | Initial release - no security features |

---

## Upgrade Guide

### From v0.9.0

1. Clear app data (Settings → Apps → Open Jarvis → Clear Data)
2. Uninstall old APK
3. Install v1.0.0-beta
4. Reconfigure API key
5. Grant permissions again

---

## Deprecations

| Feature | Deprecated | Replacement |
|--------|------------|-------------|
| HTTP provider | 0.9.0 | Use Groq instead |
| CLI (ADB) | 0.9.0 | Use socket server |

---

## Known Issues

- Local model requires NDK for llama.cpp
- Tutorial Mode may miss some custom UI elements
- Screen Watch may use extra battery on 4GB devices

---

## Download

Get latest APK from GitHub Releases: https://github.com/tokenarc/open-jarvis/releases