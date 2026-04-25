# Security — Open Jarvis

## Security Model

Open Jarvis has a defense-in-depth security architecture focusing on user privacy and device safety.

---

## Core Principles

1. **Data stays on device** — No cloud sync, all local
2. **Least privilege** — Only needed permissions
3. **Defense in depth** — Multiple security layers
4. **Transparent** — User knows what's happening

---

## Permissions

### Required Permissions

| Permission | Purpose | Risk |
|------------|---------|------|
| Accessibility Service | Control UI | High |
| Overlay | Floating widget | Medium |
| Microphone | Voice input | High |
| Storage | Local model | Medium |
| Network | LLM APIs | Medium |

### Not Required

- Camera (uses screen capture only)
- Contacts (optional, for calls only)
- Location (never used)

---

## Privacy Protections

### Local-First

- Graphify database is local SQLite
- API keys in EncryptedSharedPreferences
- No analytics or tracking
- No cloud backup

### Notification Privacy

Banking apps blocked from notification reading:

```
com.google.android.apps.authenticator2
com Chase
com.bankofamerica
com.wellsfargo
com.citibank
com.paypal
```

 Jarvis won't read or reply to these notifications.

---

## Security Features

### Prompt Injection Guard

```kotlin
val injectionPatterns = listOf(
    "ignore instructions",
    "jailbreak",
    "DAN mode",
    "new instructions"
)
```

Detects and blocks prompt injection attempts.

### Provider Fallback Chain

If one LLM fails:
1. Try primary provider
2. Wait 1 second
3. Try fallback provider
4. Show error with recovery suggestion

### Socket Authentication

Only allowed UIDs can connect:

```kotlin
val allowedUids = setOf(
    context.applicationInfo.uid,
    2000,  // shell
    "com.termux".hashCode()
)
```

---

## Data Handling

### What's Stored

- Task history (commands, results)
- App usage patterns
- User preferences
- Graphify memory nodes

### What's NOT Stored

- Screenshots (immediate discard)
- Voice recordings (session-only)
- API keys (encrypted only)
- Clipboard content (optional)

---

## API Key Security

Keys stored using EncryptedSharedPreferences:

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val prefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

## Security Best Practices

1. **Review permissions** — Grant only what's needed
2. **Use local models** — Zero network dependency
3. **Disable voice** — If not needed
4. **Check notifications** — Review what Jarvis reads
5. **Update regularly** — Security fixes

---

## Known Security Considerations

### Accessibility Service

The accessibility service can:
- Read all screen content
- Perform gestures
- Type text
- Launch apps

This is why:
- No network needed for operation
- Local data storage
- Notification privacy guard
- Prompt injection protection

---

## Reporting Security Issues

If you find a security issue:
1. Don't open public issue
2. Email: security@tokenarc.com
3. Include details
4. We'll patch and credit

---

## Security Checklist

Before using:
- [ ] Review permissions granted
- [ ] Enable notification privacy
- [ ] Use local model for sensitive tasks
- [ ] Don't run during sensitive operations
- [ ] Keep app updated