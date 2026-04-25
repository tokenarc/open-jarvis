# Open Jarvis — Full Repository Audit Report

## M7 — Pre-Release Security & Bug Audit

---

## Critical Bugs Found

### 1. Accessibility Node Safety
- **File**: `accessibility/ScreenReader.kt`
- **Line**: 9, 38, 72
- **Issue**: AccessibilityNodeInfo references stored and used after screen changes — goes stale immediately
- **Risk**: CRITICAL
- **Fix**: Every node access needs safe wrapper, never store references

### 2. No Race Condition Prevention  
- **File**: `agent/AgentCore.kt`
- **Line**: 64-140
- **Issue**: executeTask() can be called multiple times — no mutex, concurrent tasks possible
- **Risk**: CRITICAL
- **Fix**: Add Mutex() for single task at a time

### 3. GlobalScope Usage
- **File**: Multiple files use `GlobalScope.launch()`
- **Issue**: Coroutines not tied to lifecycle — memory leaks
- **Risk**: HIGH
- **Fix**: Replace with viewModelScope or service lifecycle scope

### 4. Unvalidated LLM Response
- **File**: `agent/AgentCore.kt`
- **Line**: 86-102
- **Issue**: LLM JSON response parsed directly without validation
- **Risk**: HIGH
- **Fix**: Add LLMResponseValidator before ActionJsonParser

### 5. Bitmap Not Recycled
- **File**: `vision/ScreenshotCapture.kt`
- **Issue**: capture() returns bitmap never recycled
- **Risk**: HIGH
- **Fix**: Add try/finally with recycle

### 6. Null Safety - Nullable Actions
- **File**: `agent/ActionPlan.kt`
- **Line**: 6-19
- **Issue**: Action fields use nullable types, potential NPE
- **Risk**: MEDIUM
- **Fix**: Add null checks before every action execution

---

## Security Issues Found

### 1. No Prompt Injection Guard
- **File**: `agent/AgentCore.kt`
- **Issue**: User input goes directly to LLM prompt without sanitization
- **Risk**: CRITICAL
- **Fix**: Add PromptSanitizer before LLM call

### 2. Socket No Auth
- **File**: `bridge/SocketServer.kt`
- **Issue**: Accepts any connection, no UID verification
- **Risk**: HIGH
- **Fix**: Verify connecting process UID

### 3. Notification Privacy
- **File**: `intelligence/JarvisNotificationListener.kt`
- **Issue**: Reads ALL notifications including banking apps
- **Risk**: HIGH
- **Fix**: Add PRIVACY_PROTECTED_APPS block list

### 4. Plain SharedPreferences
- **File**: `ui/OverlayService.kt` (possibly)
- **Issue**: Using SharedPreferences instead of EncryptedSharedPreferences
- **Risk**: MEDIUM
- **Fix**: Audit all prefs usage

---

## Missing Error Handling

### 1. No Timeout on Network
- **File**: `llm/HttpClient.kt`
- **Issue**: 30s default may be too long, no configurable timeout
- **Risk**: MEDIUM

### 2. Room Query No Null Check
- **File**: Multiple DAO files
- **Issue**: queries can return null, handled but could crash
- **Risk**: LOW

### 3. Accessibility Service Null
- **File**: Multiple agent files
- **Issue**: `JarvisAccessibilityService.instance` accessed without null check
- **Risk**: MEDIUM

---

## Performance Issues

### 1. No Adaptive Timing
- **File**: `agent/AgentCore.kt`
- **Issue**: Hardcoded delays (500ms, 2000ms) not device-aware
- **Risk**: LOW
- **Fix**: Add AdaptiveTiming class

### 2. Screen Watch Always Polling
- **File**: `watch/ScreenWatcher.kt`
- **Issue**: 2s polling even when screen off
- **Risk**: MEDIUM
- **Fix**: Pause when screen off (already added in earlier code)

---

## Memory Leaks

### 1. CoroutineScope Not Cancelled
- **File**: Multiple files
- **Issue**: Services may not cancel coroutines in onDestroy()
- **Risk**: HIGH
- **Fix**: Add proper cancellation

### 2. Listener Not Unregistered
- **File**: `MainActivity.kt` 
- **Issue**: If registerReceiver used, needs unregisterReceiver
- **Risk**: MEDIUM

---

## Issues Found by File

### agent/AgentCore.kt
- Line 64: No mutex to prevent concurrent executeTask()
- Line 86-102: No LLM response validation
- Uses GlobalScope in some places

### accessibility/ScreenReader.kt  
- Multiple stale AccessibilityNodeInfo references
- No safeText() or safeClick() wrappers

### bridge/SocketServer.kt
- No process UID verification
- No command length cap

### vision/ScreenshotCapture.kt
- Bitmap never recycled

### intelligence/JarvisNotificationListener.kt
- No privacy protection for banking apps

### ui/OverlayService.kt
- Check for SharedPreferences vs EncryptedSharedPreferences

---

## Fixes Required Priority

### CRITICAL (Must Fix Before Release)
1. Add PromptSanitizer
2. Add task mutex to AgentCore
3. Add LLMResponseValidator
4. Add Socket UID verification
5. Add notification privacy block list

### HIGH
1. Replace all GlobalScope with proper scopes
2. Add Bitmap recycling
3. Add NullPointer safety to Action class
4. Fix ScreenReader stale node references

### MEDIUM
1. Add AdaptiveTiming for wait delays
2. Smart error messages
3. Provider fallback chain

### LOW
1. Performance optimizations
2. App Builder Mode

---

## Recommendations

Given the criticality of issues found, the release should be delayed until:
1. All CRITICAL fixes implemented
2. Full integration test suite passes
3. Security audit by external party

This is a security-critical Android app that controls user device — must be bulletproof before release.