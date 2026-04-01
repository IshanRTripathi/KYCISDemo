````markdown
# 📱 PRD: Demo Android App Using KYC Voice AI SDK (KYCis)

---

# 1. Product Overview

## Objective

Build a **demo Android application** that:

- simulates a **real fintech KYC journey**
- integrates your **KYCis SDK**
- demonstrates **AI-assisted onboarding via voice**
- showcases **drop-off recovery + real-time help**

---

## Why this app matters

This is your:

```text
sales demo
SDK showcase
reference implementation
testing ground
````

---

## App Name (Internal)

```text
KYCis Demo App
```

---

# 2. Core User Flow

```text
Login / Start
   ↓
Personal Details
   ↓
PAN Entry
   ↓
PAN Upload
   ↓
Aadhaar Entry
   ↓
OTP Verification
   ↓
Selfie Capture
   ↓
KYC Complete
```

---

# 3. SDK Integration Points

SDK is active across all screens:

```text
screen tracking
error tracking
time tracking
voice trigger
assistant UI
```

---

# 4. Tech Stack

## 4.1 Android App

```text
Language: Kotlin
UI: Jetpack Compose
Architecture: Clean Architecture (MVVM)
Navigation: Jetpack Navigation
DI: Hilt
Networking: Retrofit
State: ViewModel + StateFlow
Image: Coil
Permissions: Accompanist Permissions
```

---

## 4.2 SDK (Integrated)

```text
KYCis SDK
LiveKit (voice streaming)
```

---

## 4.3 Backend (for demo)

```text
FastAPI
Postgres
Redis
Mock KYC APIs
```

---

# 5. App Architecture

```text
app/
 ├ presentation/
 ├ domain/
 ├ data/
 ├ di/
 ├ navigation/
 └ utils/
```

---

# 6. Folder Structure (Detailed)

```text
app/
 ├ presentation/
 │   ├ screens/
 │   │   ├ login/
 │   │   ├ personal/
 │   │   ├ pan/
 │   │   ├ aadhaar/
 │   │   ├ otp/
 │   │   ├ selfie/
 │   │   └ success/
 │   │
 │   ├ components/
 │   │   ├ TextField.kt
 │   │   ├ UploadCard.kt
 │   │   ├ ErrorView.kt
 │   │   └ Loader.kt
 │   │
 │   ├ viewmodels/
 │   │   ├ KycViewModel.kt
 │   │   └ AuthViewModel.kt
 │
 ├ domain/
 │   ├ models/
 │   │   ├ User.kt
 │   │   ├ KycState.kt
 │   │   └ Document.kt
 │   │
 │   ├ usecases/
 │   │   ├ ValidatePan.kt
 │   │   ├ SubmitKyc.kt
 │   │   └ UploadDocument.kt
 │
 ├ data/
 │   ├ repository/
 │   │   ├ KycRepositoryImpl.kt
 │   │   └ AuthRepositoryImpl.kt
 │   │
 │   ├ remote/
 │   │   ├ ApiService.kt
 │   │   └ NetworkModule.kt
 │
 ├ di/
 │   ├ AppModule.kt
 │   └ NetworkModule.kt
 │
 ├ navigation/
 │   ├ NavGraph.kt
 │   └ Routes.kt
 │
 └ utils/
     ├ Extensions.kt
     └ Constants.kt
```

---

# 7. Screen-Level PRD (VERY DETAILED)

---

## 7.1 Personal Details Screen

### Fields

```text
Full Name
DOB
Phone Number
Email
```

### SDK Integration

```kotlin
AI.trackScreen("personal_details")
```

### Trigger Conditions

```text
User idle > 15s
Invalid email entered twice
```

---

## 7.2 PAN Entry Screen

### Fields

```text
PAN Number
```

### Validation

```text
Regex validation
```

### SDK Events

```kotlin
AI.trackEvent("pan_error", mapOf("type" to "invalid_format"))
```

### Trigger

```text
2+ errors OR 20s idle
```

---

## 7.3 PAN Upload Screen

### Elements

```text
Upload button
Camera capture
Preview card
Retry button
```

### Failure Cases

```text
blurry image
file too large
wrong format
```

### SDK Events

```kotlin
AI.trackEvent("upload_failed")
```

---

## 7.4 Aadhaar Screen

### Fields

```text
Aadhaar number
```

### Edge Cases

```text
invalid length
OTP delay
```

---

## 7.5 OTP Screen

### Elements

```text
OTP input (6 digits)
Resend button
Timer
```

### Trigger

```text
User stuck > 30s
```

---

## 7.6 Selfie Capture

### Features

```text
Camera preview
Face detection
Capture button
```

### Failures

```text
face not detected
low light
```

---

# 8. SDK Integration (Detailed)

---

## Initialization

```kotlin
AI.init(
    apiKey = "demo-key",
    userId = "user_123"
)
```

---

## Screen tracking

Use the SDK step hint (same contract as KYCIS `API_CONTRACTS.md`):

```kotlin
AI.setKycStep("pan_upload")
```

---

## Event tracking

- Field / rule validation → **`validation_failed`** on the backend:

```kotlin
AI.trackValidationFailure(
    failureReasonCode = "pan_invalid",
    componentId = "pan_field",
)
```

- Generic errors (network, non-field) → **`error_reported`**:

```kotlin
AI.trackError("network_timeout", properties = mapOf("endpoint" to "/api/kyc"))
```

---

## Voice Trigger

```kotlin
AI.startAssistant()
```

---

# 9. UI Design for Assistant

---

## Assistant Bubble

```text
floating bottom-right
pulse animation
tap to expand
```

---

## Voice Panel

```text
waveform animation
mute button
end call button
```

---

# 10. State Management

---

## KYC State Model

```kotlin
data class KycState(
    val step: String,
    val errors: List<String>,
    val timeSpent: Int
)
```

---

# 11. Navigation Flow

```text
Login → Personal → PAN → Upload → Aadhaar → OTP → Selfie → Success
```

---

# 12. Error Simulation (IMPORTANT)

You MUST simulate friction:

```text
force PAN error
delay API responses
fail upload randomly
```

This makes AI demo realistic.

---

# 13. Analytics (Basic)

Track:

```text
screen time
drop-off points
errors per screen
AI trigger count
completion rate
```

---

# 14. Testing Strategy

---

## Unit Tests

```text
ViewModels
Validation logic
```

---

## Integration Tests

```text
API calls
SDK events
```

---

## Manual QA

```text
full KYC journey
AI trigger accuracy
voice quality
```

---

# 15. Demo Script (CRUCIAL)

---

### Scenario

```text
User enters wrong PAN
↓
AI triggers
↓
Agent says:
"Looks like there's an issue with your PAN. Let me help."
↓
User corrects
↓
Completes flow
```

---

# 16. Build Timeline

---

## Week 1

```text
Project setup
Navigation
Basic screens
```

---

## Week 2

```text
Form validations
API integration
SDK integration
```

---

## Week 3

```text
Voice UI
Trigger testing
Error simulation
```

---

## Week 4

```text
Polish
Demo readiness
```

---

# 17. Success Criteria

---

## Demo Success

```text
AI triggers correctly
Voice works smoothly
User completes KYC
```

---

## Technical Success

```text
No crashes
Low latency voice
Accurate triggers
```

---

# 🚀 Final Insight

This app is NOT just a demo.

It becomes:

```text
your SDK reference app
your sales weapon
your testing playground
```

Build it extremely polished.

```
```
