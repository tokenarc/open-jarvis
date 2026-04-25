# Skills — Open Jarvis

## Overview

Skills are JSON-defined task templates that Jarvis uses to understand common user intents and execute them reliably. Each skill defines triggers, variables, and action sequences.

---

## Skill Schema

```json
{
  "id": "unique-skill-id",
  "name": "Human readable name",
  "version": "1.0",
  "author": "tokenarc",
  "triggerPhrases": ["phrase 1", "phrase 2"],
  "variables": [
    {"key": "variableName", "description": "What this is", "extractFrom": "trigger"}
  ],
  "llmHint": "Hint for LLM when choosing this skill",
  "actionTemplate": [
    {"action": "actionType", "param": "value"}
  ],
  "successVerification": "Text that should appear after success",
  "tags": ["tag1", "tag2"],
  "usageCount": 0,
  "successRate": 1.0
}
```

---

## Fields

| Field | Required | Description |
|-------|----------|------------|
| id | Yes | Unique identifier for skill |
| name | Yes | Display name |
| version | Yes | Schema version (1.0) |
| author | No | Who created the skill |
| triggerPhrases | Yes | Patterns that activate this skill |
| variables | No | Input variables to extract |
| llmHint | No | Hint for AI decision making |
| actionTemplate | Yes | Array of actions to execute |
| successVerification | No | Text that confirms success |
| tags | No | Categorization tags |

---

## Trigger Phrases

Use `*` as wildcard for any text:

```
"text *"          → matches "text John"  
"sms to *"        → matches "sms to John"
"open youtube"   → exact match
```

Variables are extracted from trigger phrases using `{key}` syntax in actionTemplate.

---

## Actions

| Action | Parameters | Description |
|--------|------------|-------------|
| open_app | package, label | Launch an app |
| tap | text, textContains | Tap element by text |
| tap_coords | x, y | Tap by coordinates |
| long_press | text | Long press element |
| type | value | Type text into focused field |
| clear_type | value | Clear then type |
| swipe | direction, distance | Swipe gesture |
| scroll | direction | Scroll down/up |
| press_back | - | Press back button |
| press_home | - | Press home button |
| wait_for | text, timeout_ms | Wait for text to appear |

---

## Example Skills

### Send SMS

```json
{
  "id": "send-sms",
  "name": "Send SMS",
  "triggerPhrases": ["text *", "sms *", "send sms to *"],
  "variables": [
    {"key": "contact", "extractFrom": "trigger"},
    {"key": "message", "extractFrom": "trigger"}
  ],
  "actionTemplate": [
    {"action": "open_app", "package": "com.google.android.apps.messaging"},
    {"action": "tap", "text": "Start chat"},
    {"action": "type", "value": "{contact}"},
    {"action": "tap_first_result"},
    {"action": "type", "value": "{message}"},
    {"action": "tap", "text": "Send"}
  ]
}
```

### Call Contact

```json
{
  "id": "call-contact",
  "name": "Call Contact",
  "triggerPhrases": ["call *", "phone *", "dial *"],
  "actionTemplate": [
    {"action": "open_app", "package": "com.google.android.apps.dialer"},
    {"action": "type", "value": "{contact}"},
    {"action": "tap", "text": "Call"}
  ]
}
```

---

## Adding Custom Skills

1. Create JSON file in `app/src/main/assets/skills/`
2. Follow schema above
3. Test on device
4. Submit PR to skills/ folder

---

## Built-in Skills

| Skill | Description |
|-------|-------------|
| send-sms | Send SMS message |
| call-contact | Make phone call |
| take-screenshot | Capture screen |
| set-alarm | Create alarm |
| google-something | Search Google |
| open-youtube-search | YouTube search |
| send-whatsapp-message | WhatsApp message |

---

## Skill Matching

When user input is received:
1. Match against trigger phrases
2. Extract variables
3. Use llmHint to guide LLM
4. Execute actionTemplate
5. Verify with successVerification

If no skill matches, fall back to pure LLM reasoning.