# Phase 1 Quick Reference Card
## SDK v2.0 Foundation Enhancements

---

## 🎯 What's New

### 1. SDK Metadata (Automatic)
All events now include SDK identification - no action needed.

### 2. Event Ordering (Best Practice)
Send identity first, then other events.

### 3. Analytics API (New Method)
Track business metrics with `AI.trackAnalytics()`.

---

## 📋 Quick Start

### Proper Event Order
```kotlin
// 1. Identity first
AI.setUser(id = "user123", phone = "+919876543210")

// 2. Then everything else
AI.setKycStep("verification")
AI.trackValidationFailure("invalid_format", componentId = "pan")
```

### Track Analytics
```kotlin
AI.trackAnalytics("kyc_completed", mapOf(
    "duration_seconds" to 120,
    "steps" to 5
))
```

---

## 🔄 Migration

### No Breaking Changes
- All existing code continues to work
- New features are additive
- Event ordering is WARN_ONLY (logs only)

### Recommended Updates
```kotlin
// Before (still works)
AI.trackError("custom_event", mapOf("action" to "completed"))

// After (recommended for metrics)
AI.trackAnalytics("kyc_completed", mapOf("duration" to 120))
```

---

## 📊 Event Payload Changes

### Before
```json
{
  "user_id": "user123",
  "event": "validation_failed",
  "sdk_version": "1.0.0",
  "client": "android-sdk"
}
```

### After (v2.0)
```json
{
  "user_id": "user123",
  "event": "validation_failed",
  "sdk_version": "2.0.0",
  "client": "android-sdk",
  "sdk": {
    "sdk_name": "kycis-android",
    "sdk_version": "2.0.0",
    "platform": "android"
  }
}
```

---

## ⚠️ Important Notes

1. **Event Ordering:** Currently logs warnings only. Will enforce strictly in v2.1.0.
2. **Backend:** Coordinate schema changes to accept nested `sdk` object.
3. **Testing:** Run unit tests before deploying.

---

## 🚀 Coming Next

**Phase 2 (Weeks 3-4):**
- Agent event callbacks (CONNECTED, DISCONNECTED)
- Dynamic popup support

**Phase 3 (Weeks 5-6):**
- EmbedButton UI component
- Navigation Provider
- Flow-based activation

---

## 📚 Full Documentation

- Migration Guide: `KYCIS/android-sdk/PHASE1_MIGRATION_GUIDE.md`
- Changelog: `KYCIS/android-sdk/CHANGELOG.md`
- Examples: `KYCISDemo/app/src/main/java/com/kycis/demo/examples/Phase1Examples.kt`
- Complete Roadmap: `KYCISDemo/ENHANCEMENT_ROADMAP.md`
