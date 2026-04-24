# Open Jarvis

```
 ██████╗ ███████╗██╗   ██╗
 ██╔══██╗██╔════╝██║   ██║
 ██║  ██║█████╗  ██║   ██║
 ██║  ██║██╔══╝  ╚██╗ ██╔╝
 ██████╔╝██║      ╚████╔╝ 
 ╚═════╝ ╚═╝       ╚═══╝  
```

An open-source, lightweight Android AI agent. Control your device with voice or text. No root. No subscriptions.

[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/tokenarc/open-jarvis)](https://github.com/tokenarc/open-jarvis/stargazers)

Open Jarvis is an open-source Android AI agent that lets any user control their Android device using voice or text commands. No root required. Works with any LLM API (Groq, Gemini, OpenRouter, Ollama, Anthropic, OpenAI, or any OpenAI-compatible endpoint).

## Features

- [x] M1: Accessibility Service + App Launching
- [ ] M2: Screenshot + OCR + LLM Integration
- [ ] M3: Full Action Executor (tap/type/scroll)
- [ ] M4: Graphify Memory System
- [ ] M5: Voice Input (Whisper STT + TTS)
- [ ] M6: Termux CLI Bridge
- [ ] M7: Local Model Support (llama.cpp)
- [ ] M8: Optimization + Public Release

## Quick Start

1. Download the latest APK from [Releases](https://github.com/tokenarc/open-jarvis/releases)
2. Install the APK on your Android device (Android 8.0+)
3. Grant Accessibility permission when prompted
4. Grant Overlay permission when prompted
5. Tap the floating "J" button to enter commands

### Example Commands

- "open WhatsApp" - Opens WhatsApp
- "open Chrome" - Opens Google Chrome
- "back" - Press back button
- "home" - Go to home screen

## LLM Provider Setup

Open Jarvis supports multiple LLM providers. To configure:

1. Open Settings from the main screen
2. Select your provider (Groq, Gemini, OpenRouter, Anthropic, OpenAI, Ollama, or Custom)
3. Enter your API key (stored securely)
4. Optionally specify a custom Base URL for self-hosted models

### Supported Providers

| Provider | Model Example | Notes |
|----------|--------------|-------|
| Groq | llama-3.1-70b-versatile | Fast inference |
| Gemini | gemini-1.5-flash | Google's model |
| OpenRouter | anthropic/claude-3.5-sonnet | Aggregator |
| Anthropic | claude-3-5-sonnet-20241022 | Claude API |
| OpenAI | gpt-4o | OpenAI API |
| Ollama | llama3.1 | Local (localhost:11434) |
| Custom | Any | Self-hosted APIs |

## Graphify Memory

Open Jarvis includes **Graphify** - a graph-based memory system that learns from your usage patterns:

- **App Nodes**: Tracks which apps you open and how often
- **Task Nodes**: Stores completed commands and results
- **Pattern Edges**: Learns patterns like "You usually open X after Y"

The memory system helps the LLM make smarter decisions by providing context about your typical app usage patterns.

## Milestone Roadmap

| Milestone | Goal | Status |
|-----------|------|--------|
| M1 | Accessibility Service + App Launcher | Complete |
| M2 | Screenshot + OCR + LLM Wired | In Progress |
| M3 | Full Action Executor | Pending |
| M4 | Graphify Memory Integration | Pending |
| M5 | Voice Input (STT + TTS) | Pending |
| M6 | Termux CLI Bridge | Pending |
| M7 | Local Model Support | Pending |
| M8 | Optimization + Public Release | Pending |

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Built with love on Android/Termux by [TokenArc](https://tokenarc.ai)