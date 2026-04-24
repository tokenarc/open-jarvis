# App Flow - Open Jarvis

## Overview

Open Jarvis follows a five-stage flow from user input to task completion, with feedback loops for verification and error recovery.

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER INPUT                                  │
│                  (voice or text command)                            │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     GRAPHIFY MEMORY QUERY                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │
│  │ App Nodes   │  │ Task Nodes  │  │Pattern Edges│               │
│  │ (recent,   │──│  (similar  │  │  (learned  │               │
│  │  frequent) │  │   tasks)   │  │  patterns) │               │
│  └─────────────┘  └─────────────┘  └─────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        LLM BRAIN                                   │
│  Input: {command + memory_context + screen_OCR}                    │
│  System: "You are an Android automation agent..."                │
│  Output: {JSON action plan}                                        │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   ACTION EXECUTOR (loop)                           │
│                                                                     │
│  for each action in plan:                                            │
│    ┌──────────────────────┐                                       │
│    │ Execute Action         │                                       │
│    │ (tap/type/scroll/etc)  │                                       │
│    └──────────────────────┘                                       │
│             │                                                       │
│             ▼                                                       │
│    ┌───���──────────────────┐                                       │
│    │ verify_result()      │                                       │
│    │ (screenshot + OCR)   │                                       │
│    └──────────────────────┘                                       │
│             │                                                       │
│     ┌───────┴───────┐                                               │
│     │              │                                               │
│    YES            NO                                                │
│     │              │                                                │
│     ▼              ▼                                               │
│  continue    ┌──────────────────────┐                              │
│              │ Retry once           │                              │
│              │ then: ask LLM       │                              │
│              │ for correction     │                              │
│              └──────────────────────┘                              │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         RESULT                                      │
│  • Speak result via TTS                                             │
│  • Store task + outcome in Graphify                                │
│  • Update pattern edges                                           │
└─────────────────────────────────────────────────────────────────────┘
```

## Stage Details

### Stage 1: User Input

**Input Methods:**
- Voice: Press floating button → voice recording → Whisper STT → text
- Text: Tap floating button → expand overlay → type command → send

**Example Commands:**
- "Open WhatsApp"
- "Send message to John: Hello"
- "Search for cat videos on YouTube"
- "Turn on the flashlight"

### Stage 2: Graphify Memory Query

Before sending to LLM, query Graphify for:
- Recent apps used
- Most frequent apps
- Similar past tasks
- Learned patterns

**Context Building:**
```
Memory Context:
- Recent apps: [WhatsApp, Chrome, Camera]
- Frequent: WhatsApp (15x), Chrome (12x)
- Last task: "Open WhatsApp" → Success
- Pattern: Often opens WhatsApp after Camera
```

### Stage 3: LLM Brain

**System Prompt:**
```
You are an Android automation agent. Given a user command and context about the user's 
recent app usage and screen content, respond with a JSON action plan to complete the task.

Available actions: open_app, tap, type, swipe, scroll, press_back, press_home, 
                 press_recents, wait_for, screenshot

Respond ONLY with JSON array. No explanations.
```

**User Message Format:**
```
Command: {user_input}
Screen: {OCR text from current screen}
Memory: {context from Graphify}
```

**Output:** JSON array of actions.

### Stage 4: Action Executor

Execute each action sequentially:
1. Execute current action
2. Capture screenshot
3. Run OCR on screenshot
4. Verify expected text is present
5. If verification fails: retry once
6. If retry fails: ask LLM for correction
7. Continue to next action

### Stage 5: Result

- Text-to-speech result to user
- Store in Graphify:
  - If success: log task to TaskNode + update AppNode useCount
  - If failure: log task with error
- Update pattern edges for learning

## Error Handling

| Error Type | Handling |
|------------|----------|
| App not found | Try fuzzy match → if fails, ask user |
| Action fails | Retry once → then ask LLM for alternative |
| Timeout | Wait for action times out → ask LLM to retry |
| Permission denied | Prompt user to grant permission |
| LLM error | Use fallback provider if configured |

## State Machine

```
[Idle] ──command──► [Parsing]
                         │
                         ▼
                   [QueryMemory]
                         │
                         ▼
                     [LLMCall]
                         │
                         ▼
                   [Executing]
                         │
               ┌─────────┴─────────┐
               │                   │
          [Success]           [Retry]
               │                   │
               ▼                   ▼
            [Done]           [RetryAction]
                                   │
                              ┌─────┴─────┐
                              │           │
                           [OK]      [Fail]
                              │           │
                              ▼           ▼
                           [Done]    [Error]
```

## Voice Flow Extension

For voice commands, add TTS at start and end:

```
[User Voice] ──Whisper──► [Text]
                            │
                            ▼ (continue normal flow)
                       [LLMBrain]
                            │
                            ▼
                       [Execute]
                            │
                            ▼
                       [TTS Result]
```