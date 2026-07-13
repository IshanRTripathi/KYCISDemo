# KYCISDemo × KYCIS SDK/Backend Integration — Gap Analysis (2026-07-13)

**Method:** fresh read of live code (not the 2026-05-04 `archive/SDK_FEATURE_INTEGRATION_ANALYSIS.md`,
which has stale claims — see the notice at its top). Two parallel read-only audits covered (A)
every screen file in `KYCISDemo/app/src/main/java/com/kycis/demo/presentation/screens/` against
what it actually sends the SDK, and (B) the current SDK public surface + backend contract +
logging/observability state. Findings below are cross-checked against both.

## 1. What is already correct (re-verified, do not "fix" again)

These were flagged as broken in the 2026-05-04 doc's addendum. All three are **already correct**
in the current code:

| Claim (2026-05-04 doc) | Current reality (2026-07-13) |
|---|---|
| "Flow contract mismatch: SDK sends `flow_key`, backend requires `app_domain`" | Backend `FlowSetIn.flow_key: str` (required) — `backend/app/routes/flow.py:24-34`. SDK sends `flow_key` — `HttpBackendClient.setFlow`, `HttpBackendClient.kt:823-834`. **Aligned.** |
| "`validation_failed` is not in the backend sync path" | It is: `_SYNC_EVENT_TYPES` and `_VOICE_AGENT_CONTEXT_PUSH_EVENTS` both include `validation_failed` — `backend/app/routes/events.py:118-137`. |
| "`HttpBackendClient.setUser` posts `session_id = \"unknown\"`" | It sends the real session id (`SdkRuntime.setUser` → `userContext.sessionId`) — `HttpBackendClient.kt:37-61`, `SdkRuntime.kt:346-352`. Never hardcoded. |

## 2. Connectivity — real, partially-fixed infra bug found this session

`KYCISDemo/app/src/main/AndroidManifest.xml` and `BackendUrlStore.kt` had **uncommitted working-tree
changes already in progress before this session**:

- `AndroidManifest.xml` was missing `<uses-permission android:name="android.permission.INTERNET" />`
  entirely — **the demo app could not make any network call at all** on a real device/emulator
  until this was added. It is now present (uncommitted).
- `BackendUrlStore`'s default backend URL was switched from an ngrok tunnel
  (`https://subpilose-abigail-unsuspectingly.ngrok-free.dev/v1`) to a production ALB
  (`http://prod-alb-1551985914.ap-south-1.elb.amazonaws.com`).
- The manifest already had `android:usesCleartextTraffic="true"`, so plain `http://` to that ALB
  is not blocked by Android's network security defaults — no further manifest change needed for
  that specific host.

**Action needed before trusting any further E2E test against this app:** confirm (a) these two
files are committed (not left as silent uncommitted state), and (b) the ALB host is actually the
backend you want the demo pointed at for testing — a production ALB is an unusual default for a
demo app's E2E testing target; consider whether local/dev backend should be the default instead
and the ALB reserved for a "prod" preset in `BackendSettingsScreen`.

## 3. WS realtime control plane — Android wiring confirmed working, now logged

The Phase 1/2 WS work from earlier this session was re-confirmed present and correctly scoped:
`RealtimeControlChannel` is receive-only (never carries durable telemetry), gated by
`use_websockets_for_control_plane` from `GET /v1/sdk/config`, polled every 30s from
`SdkRuntime.startRealtimeControlPlaneLoop`. It previously had **zero logging** (every connect
attempt, failure, and frame was silent) — fixed in this same session, see
[`docs/architecture/12-logging-and-debugging.md`](../KYCIS/docs/architecture/12-logging-and-debugging.md)
in the KYCIS repo.

## 4. Screen-by-screen SDK usage (current, condensed)

Full per-screen detail (component IDs, line citations) is in the audit transcript; condensed here.
"Auto" = via `EmbedProvider` route-change tracking, not a direct `AI.setKycStep` call.

| Screen | setKycStep | componentInput | validationFailed | analytics | trackError |
|---|---|---|---|---|---|
| `HomeScreen` | excluded (no tracking at all — `home` is in `excludeRoutes`) | none | none | none | none |
| `BackendSettingsScreen` | auto (no schema registered) | none | none | none | none |
| `PhoneEntryScreen` | auto | `phone_field` | `phone_invalid_format` | none | none |
| `PhoneOtpScreen` | auto | `phone_otp_field` (only at 4 digits) | `otp_incomplete` | none | none |
| `EmailEntryScreen` | auto | `email_field` | `email_invalid` | `otp_requested` | none |
| `EmailOtpScreen` | auto | `email_otp_field` (only at 4 digits) | `otp_incomplete` | none | none |
| `PanDetailsScreen` | auto | `pan_field`, `dob_field` (not `terms_checkbox`) | pan/dob/terms codes | none | none |
| `PersonalDetailsScreen` | auto | all 5 fields | per-field `required_field` | none | none |
| `VerifyDocumentsScreen` | auto | button hint only | none | none | none |
| `DigilockerAadhaarScreen` | auto | only on submit (no live typing) | `aadhaar_incomplete` | none | none |
| `UploadAadhaarScreen` (front/back) | auto | on mock-upload click | none | none | none |
| `SelfieCaptureScreen` | auto | after simulated capture | none | none | none |
| `SignatureScreen` | auto | on submit | none | none | none |
| `SdkDiagnosticsScreen` | manual | none | manual harness | — | manual harness |

**Cross-cutting gaps confirmed current:**

- `AI.completeKycFlow(...)` is **never called** — journey never formally closes; `SignatureScreen`
  submit navigates home directly (`NavGraph.kt:186-189`).
- `AI.clearFlow()` is **never called**.
- `AI.setFlow("onboarding")` is set once and never changed — the Home screen's `KYC Flow` vs
  `MFD Support` selector has no SDK effect (`HomeScreen.kt:33, 99-103`).
- Phone/email OTP **resend is a no-op lambda** (`NavGraph.kt:103, 126`) — not an SDK gap, but will
  produce misleading "resend" UX with no backend signal either way.
- Popup UX is **Toast-only** — `MainActivity.kt:190-198`. No accept/dismiss, no real UI surface.
- `AI.setAgentEventListener` is **not used in production** — only in `examples/Phase2Examples.kt`.
- Legacy screen schemas (`pan_entry`, `pan_upload`, `aadhaar_entry`, `otp_verify`) are registered
  with **no matching route** — dead schema data sent to the backend for screens that don't exist
  in this app's current nav graph.
- `backend_settings` route has **no registered schema** (gets `setKycStep` via auto-tracking, but
  no schema context).
- `VoiceUiSnapshotHolder` is populated **only from `PersonalDetailsScreen`** — voice context is
  rich on exactly one screen and empty everywhere else.
- Dead/unused SDK-adjacent files: `KycTextField.kt`, `KycButton.kt`, `LoadingIndicator.kt`,
  `ErrorView.kt`, `HintMasking.kt` wrappers exist but have zero call sites.

## 5. Remediation priority order

This supersedes section 8 of the old doc (same spirit, re-validated against current code).

**P0 — before any further E2E testing:**
1. Commit or resolve the INTERNET-permission + backend-URL working-tree changes (§2) —
   confirm intended target backend for testing.
2. Decide whether the production ALB should really be the demo's default, or whether a
   local/dev URL should be the default with the ALB as an explicit alternate preset.

**P1 — telemetry foundation (highest signal-to-effort):**
3. `AI.trackValidationFailure` is present on most screens already (better than the 2026-05-04
   doc suggested) — the two gaps left are `terms_checkbox` (PanDetailsScreen) and no validation
   telemetry at all on `VerifyDocumentsScreen` / `UploadAadhaarScreen` / `SelfieCaptureScreen` /
   `SignatureScreen` (these rely entirely on disabled-button local validation).
4. Add `AI.trackAnalytics` for step-completion and branch decisions — currently only
   `EmailEntryScreen` emits any analytics event (`otp_requested`). None of the other 12 screens
   emit step-started/step-completed/upload-outcome events.
5. Add `AI.trackError` for async/network failure paths — currently only used in
   `SdkDiagnosticsScreen`'s manual harness, never in a real failure path.

**P2 — orchestration correctness:**
6. Call `AI.completeKycFlow(...)` at `SignatureScreen` submit.
7. Wire Home's flow selector to an actual `AI.setFlow(...)` call (or remove the selector if
   `MFD Support` isn't real yet, to avoid misleading telemetry).
8. Clean up `KycDemoApplication`'s registered schemas: drop the four legacy no-route schemas,
   add one for `backend_settings` (or intentionally document why it's excluded), align
   `pan_details` schema with the `terms_checkbox` component it actually validates.
9. Extend `VoiceUiSnapshotHolder` coverage beyond `PersonalDetailsScreen`.

**P3 — intervention UX:**
10. Replace Toast-only popup handling with a real dialog/bottom sheet + accept/dismiss analytics.
11. Register `AI.setAgentEventListener` in `MainActivity` for connect/disconnect/popup analytics.
12. Wire the phone/email OTP resend no-ops to a real resend call + analytics.

**P4 — cleanup:**
13. Either adopt the unused `KycTextField`/`KycButton`/etc. wrappers or delete them.

## 6. Logging (separate workstream, already done)

See [`docs/architecture/12-logging-and-debugging.md`](../KYCIS/docs/architecture/12-logging-and-debugging.md)
in the KYCIS repo for the detailed backend/WS/voice-agent/SDK logging system delivered alongside
this audit. It does not change any of the gaps above — it makes them much faster to *diagnose*
once you start closing them (every SDK network/WS call and every backend request now logs
timing, session correlation, and full stack traces on failure).
