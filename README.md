# KYCIS Demo (Android)

Sample host app for the **KYCIS** Android SDK. Business logic lives in the **KYCIS** monorepo; this project is the integration surface for demos and manual testing.

## Relation to KYCIS

| Topic | Location |
|--------|----------|
| Canonical SDK source | [KYCIS/android-sdk/sdk/](../KYCIS/android-sdk/sdk/) |
| Backend API + contracts | `../KYCIS/backend/`, [API_CONTRACTS.md](../KYCIS/API_CONTRACTS.md) |
| Web SDK simulator (same events as Android) | [KYCIS/frontend/src/SdkSimulator.tsx](../KYCIS/frontend/src/SdkSimulator.tsx) |

## SDK dependency (local dev — **no version bump**)

This app does **not** consume a published AAR for day-to-day work. [`settings.gradle.kts`](settings.gradle.kts) uses Gradle’s **composite build**:

- `includeBuild("../KYCIS/android-sdk")` with `dependencySubstitution` so `com.kycis:kycis-sdk` resolves to the **`sdk`** project in that tree.
- [`app/build.gradle.kts`](app/build.gradle.kts) declares `implementation("com.kycis:kycis-sdk:1.0.0")` — the **`1.0.0` is only a coordinate placeholder**; Gradle substitutes the included project, so **edits under `../KYCIS/android-sdk/` are picked up on the next build** without changing that version string or the library’s `version` in `android-sdk/sdk/build.gradle.kts`.

Rebuild or run the app after SDK changes; use **Build → Clean Project** only if you see stale classes.

For **release** or **CI without a sibling checkout**, you would use a real versioned artifact (Maven) or a different wiring — not required for this workspace layout.

## Event API alignment

Use the same semantics as the SDK:

- **`AI.trackValidationFailure(...)`** → backend event **`validation_failed`** (field-level; updates `last_validation_failure`).
- **`AI.trackError(...)`** → backend event **`error_reported`** (generic; e.g. `personal_details_submit_failed`, debug).
- **`AI.reportComponentInput(...)`** → **`component_input`** (redacted hints).

Optional **`RuntimePolicy.clientId`** / **`mappingVersion`** match the backend mapping catalog (see KYCIS `API_CONTRACTS.md`).

## Local run

1. Start the KYCIS backend (e.g. `uvicorn` on port **8000**).
2. In **KycDemoApplication**, set **`backendBaseUrl`** to:
   - Emulator: `http://10.0.2.2:8000/v1`
   - Physical device: `http://<your-LAN-ip>:8000/v1`
3. Open the demo app, complete flows; verify backend logs and `/v1/assistant/context/{session_id}`.

## See also

- [KYCIS/CLIENT_APP_INTEGRATION.md](../KYCIS/CLIENT_APP_INTEGRATION.md)
- [KYCIS/SDK_BACKEND_ANDROID_SYNC_AUDIT.md](../KYCIS/SDK_BACKEND_ANDROID_SYNC_AUDIT.md)
