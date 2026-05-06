# KYCIS SDK Feature And Integration Analysis

**Project:** `KYCISDemo`  
**Date:** 2026-05-04  
**Scope:** Current code on disk in:
- `KYCISDemo/app/src/main/java/...`
- `KYCIS/android-sdk/sdk/src/main/java/...`
- `KYCIS/CLIENT_APP_INTEGRATION.md`
- `KYCIS/SDK_BACKEND_ANDROID_SYNC_AUDIT.md`

## 1. Executive Summary

The demo app already uses the KYCIS SDK for the most visible parts of the experience:
- SDK bootstrap via `useKycis(...)`
- lifecycle attachment
- route-driven `AI.setKycStep(...)`
- runtime policy / backend configuration
- voice start / stop orchestration
- LiveKit connection handoff
- dynamic popup checks
- `component_input` reporting on several screens
- screen schema registration

However, the integration is still incomplete relative to what the SDK supports.

The biggest gaps are:
- user identity is only set with a static demo user and never upgraded with real verified user data
- analytics events are not used in production screens
- validation failure telemetry is almost entirely missing from the actual KYC flow
- operational error telemetry is almost entirely missing from the actual KYC flow
- flow lifecycle is incomplete (`setFlow(...)` is used once, but `clearFlow()` / `completeKycFlow()` are not used)
- dynamic popup UX is wired only to a Toast, not to a real intervention surface
- voice context snapshots only cover a small subset of fields
- several SDK UI helpers exist but are not used
- some registered screen schemas are stale / legacy, and some active screens still have incomplete schema coverage

So the current state is:
- **basic integration exists**
- **advanced integration is partial**
- **behavioral telemetry is weak**
- **backend-aware orchestration is underused**

This is why the app feels loosely integrated despite the SDK and backend being capable of much more.

## 2. Capability Inventory

The table below lists the important SDK capabilities visible in the current SDK source, whether the app uses them, and whether that usage is complete.

| SDK capability | Available in SDK | Used in app | Status in app | Notes |
|---|---|---|---|---|
| `AI.init(...)` / `useKycis(...)` | Yes | Yes | Full | Correctly bootstrapped in `MainActivity` |
| `AI.attach(...)` | Yes | Yes | Full | Enabled through `useKycis(... attachToLifecycle = true)` |
| `AI.bindActivity(...)` | Yes | Yes | Full | Manually called in `MainActivity.onCreate()` |
| `AI.onRequestPermissionsResult(...)` | Yes | Yes | Full | Forwarded from `MainActivity` |
| `AI.refreshVoiceAudioFocus()` | Yes | Yes | Full | Called from `MainActivity.onResume()` |
| `RuntimePolicy` | Yes | Yes | Strong / Partial | Good policy usage, but app does not exploit all policy-driven behaviors |
| Remote config / feature flags via `AI.getConfig()` / `AI.isFeatureEnabled()` | Yes | Partial | Partial | Only hint masking is tied to feature flags in app code |
| `AI.setUser(...)` | Yes | Indirectly | Partial | App calls `KycEvent.identity(...)`, but only with static demo data |
| Event ordering helper `KycEvent.identity(...)` | Yes | Yes | Partial | Identity is sent, but not at the correct business milestone with real user data |
| `AI.setKycStep(...)` | Yes | Yes | Partial / Strong | Auto-tracked through `EmbedProvider`, but not perfectly aligned across all routes and schemas |
| `AI.setFlow(...)` / `AI.clearFlow()` | Yes | Partial | Partial | Flow is set to `"onboarding"` only; never switched, cleared, or completed |
| `AI.completeKycFlow(...)` | Yes | No | Not integrated | End of journey does not formally complete SDK flow/session |
| `AI.trackAnalytics(...)` | Yes | Only in examples | Not integrated in production | Major gap |
| `AI.trackError(...)` | Yes | Only in diagnostics screen | Not integrated in production | Major gap |
| `AI.trackValidationFailure(...)` | Yes | Only in diagnostics screen | Not integrated in production | Major gap |
| `AI.reportComponentInput(...)` / `KycEvent.componentInput(...)` | Yes | Yes | Partial | Used in many screens, but coverage and depth are inconsistent |
| Screen schema registry | Yes | Yes | Partial | Good foundation, but contains stale legacy screens and incomplete coverage |
| `EmbedProvider` | Yes | Yes | Strong / Partial | Good usage for auto-tracking; route inclusion/exclusion choices need refinement |
| `EmbedButton` / draggable embed button | Yes | No | Not used | SDK UI capability available but unused |
| SDK `KycTextField` helpers | Yes | Wrapper exists | Not used | App has a wrapper but uses native fields almost everywhere |
| Dynamic popup evaluation | Yes | Yes | Partial | Auto-check exists, but rendering and analytics are minimal |
| Agent event callbacks (`AI.setAgentEventListener`) | Yes | Only in examples | Not integrated in production | Missed opportunity for richer UX / analytics |
| Voice session listener (`AI.setVoiceSessionListener`) | Yes | Yes | Full / Partial | Properly used for LiveKit credentials; surrounding lifecycle analytics are missing |
| Voice FAB host / voice UI primitives | Yes | Yes | Partial / Strong | Good usage, but not fully connected to agent events, analytics, or complete context |
| Trace propagation headers `AI.traceHeadersForRequest(...)` | Yes | No | Not integrated | Useful if host app makes its own backend calls |
| Manual passive-idle reset `AI.onUserInteractionObserved()` | Yes | No | Not integrated | Could reduce false idle-trigger behavior |
| Feature-aware completion / session rotation | Yes | No | Not integrated | `completeKycFlow()` not used |
| Security helpers (`SecurityConfig`) | Present in SDK source | No | Not integrated | No host wiring found in current demo integration path |
| Offline event queue (`OfflineEventQueue`) | Present in SDK source | No | Not integrated | No host wiring found in current demo integration path |
| Voice overlays (`VoiceCallOverlay`, `VoiceCallPillOverlay`) | Yes | No | Not used | App uses `VoiceFabHost` instead |

## 3. What Is Already Done Well

These are the strongest parts of the current integration.

### 3.1 SDK bootstrap is correct

The app initializes the SDK through `useKycis(...)` in `MainActivity`, passing:
- `apiKey`
- `userId`
- `RuntimePolicy`
- lifecycle auto-attach
- popup callback

This gives the app:
- a proper initialization lifecycle
- loading / ready / error states
- status callbacks
- automatic lifecycle attachment

### 3.2 Runtime policy is meaningfully configured

The app already sets:
- `backendBaseUrl`
- `clientId`
- `mappingVersion`
- `appVersion`
- `triggerStartMode = CONFIRM_UI`
- `kycStepStrategy = HINT_THEN_INFER`
- passive evaluation settings
- trigger settings
- confirm UI copy
- component hint masking policy

This is one of the strongest parts of the app-side integration.

### 3.3 Voice session start / stop is actively integrated

The app does more than a superficial SDK demo:
- receives voice session credentials through `AI.setVoiceSessionListener(...)`
- connects to LiveKit using `VoiceRoomConnector`
- renders `VoiceFabHost`
- manages mute / unmute
- stops the assistant when the call ends
- refreshes audio focus on resume
- requests microphone permission

This is a legitimate integration, not just a placeholder.

### 3.4 Auto route tracking exists

`EmbedProvider` is used with explicit route configuration, and route normalization is handled for parameterized routes like:
- `phone_otp/{phone}`
- `email_otp/{email}`

That is the right architectural direction.

### 3.5 Screen schemas are registered before init

`KycDemoApplication` registers screen schemas in `Application.onCreate()` before `AI.init(...)` happens later through `useKycis(...)`.

That part is correct and aligns with the SDK guidance.

## 4. App-Wide Gaps

This section explains where the demo falls short across the whole app, not just on one screen.

### 4.1 Identity is technically present but business-wise weak

Current behavior:
- `useKycis(...)` initializes with `userId = "demo-user"`
- `KycEvent.identity(userId = "demo-user")` is called after readiness

Why this is incomplete:
- the identity is static, not tied to the actual user entering phone / email
- the phone number is never attached to SDK identity after verification
- the email is never attached as event context through analytics
- there is no identity reset on logout / session restart

What to add:
- after phone OTP success, call `AI.setUser(id = resolvedUserId, phone = verifiedPhone, phoneMasked = false or true based on policy)`
- keep `KycEvent.identity(...)` consistent with the same verified identity if you want to stay on the wrapper API
- if this demo supports multiple runs for the same tester, rotate or derive identity per session

Where to add:
- `PhoneOtpScreen.kt` after successful verification
- optionally centralize in `NavGraph.kt` or a small flow state holder if user identity is stored globally

### 4.2 Flow lifecycle is incomplete

Current behavior:
- `AI.setFlow("onboarding")` is called once
- there is no `AI.clearFlow()`
- there is no `AI.completeKycFlow(...)`
- the Home screen shows two flows (`KYC Flow` and `MFD Support`) but this selection does not drive SDK flow context

Impact:
- backend cannot distinguish one completed journey from the next as cleanly as it could
- session rotation at flow completion does not happen
- multi-flow capability is effectively unused

What to add:
- call `AI.setFlow("onboarding")` only when user starts that flow
- if `MFD Support` is real, wire it to a different flow name such as `"mfd_support"`
- on successful signature submit / final completion, call `AI.completeKycFlow(...)`
- if user abandons the journey and returns home, consider `AI.clearFlow()`

Where to add:
- `HomeScreen.kt`
- `NavGraph.kt`
- `SignatureScreen.kt`

### 4.3 Business analytics are missing

The SDK supports `AI.trackAnalytics(...)`, but the production KYC screens do not use it.

What is missing:
- journey started
- step started
- step completed
- otp requested
- otp resent
- otp verified
- document upload started / success / failure
- DigiLocker chosen vs offline upload chosen
- selfie completed
- signature completed
- flow completed
- flow abandoned

Impact:
- the backend gets low-quality behavioral telemetry
- drop-off analysis is weak
- popup / trigger tuning lacks business event correlation

What to add:
- `kyc_journey_started`
- `kyc_step_completed`
- `otp_requested`
- `otp_verified`
- `document_upload_success`
- `digilocker_path_selected`
- `offline_path_selected`
- `selfie_completed`
- `signature_submitted`
- `kyc_journey_completed`
- `form_abandoned`

Where to add:
- all KYC screens
- especially `HomeScreen.kt`, `PhoneEntryScreen.kt`, `PhoneOtpScreen.kt`, `EmailEntryScreen.kt`, `EmailOtpScreen.kt`, `VerifyDocumentsScreen.kt`, `UploadAadhaarScreen.kt`, `SelfieCaptureScreen.kt`, `SignatureScreen.kt`

### 4.4 Validation telemetry is almost absent in the real flow

The SDK supports structured validation reporting through `AI.trackValidationFailure(...)`.  
The demo uses field validation visually in Compose, but does not report most of those failures to the SDK / backend.

Examples of missing validation telemetry:
- invalid phone
- invalid phone OTP
- invalid email
- invalid email OTP
- invalid PAN
- invalid DOB
- empty required personal details
- invalid Aadhaar number in DigiLocker flow
- upload failures / invalid file type / oversize file
- selfie/liveness failure

Impact:
- backend trigger logic loses one of the most useful intervention signals
- voice assistant cannot react with precision to the actual failure reason
- schema registry is underutilized because error codes are not emitted

What to add:
- whenever a field becomes invalid, report a failure with `failureReasonCode`, `componentId`, `componentType`, `validationRuleId`, and masked `hint` where appropriate
- resend validation only on meaningful transitions, not every keystroke

Where to add:
- every form screen
- especially `PhoneEntryScreen.kt`, `PhoneOtpScreen.kt`, `EmailEntryScreen.kt`, `EmailOtpScreen.kt`, `PanDetailsScreen.kt`, `PersonalDetailsScreen.kt`, `DigilockerAadhaarScreen.kt`, `UploadAadhaarScreen.kt`

### 4.5 Error telemetry is almost absent in the real flow

The SDK supports operational error reporting via `AI.trackError(...)`, but the demo only uses it in `SdkDiagnosticsScreen`.

Missing error reporting opportunities:
- backend URL test failure
- OTP request failure
- OTP verify failure
- document picker / camera failure
- file upload failure
- voice connection failure
- permission denied
- network timeout
- DigiLocker failure
- signature submission failure

Impact:
- backend sees less operational context than the app actually has
- intervention scoring cannot use real error bursts
- support / QA triage becomes harder

Where to add:
- every async or failure-prone action
- particularly `BackendSettingsScreen.kt`, `PhoneOtpScreen.kt`, `EmailOtpScreen.kt`, `UploadAadhaarScreen.kt`, `SelfieCaptureScreen.kt`, `SignatureScreen.kt`, `MainActivity.kt`

### 4.6 Dynamic popup UX is only partially integrated

Current behavior:
- popup checks are wired through `EmbedProvider`
- `AI.checkForDynamicPopup()` is called
- `onPopup` receives backend popup data
- UI shows a Toast

What is missing:
- actual modal / bottom sheet / inline intervention UI
- accept / dismiss callbacks
- analytics for popup impression / accept / dismiss
- step-specific actions after popup acceptance

Impact:
- one of the strongest SDK/backend intervention surfaces is reduced to a transient message
- the user cannot meaningfully act on the intervention

What to add:
- render a real Compose `AlertDialog` or bottom sheet
- on accept: call `AI.startAssistant()` or route to a guided help state
- on dismiss: track analytics
- tie popup reason code to custom UX branches

Where to add:
- `MainActivity.kt` or a dedicated popup state holder / composable

### 4.7 Voice context snapshot is too narrow

Current behavior:
- app populates `VoiceUiSnapshotHolder`
- current implementation mainly tracks `personal_details`

What is missing:
- phone number state
- phone OTP state
- email state
- email OTP state
- PAN + DOB state
- DigiLocker Aadhaar state
- upload state
- signature state

Impact:
- the voice layer gets incomplete form-state context
- assistant can help well only on the personal details step

What to add:
- extend `VoiceUiSnapshotHolder` metadata and per-screen field tracking
- update snapshots from all important screens, not just one

Where to add:
- `VoiceUiSnapshotHolder.kt`
- all major screen files with inputs

### 4.8 Screen schema registry is useful but not clean yet

Current behavior:
- the app registers many useful schemas
- but it still contains legacy steps:
  - `pan_entry`
  - `pan_upload`
  - `aadhaar_entry`
  - `otp_verify`
- active route `backend_settings` has no schema
- `home` has a schema, but `EmbedProvider` excludes `home`, so the route is not auto-tracked there

Impact:
- backend receives a mixed picture of legacy and active journeys
- some real routes do not have strong schema support
- some screens are tracked without a matching schema

What to add / fix:
- remove or separate legacy schema definitions if they are no longer used
- add schema for `backend_settings` only if you want it analyzed by the backend
- decide whether `home` should be tracked or intentionally excluded
- keep `ScreenSchema.screenId` perfectly aligned with `AI.setKycStep(...)`

Where to add:
- `KycDemoApplication.kt`
- `MainActivity.kt`

### 4.9 SDK helper components exist but are barely used

Available in SDK:
- `EmbedButton`
- draggable `EmbedButton`
- `KycTextField`
- `KycOutlinedTextField`

Current app behavior:
- custom fields are used almost everywhere
- app has its own wrapper `presentation/components/KycTextField.kt`
- the wrapper is not actually used in the current screens

Impact:
- repeated custom event wiring across screens
- inconsistent debounce / masking / context behavior
- more manual maintenance than necessary

What to add:
- either fully commit to custom fields and centralize helper logic
- or migrate repeated text inputs to SDK `KycTextField` / `KycOutlinedTextField`

Best candidates for migration:
- `PhoneEntryScreen.kt`
- `EmailEntryScreen.kt`
- `PanDetailsScreen.kt`
- DigiLocker Aadhaar input widgets

### 4.10 Agent event callbacks are not used in production

SDK supports:
- `CONNECTED`
- `DISCONNECTED`
- `POPUP_VISIBLE`
- `TRANSCRIPTION_RECEIVED`

Current app behavior:
- only example files use `AI.setAgentEventListener(...)`
- production code uses only `AI.setVoiceSessionListener(...)`

Impact:
- no call-duration analytics
- no connected / disconnected UX states tied to actual agent lifecycle
- no popup-specific analytics through event stream
- no transcript-level enrichment hooks

What to add:
- register an agent event listener in `MainActivity`
- track voice call start / end analytics
- use popup events for richer intervention tracking
- optionally surface transcript metadata in a debug / QA layer

## 5. Screen-By-Screen Analysis

This section maps the actual KYC journey against the SDK capability set.

### 5.1 `HomeScreen`

Current integration:
- no SDK analytics
- flow selection UI does not drive `AI.setFlow(...)`
- start button just navigates

Status: **Partial**

What to add:
- on Start click: `AI.trackAnalytics("kyc_journey_started", ...)`
- when user selects `KYC Flow` vs `MFD Support`, set the SDK flow accordingly
- if `MFD Support` is not implemented yet, either remove the selection or explicitly keep SDK flow fixed to avoid misleading telemetry

### 5.2 `PhoneEntryScreen`

Current integration:
- sends `component_input`
- includes strong `sdkKb`
- good debounce behavior

Missing:
- validation failure telemetry
- analytics for OTP request
- identity upgrade when phone is the first real user identifier

Status: **Partial, but better than average**

Add here:
- `AI.trackValidationFailure("phone_invalid", componentId = "phone_field", ...)`
- `AI.trackAnalytics("otp_requested", mapOf("channel" to "phone"))`

### 5.3 `PhoneOtpScreen`

Current integration:
- sends OTP `component_input`

Missing:
- OTP validation failure events
- resend analytics
- verify success / failure analytics
- user identity finalization after successful verification

Status: **Partial**

Add here:
- `AI.trackValidationFailure("phone_otp_invalid", componentId = "phone_otp_field", ...)`
- `AI.trackAnalytics("otp_resent", mapOf("channel" to "phone"))`
- `AI.trackAnalytics("otp_verified", mapOf("channel" to "phone"))`
- `AI.setUser(...)` on successful verification

### 5.4 `EmailEntryScreen`

Current integration:
- sends `component_input`

Missing:
- `sdkKb`
- validation failure telemetry
- analytics for email OTP request

Status: **Partial**

Add here:
- richer `sdkKb`
- `AI.trackValidationFailure("email_invalid", componentId = "email_field", ...)`
- `AI.trackAnalytics("otp_requested", mapOf("channel" to "email"))`

### 5.5 `EmailOtpScreen`

Current integration:
- sends OTP `component_input`

Missing:
- invalid OTP telemetry
- resend analytics
- verify success / failure analytics

Status: **Partial**

Add here:
- `AI.trackValidationFailure("email_otp_invalid", componentId = "email_otp_field", ...)`
- `AI.trackAnalytics("otp_verified", mapOf("channel" to "email"))`

### 5.6 `PanDetailsScreen`

Current integration:
- PAN field reports `component_input`
- PAN field includes KB

Missing:
- DOB field telemetry
- checkbox / consent telemetry
- PAN validation failure telemetry
- DOB validation failure telemetry
- step completion analytics

Status: **Partial**

Add here:
- `component_input` for `dob_field`
- analytics for terms accepted
- validation failures for invalid PAN and DOB
- analytics for PAN step completed

### 5.7 `PersonalDetailsScreen`

Current integration:
- best telemetry coverage in the app
- multiple `component_input` events
- dropdown selections reported
- `VoiceUiSnapshotHolder` updated here

Missing:
- validation failure telemetry
- analytics for completion
- error telemetry for submit failures

Status: **Best integrated screen, but still partial**

Add here:
- `AI.trackValidationFailure(...)` for missing required fields
- `AI.trackAnalytics("kyc_step_completed", mapOf("step" to "personal_details"))`

### 5.8 `VerifyDocumentsScreen`

Current integration:
- choice buttons send `component_input`

Missing:
- analytics for branch selection
- flow-branch telemetry
- richer context around why user chose DigiLocker vs offline path

Status: **Partial**

Add here:
- `AI.trackAnalytics("digilocker_path_selected", ...)`
- `AI.trackAnalytics("offline_path_selected", ...)`

### 5.9 `DigilockerAadhaarScreen`

Current integration:
- final concatenated Aadhaar input is sent on Next click

Missing:
- per-part or debounced full-field reporting during entry
- validation failure telemetry
- richer `sdkKb`
- analytics for DigiLocker continuation

Status: **Partial and too thin for a critical step**

Add here:
- report Aadhaar as user types or after debounce
- add validation failure when input is not 12 digits
- send branch analytics

### 5.10 `UploadAadhaarScreen`

Current integration:
- sends `component_input` when upload is marked complete

Missing:
- real file picker/camera integration
- upload start / success / failure analytics
- error telemetry
- metadata like source, size, mime type, retry count

Status: **Partial / demo-only**

Add here:
- `AI.trackAnalytics("document_upload_started", ...)`
- `AI.trackAnalytics("document_upload_success", ...)`
- `AI.trackError("document_upload_failed", ...)`
- `AI.trackValidationFailure(...)` for unsupported type / oversize

### 5.11 `SelfieCaptureScreen`

Current integration:
- reports final `component_input`

Missing:
- actual liveness / camera failure telemetry
- analytics for capture success
- retry / fail telemetry

Status: **Partial / demo-only**

Add here:
- `AI.trackAnalytics("selfie_capture_started")`
- `AI.trackAnalytics("selfie_capture_completed")`
- `AI.trackValidationFailure("liveness_failed", ...)`
- `AI.trackError("camera_capture_failed", ...)`

### 5.12 `SignatureScreen`

Current integration:
- reports a final button event

Missing:
- actual signature capture context
- completion analytics
- final SDK flow completion

Status: **Partial**

Add here:
- `AI.trackAnalytics("signature_submitted")`
- `AI.completeKycFlow(mapOf("completion_surface" to "signature"))`

This is the most important place to formally close the journey.

### 5.13 `SdkDiagnosticsScreen`

Current integration:
- very useful manual harness
- already uses:
  - `AI.setKycStep(...)`
  - `AI.trackValidationFailure(...)`
  - `AI.checkForDynamicPopup()`
  - `AI.trackError(...)`

Status: **Good harness integration**

Important note:
- this screen proves the SDK features exist
- it does **not** mean the main KYC flow uses them properly

### 5.14 `BackendSettingsScreen`

Current integration:
- useful operational screen
- no clear schema registration
- likely no analytics around health test / URL change

Status: **Outside main KYC flow, but under-instrumented**

Add here if useful:
- analytics for backend URL change
- error telemetry for health-check failure
- optional schema only if you want intervention logic on this screen

## 6. Completeness Rating By Area

### Fully integrated

- SDK initialization and ready-state handling
- lifecycle attachment
- activity binding
- permission result forwarding
- audio focus refresh
- voice credential handoff with LiveKit

### Strong but still incomplete

- runtime policy setup
- route-based screen tracking
- voice FAB / voice room UX
- schema registry foundation

### Partial

- identity management
- flow context management
- popup support
- component input coverage
- voice context snapshot coverage
- per-screen schema alignment
- feature-flag-driven app behavior

### Barely integrated or not integrated

- production analytics
- production validation failure telemetry
- production error telemetry
- agent event callbacks
- formal flow completion
- manual idle suppression via `onUserInteractionObserved()`
- trace headers on host-side API calls
- SDK helper UI fields
- `EmbedButton`

## 7. Recommended File Changes

This is the most practical section: what to add, and where.

### `MainActivity.kt`

Add / improve:
- real popup UI state instead of Toast-only handling
- `AI.setAgentEventListener(...)`
- analytics for popup shown / accepted / dismissed
- analytics for voice connected / disconnected
- optional call to `AI.onUserInteractionObserved()` from touch dispatch if you want stronger idle suppression
- verify whether `home` should be excluded from auto-tracking or only from button visibility

### `KycDemoApplication.kt`

Add / improve:
- remove stale legacy schemas if no longer used
- add missing active-screen schemas if needed
- keep active route names perfectly aligned with real navigation

### `VoiceUiSnapshotHolder.kt`

Add / improve:
- metadata for phone, OTP, email, PAN, DOB, Aadhaar, upload, signature fields
- per-screen component state storage
- branch / completion status where useful

### `HomeScreen.kt`

Add:
- journey start analytics
- flow selection analytics
- actual flow switching via `AI.setFlow(...)`

### `PhoneEntryScreen.kt`

Add:
- validation failure telemetry
- OTP request analytics

### `PhoneOtpScreen.kt`

Add:
- invalid OTP telemetry
- resend analytics
- verify success analytics
- `AI.setUser(...)` on success

### `EmailEntryScreen.kt`

Add:
- richer KB
- validation failure telemetry
- OTP request analytics

### `EmailOtpScreen.kt`

Add:
- invalid OTP telemetry
- resend analytics
- verify success analytics

### `PanDetailsScreen.kt`

Add:
- DOB `component_input`
- PAN / DOB validation failure telemetry
- consent analytics
- step completion analytics

### `PersonalDetailsScreen.kt`

Add:
- validation failure telemetry
- completion analytics
- submit failure error telemetry

### `VerifyDocumentsScreen.kt`

Add:
- branch-selection analytics

### `DigilockerAadhaarScreen.kt`

Add:
- Aadhaar input reporting during entry, not just on submit
- validation failure telemetry
- branch analytics

### `UploadAadhaarScreen.kt`

Add:
- upload started / success / failure analytics
- file validation / error telemetry
- real upload context, not only mock filename

### `SelfieCaptureScreen.kt`

Add:
- liveness analytics
- capture failure telemetry
- retry telemetry

### `SignatureScreen.kt`

Add:
- final completion analytics
- `AI.completeKycFlow(...)`

## 8. Suggested Priority Order

If the goal is to make the app feel properly integrated quickly, do the work in this order.

### Phase 1: High-value telemetry foundation

1. Add real `AI.setUser(...)` after verification
2. Add `AI.trackValidationFailure(...)` across all form screens
3. Add `AI.trackAnalytics(...)` for step completion and branch decisions
4. Add `AI.trackError(...)` for all async failures

### Phase 2: Strengthen orchestration

5. Add `AI.completeKycFlow(...)` at final completion
6. Wire `AI.setFlow(...)` to actual flow selection
7. Clean up screen schemas in `KycDemoApplication.kt`
8. Extend `VoiceUiSnapshotHolder` to all major screens

### Phase 3: Improve intervention UX

9. Replace Toast-only popup handling with real UI
10. Add `AI.setAgentEventListener(...)`
11. Add popup / voice session analytics

### Phase 4: Reduce manual inconsistency

12. Migrate repeated text-entry patterns to SDK field helpers or a single shared wrapper
13. Add host-side trace headers for any direct backend calls if needed
14. Consider manual `AI.onUserInteractionObserved()` integration if passive idle triggering needs tighter accuracy

## 9. Bottom-Line Assessment

### Is the SDK integrated?

**Yes, but mainly at the infrastructure layer.**

The app has:
- real SDK startup
- real screen tracking
- real voice session wiring
- real runtime policy
- real component input reporting

### Is the SDK integrated properly relative to its full capability?

**No, not yet.**

The app is missing the parts that make the SDK feel deeply integrated:
- behavioral analytics
- failure telemetry
- operational telemetry
- identity evolution
- formal flow completion
- popup UX
- rich snapshot coverage
- agent event callbacks

### Is there still scope to add SDK features in other screens / places?

**Yes, a lot.**

The biggest unused value is not in adding more startup code.  
It is in adding **context-rich, screen-specific telemetry and intervention wiring throughout the journey**.

That is the difference between:
- "SDK is present in the app"

and

- "SDK is actually powering the journey intelligently"

## 10. Most Important Next Step

If only one improvement batch is done next, it should be this:

1. add `AI.setUser(...)` after verification  
2. add `AI.trackValidationFailure(...)` on every form screen  
3. add `AI.trackAnalytics(...)` for journey + step completion  
4. add `AI.completeKycFlow(...)` on final submit  

That single batch would move the app from loosely integrated to meaningfully integrated.

## 11. Verified Technical Analysis Addendum (Code-Backed, Zero-Assumption)

This addendum is based only on the currently checked-in code across:

- demo app: `KYCISDemo`
- Android SDK: `KYCIS/android-sdk`
- backend + voice agent: `KYCIS/backend`

No intermediate behavior below is assumed unless the code explicitly implements it.

---

## 11.1 Email Entry Screen Behavior Analysis

### Scope of the exact scenario

Scenario requested:

1. user partially enters an email on the email entry screen
2. user asks the voice agent whether it is correct

The actual implementation supports multiple materially different paths depending on timing and whether the user speaks the value aloud. Those branches are documented separately below.

### A. App-screen execution path on `EmailEntryScreen`

File:
- `app/src/main/java/com/kycis/demo/presentation/screens/EmailEntryScreen.kt`

Current screen behavior:

1. local Compose state:
   - `email` is held in `remember { mutableStateOf("") }`
   - every keystroke updates only local UI state through `onValueChange = { email = it }`

2. local validation:
   - the screen uses local regex:
     - `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,6}$`
   - `isEmailValid = email.matches(emailRegex)`
   - this validation is purely local to the screen
   - no backend validation call is triggered by typing
   - no SDK `trackValidationFailure(...)` call is triggered when the regex fails

3. UI response:
   - `OutlinedTextField.isError = email.isNotEmpty() && !isEmailValid`
   - supporting text becomes `Enter a valid email address`
   - `Get OTP` button is disabled unless `isEmailValid == true`

4. debounced SDK transmission:
   - `LaunchedEffect(email)` waits `600 ms`
   - after `600 ms`, `debouncedEmail = email`
   - `LaunchedEffect(debouncedEmail)` sends:
     - `KycEvent.componentInput(...)`
     - `componentId = "email_field"`
     - `value/hint = debouncedEmail`
     - `screen = "email_entry"`
     - `componentType = "text_input"`

Important consequence:

- a partially typed email is not sent immediately
- it is sent only if the user pauses long enough for the `600 ms` debounce to complete

### B. How the screen becomes backend-visible context

There are two independent context streams for this screen:

1. route/screen context
2. debounced component input context

#### Screen context path

1. navigation enters `Routes.EMAIL_ENTRY`
2. `MainActivity` destination listener normalizes the route to `email_entry`
3. `providerState.updateRoute("email_entry")`
4. `EmbedProvider` observes the route and calls `AI.setKycStep("email_entry")`
5. `SdkRuntime.setKycStep(...)` sends:
   - `screen_state` event to `/events`
   - and, because schema `email_entry` is registered, `screen_schema` or lazy schema push logic for that screen

Result:

- backend session state gets current screen = `email_entry`
- backend context route `/assistant/context/{session_id}` can later return `current_screen = email_entry`

#### Component input path

1. `KycEvent.componentInput(...)`
2. `AI.reportComponentInput(...)`
3. `SdkRuntime.reportComponentInput(...)`
4. `HttpBackendClient.reportComponentInput(...)`
5. SDK POSTs `/events` with:
   - `event = "component_input"`
   - `component_id = "email_field"`
   - `screen = "email_entry"`
   - `component_type = "text_input"`
   - `hint = <raw debounced email>`
   - `masked = false` in this demo, because `RuntimePolicy.componentInputHintsMasked = false`

Important current fact:

- the demo intentionally sends the raw typed email value to the backend
- however the backend does not expose that raw value directly to the LLM prompt; it converts it into metadata in the prompt context

### C. Backend ingestion behavior for the email component input

File:
- `backend/app/routes/events.py`

`component_input` is in `_SYNC_EVENT_TYPES`, so the backend stores it synchronously before returning the HTTP response.

Actual sequence:

1. `/v1/events` receives `event = "component_input"`
2. route normalizes payload synchronously
3. `store.upsert_event(dumped)` executes synchronously
4. `SessionState.component_inputs["email_field"]` is updated synchronously
5. if a voice session is active, backend schedules `_signal_voice_agent_context_updated(session_id)`

This is the most important current real-time property:

- email component input does get immediate backend persistence
- therefore it is one of the data types that is currently synchronized quickly enough to support near-real-time voice context refresh

### D. How the voice agent gets email-entry context

Voice start path:

1. user taps voice FAB
2. `AI.startAssistant()`
3. `SdkRuntime.startAssistantSession(...)`
4. SDK POSTs `/assistant/session/start`
5. backend creates/starts assistant session and returns LiveKit credentials
6. app connects to LiveKit using `VoiceRoomConnector`
7. Python voice agent joins the room
8. voice agent fetches `/assistant/context/{session_id}`

Context fetch contents:

- `context = build_kyc_context(session_id)`
- `system_prompt = build_system_prompt(session_id)`
- `current_screen`
- recent transcript turns

For `email_entry`, `build_kyc_context(...)` includes:

- current screen label
- screen schema-derived field list
- component input metadata for `email_field`

Critical detail:

- `build_kyc_context(...)` intentionally does **not** pass the raw email value to the LLM
- instead it exposes metadata such as:
  - char count
  - digit count
  - letter count
  - pattern hints / validation rules
  - filled vs empty status

So the backend stores the raw email, but the LLM-facing prompt hides the literal value.

### E. Exact behavior when user asks "is this email correct?"

There are three different real code-backed cases.

#### Case 1: user asks before the `600 ms` debounce completes

What is guaranteed:

- the local screen has the latest text in `email`
- the UI shows local regex error immediately

What is **not** guaranteed:

- backend has not necessarily received the latest partial email
- voice agent context may still reflect older input or no input

Therefore current behavior is timing-sensitive:

- if the user speaks to the agent before the debounce finishes, the voice agent may not have the newest typed email context

#### Case 2: user pauses long enough, debounce completes, and then asks without speaking the actual email aloud

What happens:

1. backend now has `component_input` for `email_field`
2. if voice session is active, backend sends LiveKit `context_updated`
3. voice agent receives push and refreshes context
4. refreshed system prompt includes metadata for the email field

What the prompt instructs:

- for "check my screen / is it correct" intents, inspect `USER INPUTS`
- do not echo the actual value
- only call `validate_input` when the user explicitly speaks the value in the current turn

What this means in practice:

- the agent can confirm that the field is filled
- the agent can use metadata and validation rules in the prompt
- but it does **not** reliably have the raw user input available as an LLM tool argument

Current architecture limitation:

- the `validate_input` tool requires exact `user_input`
- the prompt context intentionally hides the raw typed email from the model
- therefore a screen-only question like "is it correct?" cannot be deterministically validated by the tool unless the user also speaks the value aloud

#### Case 3: user speaks the email aloud in the same utterance

Example:
- "I entered `usergmailcom`, is that correct?"
- "I typed `user@gmail.com`, is it valid now?"

Then the voice flow can validate deterministically:

1. STT transcript is captured
2. the LLM can call `validate_input(user_input=spoken_value, component_type="email", validations=...)`
3. `validate_input` in `voice_agent.py` runs deterministic email checks
4. it returns verdict:
   - missing `@`
   - spaces present
   - invalid format
   - possible domain typo
   - valid email format
5. the agent summarizes the verdict verbally

This is the only fully deterministic current correctness path for the exact value.

### F. Current validation layers for the email scenario

#### Validation layer 1: local screen validation

Implemented:

- local regex on `EmailEntryScreen`
- immediate visual error state
- `Get OTP` blocked until regex passes

Not implemented:

- no SDK validation-failure event for bad email
- no backend validation event on typing failure
- no analytics event for invalid email attempts

#### Validation layer 2: backend session context update

Implemented:

- debounced `component_input`
- synchronous backend persistence
- immediate voice context push when voice session is already active

#### Validation layer 3: voice tool validation

Implemented:

- deterministic email validator inside `voice_agent.py`

Constraint:

- it validates the **spoken** value, not the hidden raw screen value

### G. Timing characteristics actually present in code

Verified timings:

- email input debounce before SDK send: `600 ms`
- SDK passive evaluation interval in demo policy: `10 s`
- voice agent periodic context refresh default in code: `5 s`
- user-turn refresh minimum interval default in code: `3 s`
- inline context fetch timeout default: `1.2 s`
- normal context fetch timeout default: `5.0 s`
- event POST timeout in voice agent: `3.5 s`

Important code/comment mismatches:

- top comment in `voice_agent.py` says `VOICE_CONTEXT_REFRESH_SECONDS` default is `60`
- actual runtime code uses default `5.0`
- top comment says user-turn refresh min interval default is `5`
- actual runtime code uses `3.0`

So any architecture discussion should trust the executable code, not the stale comment block.

### H. Current-behavior verdict for the requested scenario

Is the current partial-email + voice-check flow "right" end-to-end?

**No, not fully.**

Why:

1. the screen validates locally but does not emit `validation_failed` telemetry
2. correctness of the exact typed email is not deterministically available to the voice tool unless the user speaks the value aloud
3. prompt instructions are internally contradictory:
   - one section says to validate filled fields during screen-check intent
   - another says do not call `validate_input` unless the user explicitly states the value in that turn

### I. Required correction for this flow

#### Issue 1: screen-only correctness cannot be deterministically validated

Current behavior:

- backend stores raw email
- LLM prompt hides raw email
- `validate_input` requires exact `user_input`

Expected behavior after fix:

- either:
  - add a backend validation endpoint that operates on stored session component values without exposing them to the LLM
- or:
  - send a precomputed validation verdict into context for the current field
- or:
  - explicitly constrain agent behavior to: "I can confirm format only if you say the email aloud"

#### Issue 2: no validation-failure telemetry from `EmailEntryScreen`

Current behavior:

- invalid partial email stays local to the screen
- backend/LLM cannot distinguish "field is partially typed and invalid" from "field is merely filled"

Expected behavior after fix:

- when local regex fails after user pause or submit attempt, emit:
  - `AI.trackValidationFailure("email_invalid", componentId = "email_field", componentType = "email", ...)`

#### Issue 3: contradictory prompt rules for screen-check validation

Current behavior:

- prompt says both:
  - validate filled fields during screen-check intent
  - only call `validate_input` when the user explicitly states the value in the current turn

Expected behavior after fix:

- keep one rule only
- recommended rule:
  - if value is not spoken aloud, do not claim deterministic validation of the exact value
  - instead say whether the field appears filled and what format is required

---

## 11.2 Voice Agent Response Constraints Investigation

### A. Hard limits found in code

#### Maximum spoken characters

No enforced maximum spoken-character limit was found in:

- `backend/voice_agent.py`
- `backend/app/services/kyc_context.py`
- `backend/app/services/kyc_language_engine.py`
- `backend/app/voice_providers.py`

There is no code that truncates assistant replies by character count before TTS.

#### Maximum LLM output tokens

No explicit `max_tokens`, `max_output_tokens`, or equivalent LLM response cap was found in:

- `backend/app/voice_providers.py`
- `backend/voice_agent.py`

The LLM provider is constructed with provider/model selection and temperature, but no explicit response-length cap is configured in the checked-in code.

#### Maximum prompt/context length

Config exists:

- `backend/config/kycis.yaml`
- `ai.max_context_length_chars: 4000`

But enforcement was **not** found in the executable context-building path.

Result:

- this looks like a configuration placeholder, not an enforced runtime constraint

### B. Actual response-guideline instructions found in the system prompt

The agent is strongly guided by prompt instructions in `build_system_prompt(...)` and `voice_agent_fallback_instructions(...)`.

Verified response-style constraints include:

1. brevity:
   - "Keep answers brief for text-to-speech unless the user asks for more detail."
   - "Use short responses by default"
   - "Prefer short sentences suitable for text-to-speech."
   - "Give exactly one clear next action per turn whenever possible."

2. empathy / tone:
   - "Sound like a supportive human KYC executive"
   - "Keep tone friendly and practical"
   - telephony mode: "Be empathetic and supportive, never pushy."

3. grounding:
   - do not invent screen ids, values, or outcomes
   - do not echo entered values
   - ask a short clarifying question if context is unclear

4. no raw technical output:
   - never read tool JSON, function names, raw ids, or raw snake_case screen ids aloud unless explicitly asked

5. language constraints:
   - Hindi-only / Hinglish-only mode when configured
   - same-language mirroring for mixed-language mode

### C. Additional non-hard response controls

1. sanitization before TTS:
   - `_sanitize_agent_text(...)` redacts known sensitive values from assistant text before TTS/transcript emission

2. no echo of entered values:
   - prompt explicitly forbids repeating phone, OTP, PAN, Aadhaar, email, name, etc.

3. transcript suppression:
   - function/tool JSON-like assistant text is suppressed from room transcription

### D. What is actually missing

Not found:

- no hard response character cap
- no hard LLM token output cap
- no enforced max context-length truncation in the current prompt builder

### E. Current-behavior verdict for response constraints

Is there a precise hard maximum spoken length today?

**No.**

There are strong prompt guidelines for short, concise, human responses, but no code-enforced hard cap was found.

### F. Required corrections

#### Issue 4: configured context-length limit is not enforced

Current behavior:

- `ai.max_context_length_chars` exists in YAML
- `build_kyc_context(...)` returns the assembled string without applying that limit

Expected behavior after fix:

- enforce the configured max context length in the context builder before returning prompt/context text
- use deterministic truncation priority:
  - current screen
  - active field states
  - latest validation failure
  - recent errors
  - only then older/secondary context

#### Issue 5: no hard assistant output limit

Current behavior:

- brevity is prompt-only

Expected behavior after fix:

- set provider-specific max output tokens if supported
- optionally post-process overlong replies before TTS

---

## 11.3 SDK Data Transmission Architecture

### A. Complete SDK-to-backend call catalog found in code

#### 1. SDK config fetch

Path:
- `SdkConfigManager.fetchConfigAsync()`
- GET `/sdk/config`

Purpose:
- fetch targeted feature flags and thresholds

Mode:
- asynchronous from SDK caller perspective

Failure handling:
- logs warning
- retains local defaults

#### 2. Identity event

Path:
- `KycEvent.identity(...)`
- `AI.setUser(...)`
- `SdkRuntime.setUser(...)`
- `HttpBackendClient.setUser(...)`
- POST `/events` with `event = "identity"`

Mode:
- asynchronous fire-and-forget via SDK executor

Failure handling:
- no retry
- no caller callback

Critical issue:

- `HttpBackendClient.setUser(...)` hardcodes `session_id = "unknown"`
- therefore identity is not posted under the real SDK session id

#### 3. Screen-state transmission

Path:
- route change
- `EmbedProvider -> AI.setKycStep(...)`
- `SdkRuntime.setKycStep(...)`
- POST `/events` with `event = "screen_state"`

Mode:
- asynchronous on SDK side
- synchronous storage on backend side

Purpose:
- keep current screen synchronized for context and trigger evaluation

Failure handling:
- no retry

#### 4. Screen schema transmission

Path:
- `AI.registerScreenSchemas(...)`
- `SdkRuntime.registerSchema(...)`
- `HttpBackendClient.pushScreenSchema(...)`
- POST `/events` with `event = "screen_schema"`

Mode:
- asynchronous on SDK side
- synchronous storage on backend side

Purpose:
- push screen/component/validation schema for context enrichment

#### 5. Lazy schema beacon

Path:
- `SdkRuntime.setKycStep(...)`
- if screen has no schema and lazy beacon not yet sent
- `pushScreenSchemaLazy(...)`
- POST `/events` with `event = "screen_schema_lazy"`

Mode:
- asynchronous on SDK side
- synchronous storage on backend side

Purpose:
- at minimum mark screen presence for context/navigation

#### 6. Component input transmission

Path:
- `KycEvent.componentInput(...)`
- `AI.reportComponentInput(...)`
- `SdkRuntime.reportComponentInput(...)`
- `HttpBackendClient.reportComponentInput(...)`
- POST `/events` with `event = "component_input"`

Mode:
- asynchronous on SDK side
- synchronous storage on backend side

Purpose:
- keep latest field state available to backend context

Current timing relevance:

- this is one of the most important immediate-sync data types for LLM correctness

Failure handling:

- SDK logs success/failure only
- no retry
- offline queue class exists in SDK source but is not wired into the current path

#### 7. Validation-failure transmission

Path:
- `AI.trackValidationFailure(...)`
- `SdkRuntime.trackValidationFailure(...)`
- `HttpBackendClient.trackValidationFailureEvent(...)`
- POST `/events` with `event = "validation_failed"`

Mode:
- asynchronous on SDK side
- **asynchronous** storage on backend side in current route logic

Important implication:

- unlike `component_input` and `screen_state`, `validation_failed` is not currently one of the backend sync-stored event types
- therefore it is not guaranteed to update voice context immediately before response return

#### 8. Analytics transmission

Path:
- `AI.trackAnalytics(...)`
- `SdkRuntime.trackAnalytics(...)`
- `HttpBackendClient.trackAnalyticsEvent(...)`
- POST `/events` with `event = "analytics_data"`

Mode:
- asynchronous on SDK side
- asynchronous backend processing

Purpose:
- business metrics, journey events, completion events

#### 9. Generic error transmission

Path:
- `AI.trackError(...)`
- `SdkRuntime.trackError(...)`
- `HttpBackendClient.trackGenericErrorEvent(...)`
- POST `/events` with `event = "error_reported"`

Mode:
- asynchronous on SDK side
- asynchronous backend processing

#### 10. Trigger evaluation

Path:
- passive loop or post-error evaluation
- `HttpBackendClient.evaluateTrigger(...)`
- POST `/assistant/trigger/evaluate`

Mode:
- asynchronous callback pattern on SDK side
- synchronous request/response on backend side

Purpose:
- ask backend if voice assistant should be offered/started

#### 11. Voice session start

Path:
- `AI.startAssistant()`
- `SdkRuntime.startAssistantSession(...)`
- `HttpBackendClient.startAssistant(...)`
- POST `/assistant/session/start`

Mode:
- asynchronous callback pattern on SDK side
- synchronous request/response on backend side

Purpose:
- obtain LiveKit token / room / invocation id

Failure handling:

- surfaced through status listener
- 409 dedup/suppression is intentionally treated as silent no-op

#### 12. Voice session stop

Path:
- `AI.stopAssistant()`
- `SdkRuntime.stopAssistantSession()`
- `HttpBackendClient.stopAssistant(...)`
- POST `/assistant/session/stop`

Mode:
- asynchronous fire-and-forget

Purpose:
- terminate backend voice session

#### 13. Dynamic popup evaluation

Path:
- `AI.checkForDynamicPopup()`
- `SdkRuntime.checkForDynamicPopup()`
- `PopupManager.fetchAndDispatchPopup(...)`
- POST `/assistant/popup/evaluate`

Mode:
- suspend call from SDK perspective
- synchronous request/response

Current throttle:

- `PopupManager` enforces minimum `30 s` between fetches

#### 14. Flow context transmission

Path:
- `AI.setFlow(...)`
- `SdkRuntime.setFlow(...)`
- `HttpBackendClient.setFlow(...)`
- POST `/flows/set`

Mode:
- asynchronous fire-and-forget

Critical issue:

- SDK sends JSON field `flow_key`
- backend `FlowSetIn` requires `app_domain`
- current contract does not match
- so flow context synchronization is currently broken

#### 15. Drop-off registration

Path:
- app background with known phone
- `SdkRuntime.onAppBackgroundObserved(...)`
- `HttpBackendClient.registerDropoffLead(...)`
- POST `/api/dropoff/register`

Mode:
- asynchronous fire-and-forget

Purpose:
- telephony callback re-engagement

#### 16. Drop-off cancel

Path:
- `AI.completeKycFlow(...)`
- `SdkRuntime.completeKycFlow(...)`
- `HttpBackendClient.cancelDropoffLead(...)`
- POST `/api/dropoff/cancel`

Mode:
- asynchronous fire-and-forget

Purpose:
- prevent callback after flow completion

### B. Which data currently gets immediate backend synchronization for LLM usefulness

#### Immediate / synchronous on backend

These are the most important currently implemented immediate context updates:

- `component_input`
- `screen_state`
- `screen_schema_lazy`
- `screen_schema`
- final `voice_conversation_turn`
- `telephony_call_state`

For LLM correctness, the key working immediate-sync items are:

- current screen
- current component input hint snapshot

#### Not immediate in the current backend route

- `validation_failed`
- `analytics_data`
- `error_reported`

This matters because:

- validation failures are highly relevant to LLM guidance
- but current backend code does not place `validation_failed` in the sync path

### C. Failure handling architecture actually present

What exists:

- status callbacks for voice-start failure
- silent suppression for duplicate/recent voice sessions
- config fetch fallback to defaults
- popup fetch failure returns null
- component_input/network failures are logged

What does not exist in the current integration path:

- no retry queue in active use
- no durable resend path for failed event posts
- `OfflineEventQueue` exists in SDK source but is not wired into the demo/runtime flow examined here

### D. Current-behavior verdict for SDK data architecture

Is the transmission architecture right for maintaining accurate LLM context?

**Partially.**

What is right:

- `screen_state` and `component_input` are the two most important real-time context signals
- backend stores both synchronously
- active voice sessions receive immediate `context_updated` push on `component_input`

What is not right:

1. flow context is broken by contract mismatch (`flow_key` vs `app_domain`)
2. identity event is posted with `session_id = "unknown"`
3. validation-failure events are not on the synchronous context-critical backend path
4. there is no active retry/offline resend path in the analyzed integration

### E. Required corrections

#### Issue 6: flow context contract mismatch

Current behavior:

- SDK sends:
  - `POST /flows/set`
  - body contains `flow_key`
- backend requires:
  - `app_domain`

Expected behavior after fix:

- either change SDK to send `app_domain`
- or add backend compatibility for `flow_key`
- until then, do not rely on `AI.setFlow(...)` as effective backend context sync

#### Issue 7: identity event uses wrong session id

Current behavior:

- `HttpBackendClient.setUser(...)` posts `session_id = "unknown"`

Expected behavior after fix:

- send the actual runtime session id in the identity event body

#### Issue 8: `validation_failed` is not synchronized like other context-critical events

Current behavior:

- backend route lists `validation_failed` as context-relevant
- but it is not in `_SYNC_EVENT_TYPES`
- therefore it is processed asynchronously and does not currently trigger the same immediate update guarantee as `component_input`

Expected behavior after fix:

- include `validation_failed` in the synchronous storage path
- and keep `context_updated` signaling for active voice sessions

#### Issue 9: offline queue exists but is not wired

Current behavior:

- no durable retry path is active for failed SDK event posts

Expected behavior after fix:

- wire `OfflineEventQueue` or equivalent retry mechanism into event posting for context-critical events

---

## 11.4 Final Answer to "Is It Right or Not?"

### Email-entry + voice correctness path

**Not fully right.**

Reason:

- local UI validation works
- backend synchronization of typed input works after debounce
- but exact validation of the typed screen value is not deterministic unless the user also speaks the value aloud

### Voice-response constraints

**Not fully right.**

Reason:

- concise and empathetic instructions do exist
- but there is no code-enforced hard cap for LLM or spoken output length
- configured context-length limit appears unused

### SDK data transmission architecture

**Partially right, but not correct end-to-end yet.**

Reason:

- the context-critical `screen_state` and `component_input` paths are well positioned for real-time voice assistance
- however flow sync is broken, identity session tagging is wrong, validation-failure sync is incomplete, and offline retry is not active

### Highest-priority implementation corrections

#### Priority 1 ✅ COMPLETED

1. **Fix flow contract — standardize on `flow_key` throughout (no `app_domain` alias)**
   - ✅ All backend models, routes, services, and SDK updated to use `flow_key` as the single canonical field
   - ✅ No backward-compatibility aliases remain in any production code
   - ✅ Test files updated to use `flow_key`
   - ✅ Documented in section 12 above

2. **`validation_failed` added to `_SYNC_EVENT_TYPES`**
   - ✅ Backend `events.py` now handles `validation_failed` synchronously in the same fast-path as `component_input` and `screen_state`
   - ✅ `validation_failed` also added to `_VOICE_AGENT_CONTEXT_PUSH_EVENTS` so it immediately refreshes voice context

3. ~~Fix identity session id~~ — **No change needed; call chain already passes the real `sessionId`**
   - `SdkRuntime.setUser(id, phone, phoneMasked)` calls `backendClient.setUser(id, userContext.sessionId, ...)` which passes the real runtime session id
   - The earlier analysis flag was incorrect; no code change was made

#### Priority 2 ✅ COMPLETED

4. **Email telemetry in `EmailEntryScreen`**
   - ✅ `KycEvent.validationFailed(code="email_invalid", componentId="email_field", componentType="text_input", businessStep="email_entry")` emitted when user taps "Get OTP" while the email field is invalid
   - ✅ `KycEvent.analytics(eventName="otp_requested", ...)` emitted on every valid OTP request (both from button and Skip)
   - ✅ `EmailEntryScreen.kt` diagnostics: clean

5. **`ai.max_context_length_chars` enforcement**
   - ✅ `build_kyc_context()` now reads `settings.ai.max_context_length_chars` and truncates the returned context string if it exceeds the limit, with a warning log
   - ✅ `session.flow_graph_position` → `session.flow_position` (field renamed in store)
   - ✅ `session.app_domain` → `session.flow_key` in `build_kyc_context` and `build_system_prompt`
   - ✅ `kyc_context.py` diagnostics: clean

#### Priority 3 ✅ COMPLETED

6. **`max_output_tokens` enforcement in voice agent**
   - ✅ New field `VoiceAgentSettings.max_output_tokens` added to `config.py` with env-var override (`VOICE_MAX_OUTPUT_TOKENS`) and YAML support; default `0` = no cap
   - ✅ `AgentSession` in `voice_agent.py` now receives `model_settings=ModelSettings(max_output_tokens=N)` when a non-zero cap is configured; otherwise passes `None` (uses provider default)
   - ✅ `voice_agent.py` diagnostics: clean
   - ✅ `config.py` diagnostics: clean

7. ~~Screen-only value correctness without raw field exposure~~ — deferred pending design decision

---

## 12. Completed Refactor: `flow_key` Only — No `app_domain` Alias

### What was changed

All production code that previously used `app_domain` as a field name has been updated to use `flow_key` as the single canonical identifier for business-flow context. No backward-compatibility aliases remain.

### Files changed

#### Backend model layer

- `backend/app/models.py`
  - `EventIn.flow_key`: renamed from `app_domain` (optional field)
  - `AssistantStartIn.flow_key`: renamed from `app_domain` (optional field)
  - `TriggerEvaluateIn.flow_key`: renamed from `app_domain` (optional field)
  - `FlowSetIn.flow_key`: now the single required field, no `app_domain` field exists
  - `FlowSetOut.flow_key`: renamed from `app_domain` in response model

#### Backend session store

- `backend/app/services/store.py`
  - `SessionState.flow_key`: renamed from `app_domain`
  - `InMemorySessionStore.upsert_event`: reads `flow_key` from payload only
  - `SQLiteSessionStore`: schema updated — `app_domain` column replaced with `flow_key`; migration adds new column
  - `SQLiteSessionStore._upsert_state`: writes `flow_key`
  - `SQLiteSessionStore._fetch_session` / `_row_to_state`: reads `flow_key`

#### Backend route layer

- `backend/app/routes/flow.py`
  - `FlowSetIn`: single required `flow_key` field, no optional `app_domain`
  - `FlowSetOut`: `flow_key` in response
  - `set_flow`: uses `payload.flow_key` directly, no resolution/aliasing
  - `record_flow_sequence`: called with `flow_key=flow_key`
  - `get_flow_position`: called with `flow_key=flow_key`
  - Activity logs: `flow_key` field name

- `backend/app/routes/assistant.py`
  - All reads/writes of `session.flow_key` (previously `session.app_domain`)
  - Context response payload: `flow_key` key
  - `normalize_step_label`: called with `session.flow_key` (was `session.app_domain`)

#### Backend service layer

- `backend/app/services/kyc_context.py`
  - `session.flow_key` (was `session.app_domain`) in `build_kyc_context`
  - `_domain_role_description(flow_key)` and `_domain_persona_block(flow_key)`: parameter renamed
  - Prompt output: `Flow key:` label (was `App domain:`)

- `backend/app/services/flow_graph.py`
  - `record_flow_sequence(flow_key, ...)`: parameter renamed
  - `get_flow_position(flow_key, ...)`: parameter renamed
  - MongoDB index: `flow_key` field (was `app_domain`)
  - In-memory store key: `flow_key` (was `app_domain`)
  - Activity log field: `flow_key`

- `backend/app/services/step_label_cache.py`
  - `normalize_step_label(raw_label, flow_key)`: parameter renamed
  - `get_cached_label(raw_label, flow_key)`: parameter renamed
  - MongoDB index: `flow_key` field (was `app_domain`)
  - In-memory cache key: `flow_key` (was `app_domain`)
  - Activity log field: `flow_key`

#### Backend voice agent

- `backend/voice_agent.py`
  - Context fetch result keys: `flow_key` (was `app_domain`)
  - Log lines: `flow_key` field name

#### Android SDK

- `android-sdk/sdk/src/main/java/com/kycis/sdk/core/HttpBackendClient.kt`
  - `setFlow`: POST body sends `flow_key` (was `app_domain`)

### Test files updated

- `backend/tests/test_flow_api_comprehensive.py`: all payload/assertion `app_domain` → `flow_key`
- `backend/tests/test_zynnex_framework_smoke.py`: all payload/assertion `app_domain` → `flow_key`
- `backend/tests/test_logic_flaw_fixes.py`: test class and method names, payload fields updated to `flow_key`
- `backend/tests/test_event_model_validation.py`: `flow_key=None` in test event construction

### What was intentionally NOT changed

- MongoDB collection field names in existing data: MongoDB stores are schema-flexible; existing documents continue to be readable. New writes use `flow_key`.
- YAML config `flow_configurations` keys: these are data values (e.g., `onboarding`, `kyc`) not field names, so no change is needed.
- `kycis.yaml` comments describing the config structure.

### Why this is correct

- `flow_key` is the value actually used to index flow configurations and graph sequences in backend code.
- Using one field name throughout eliminates the class of bugs where SDK sends `flow_key` but backend expects `app_domain`.
- The backend now has a single source of truth: `session.flow_key` stored in `SessionState` and used by all downstream services.
4. make screen-only value correctness deterministic without exposing raw field values to the LLM
5. enforce configured context/output length limits in executable code, not only in prompts/config
