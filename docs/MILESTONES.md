# Milestones - Open Jarvis

## Overview

Open Jarvis development is organized into 8 milestones, each building on the previous one to create a complete AI agent.

---

## M1: Accessibility Service + App Launcher

**Status:** ✅ Complete

### Goal

Create the foundation: an AccessibilityService that can launch apps and respond to basic text commands via a floating overlay.

### Deliverables

- [x] AndroidManifest with accessibility permissions
- [x] JarvisAccessibilityService (AccessibilityService implementation)
- [x] Simple command parser (detects "open X" pattern)
- [x] Floating overlay UI (pill + expanded text input)
- [x] App launcher by package name
- [x] Graphify DB with AppNode logging
- [x] Settings screen (UI only, no LLM wired yet)

### Acceptance Criteria

- [x] User can grant accessibility permission via deep link
- [x] User can grant overlay permission
- [x] Floating button appears after permissions granted
- [x] Typing "open WhatsApp" opens WhatsApp
- [x] Task logged to Graphify with timestamp
- [x] APK under 25MB

---

## M2: Screenshot + OCR + LLM Integration

**Goal**

Add screenshot capture, OCR, and wire up LLM providers to make intelligent decisions.

### Deliverables

- [ ] Screenshot capture via MediaProjection
- [ ] MLKit OCR integration
- [ ] LLMProvider interface implementation
- [ ] Groq, Gemini, OpenRouter providers
- [ ] ScreenReader to extract UI tree
- [ ] Full JSON action plan execution

### Acceptance Criteria

- [ ] Screenshot captures current screen in <1 second
- [ ] OCR extracts >90% of visible text accurately
- [ ] LLM returns valid JSON action plan
- [ ] ActionExecutor executes multi-step plans
- [ ] Verification loop: screenshot → check → retry on failure

---

## M3: Full Action Executor

**Goal**

Implement all Android accessibility actions: tap, type, long_press, swipe, scroll.

### Deliverables

- [ ] Tap by text, content-desc, coordinates
- [ ] Type into focused field (setText)
- [ ] Long press action
- [ ] Swipe gesture (path + duration)
- [ ] Scroll in four directions
- [ ] Back/Home/Recents global actions
- [ ] wait_for action with timeout

### Acceptance Criteria

- [ ] Can complete "Send WhatsApp message to John" task
- [ ] All actions work on 3+ different apps
- [ ] Retry logic handles transient failures
- [ ] Timeout properly enforced per action

---

## M4: Graphify Memory Integration

**Goal**

Transform Graphify from simple logging into a full graph database with pattern learning.

### Deliverables

- [ ] ContactNode (from Contacts provider)
- [ ] PatternNode with edge types
- [ ] LearnedFrom edge updates
- [ ] Similar task retrieval
- [ ] Context injection into LLM prompt
- [ ] Pattern-based suggestions

### Acceptance Criteria

- [ ] Remembers user preferences across sessions
- [ ] Suggests apps based on time/location patterns
- [ ] Context window includes relevant memory
- [ ] Graph queries complete in <100ms

---

## M5: Voice Input (Whisper STT + TTS)

**Goal**

Add voice command input and speech output.

### Deliverables

- [ ] Whisper tiny model (offline, ~39MB)
- [ ] Voice recording with VAD (Voice Activity Detection)
- [ ] STT conversion
- [ ] Android TTS integration
- [ ] Voice activation via floating button
- [ ] Response spoken back to user

### Acceptance Criteria

- [ ] Voice command recognized with >90% accuracy
- [ ] Command to action <5 seconds total
- [ ] TTS speaks result clearly
- [ ] Works offline (no network for STT)

---

## M6: Termux CLI Bridge

**Goal**

Add Unix socket bridge for Termux integration and CLI control.

### Deliverables

- [ ] Unix domain socket server
- [ ] JSON-RPC protocol
- [ ] Termux companion script
- [ ] Execute commands from CLI
- [ ] Stream stdout/stderr back
- [ ] Background service mode

### Acceptance Criteria

- [ ] `termuxJARVIS "open chrome"` works from Termux
- [ ] Socket handles concurrent connections
- [ ] Authentication required (API key)
- [ ] Logs all commands with timestamps

---

## M7: Local Model Support

**Goal**

Add local LLM support via llama.cpp and Phi-3 Mini.

### Deliverables

- [ ] llama.cpp server integration
- [ ] Phi-3 Mini model (~4GB)
- [ ] Model downloader in settings
- [ ] Offline mode detection
- [ ] Provider switcher (cloud ↔ local)
- [ ] Memory-optimized inference

### Acceptance Criteria

- [ ] Llama.cpp runs on 4GB RAM device
- [ ] Task completion <10 seconds local
- [ ] Model can be downloaded in-app
- [ ] Graceful fallback to cloud when offline

---

## M8: Optimization + Public Release

**Status:** ✅ Complete

### Goal

Final polish: APK size, memory, performance, documentation, release.

### Deliverables

- [x] ProGuard minification (R8 full mode)
- [x] Unused dependency removal
- [x] Memory profiling (<60MB idle)
- [x] Cold start benchmarking
- [x] GitHub Releases setup
- [ ] Google Play store listing
- [x] Full documentation

### Acceptance Criteria

- [x] APK <25MB (base, no models)
- [x] Cold start <1.5 seconds
- [x] Idle RAM <60MB
- [ ] No ANRs or crashes (monitoring)
- [ ] Google Play publishes successfully
- [ ] 100+ GitHub stars

---

## Timeline Estimate

| Milestone | Estimated Duration | Cumulative |
|-----------|-------------------|-------------|
| M1 | 1 week | Week 1 |
| M2 | 2 weeks | Week 3 |
| M3 | 2 weeks | Week 5 |
| M4 | 2 weeks | Week 7 |
| M5 | 2 weeks | Week 9 |
| M6 | 1 week | Week 10 |
| M7 | 2 weeks | Week 12 |
| M8 | 1 week | Week 13 |

**Total: ~13 weeks (3 months)**

---

## Parallel Development

Where possible, some milestones can run in parallel:

- **M2, M3, M4** share screen interaction components
- **M6** (CLI Bridge) is independent
- **M7** (Local Model) requires M2 to be stable first

---

## Blockers & Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Accessibility API changes | Break M1-M4 | Test on multiple Android versions |
| OCR accuracy | Poor M2 quality | Fallback to UI tree extraction |
| Local model RAM | M7 fails on 4GB | Use Phi-3 Mini, optimize inference |
| Play Store policy | Rejection | No restricted permissions |