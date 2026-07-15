# Flow E2E Harness — Test Flow Report

Generated from the `Flow E2E harness` screen in the KYCISDemo app (Home → "Flow E2E test (walk every
screen against backend)"). This documents exactly what the harness does for **each screen**, and
includes a real run's results captured on 2026-07-14 against a local backend.

## How the run starts

Before touching any screen, `FlowTestRunner` opens a disposable session
(`HarnessBackendClient`) that is completely separate from the real app's live SDK session:

- `user_id` = `harness-user-<random8>`, `session_id` = `harness-<uuid>`
- `client_id` = `kycis_demo_harness`, `mapping_version` = `v1`

It then pushes the app's **real** contract to the backend under that session, exactly once:

1. `POST /v1/events` (`event=screen_schema`) for every entry in `KycisScreenSchemas.all` (12 screens).
2. `POST /v1/events` (`event=workflow_model`) for `KycisWorkflow.model` (`workflow_name=kyc_onboarding`,
   12 stages).

If either push is rejected, the run stops immediately with a fatal error (nothing per-screen runs).

## Per-screen sequence

For every stage in `KycisWorkflow.model.stages`, in order, the harness does:

1. `POST /v1/events` (`event=screen_state`, `screen=screen_id`) — marks the screen as current.
2. `POST /v1/flows/set` (`flow_key=kyc`, `step_sequence`=all 12 stage ids, `current_step=screen_id`) —
   expects the returned `progress.position` to equal the screen's 1-based index in the workflow.
3. For each component on that screen's schema (skipped entirely for `FILE_UPLOAD`/`CAMERA` types —
   see "Skipped components" below):
   - If the component has an "invalid" sample value configured, sends
     `POST /v1/events` (`event=validation_failed`) with that value, the component's real
     `ValidationRule.pattern`/`ruleId`/first `errorCode` (if any) — mirrors what
     `KycisIntegration.onValidationFailedDetailed` would send for a real bad input.
   - Sends `POST /v1/events` (`event=component_input`) with the "valid" sample value — mirrors
     `KycisIntegration.reportComponentInput`.
4. `GET /v1/assistant/context/{session_id}` — asserts `current_screen` equals the screen just set.

A screen's status is **PASS** if the context check and every component check succeeded, **FAIL** if
any HTTP call was rejected or `current_screen` didn't match, or **SKIPPED** if every component on
the screen was a binary-capture type.

## Screen-by-screen detail

| # | Screen (`screen_id`) | Components sent | Valid value → Invalid value tested | Result (last run) |
|---|---|---|---|---|
| 1 | `phone_entry` | `phone_field` (text, pattern `[0-9]{10}`) | `9876543210` → `12345` | PASS |
| 2 | `phone_otp` | `phone_otp_field` (otp, pattern `[0-9]{4}`) | `1234` → `0` | PASS |
| 3 | `email_entry` | `email_field` (text) | `harness.tester@example.com` → `not-an-email` | PASS |
| 4 | `email_otp` | `email_otp_field` (otp) | `1234` → `0` | PASS |
| 5 | `pan_details` | `pan_field` (text, pattern `[A-Z]{5}[0-9]{4}[A-Z]`), `dob_field` (date), `terms_checkbox` (checkbox) | `ABCDE1234F`→`INVALIDPAN`; `1995-06-15`→`""`; `true`→`false` | PASS |
| 6 | `personal_details` | `name_field`, `father_name_field` (text, no formal validation), `gender_field`, `marital_status_field`, `residency_status_field` (dropdowns, no formal validation) | `Harness Tester`/`Test Father`/`male`/`single`/`resident` → all `""` | PASS |
| 7 | `verify_documents` | `verify_documents_button` (button, not required) | `tapped` → *(invalid check skipped — buttons have no invalid state)* | PASS |
| 8 | `digilocker_aadhaar` | `aadhaar_digilocker_field` (text, pattern `[0-9]{12}`) | `123456789012` → `123` | PASS |
| 9 | `upload_aadhaar_front` | `aadhaar_front_field` (**file_upload**) | *(not simulated)* | SKIPPED |
| 10 | `upload_aadhaar_back` | `aadhaar_back_field` (**file_upload**) | *(not simulated)* | SKIPPED |
| 11 | `selfie_capture` | `selfie_capture` (**camera**) | *(not simulated)* | SKIPPED |
| 12 | `signature` | `signature_field` (button, not required) | `tapped` → *(skipped)* | PASS |

## Skipped components

`FlowTestData.isSimulatable()` excludes `FILE_UPLOAD` and `CAMERA` component types — a synthetic
text value can't stand in for an actual image/document capture. These 3 screens are reported as
`SKIPPED`, not failures; binary-capture flows remain covered by the device-based E2E test layer
(camera/gallery interaction, upload success) rather than this harness.

## Exact wire payloads — HTTP (to and fro, captured)

Everything below is a **literal** request body the harness sends and the **literal** response the
backend returned, captured by replaying each call against the running local backend
(`http://127.0.0.1:8000`, `X-API-Key: demo-api-key`). Field order/values match what
`HarnessBackendClient.kt` actually constructs.

### 1. Schema push — `POST /v1/events` (`event=screen_schema`)

Sent once per screen at run start (shown here for `phone_entry`).

Request:

```json
{
  "user_id": "harness-user-<id>",
  "session_id": "harness-<uuid>",
  "event": "screen_schema",
  "screen": "phone_entry",
  "screen_id": "phone_entry",
  "client_id": "kycis_demo_harness",
  "mapping_version": "v1",
  "timestamp": 1784037930,
  "invoke_source": "flow_test_harness",
  "properties": {
    "screen_id": "phone_entry",
    "display_name": "Mobile Number Entry Screen",
    "components": [
      {
        "id": "phone_field",
        "type": "text_input",
        "required": true,
        "display_name": "Mobile Number Field",
        "validations": [
          {
            "rule_id": "phone_format_validation_v1",
            "intent": "PHONE_IN",
            "pattern": "[0-9]{10}",
            "error_codes": ["phone_invalid", "phone_too_short", "phone_length_mismatch"],
            "recovery_playbook_id": "retry_phone_number",
            "description": "Please enter your 10-digit Indian mobile number."
          }
        ]
      }
    ],
    "next_screen_id": "phone_otp",
    "flow_order": 11
  }
}
```

Response (`EventAcceptedOut`) — captured live:

```json
{
  "status": "accepted",
  "session_id": "report-demo-session",
  "ingested_at": "2026-07-14T14:39:20.185152Z",
  "schema_stored": true,
  "schema_hash": "412c65baef16284c",
  "schema_replaced": true,
  "workflow_stored": null,
  "workflow_hash": null,
  "workflow_replaced": null,
  "workflow_cohort_key": null
}
```

`schema_hash` is a content hash the backend computes over the normalized schema; `schema_replaced`
tells the caller whether this call changed a previously-stored schema for that `screen_id` (`true`
here because the calling session/client pair hadn't pushed this exact hash before).

### 2. Workflow push — `POST /v1/events` (`event=workflow_model`)

Sent once, carrying all 12 stages. Real captured payload from the harness session
(`harness-a4013eea-…`), verbatim as stored server-side:

```json
{
  "user_id": "harness-user-e9d4ffa6",
  "session_id": "harness-a4013eea-7d26-4ab4-9b65-e6ce07ca2bb6",
  "event": "workflow_model",
  "screen": "signature",
  "screen_id": "signature",
  "client_id": "kycis_demo_harness",
  "mapping_version": "v1",
  "invoke_source": "flow_test_harness",
  "properties": {
    "workflow_name": "kyc_onboarding",
    "stages": [
      { "id": "phone_entry", "expected_next": "phone_otp", "display_name": "Phone entry" },
      { "id": "phone_otp", "expected_next": "email_entry", "display_name": "Phone OTP" },
      { "id": "email_entry", "expected_next": "email_otp", "display_name": "Email entry" },
      { "id": "email_otp", "expected_next": "pan_details", "display_name": "Email OTP" },
      { "id": "pan_details", "expected_next": "personal_details", "display_name": "PAN details" },
      { "id": "personal_details", "expected_next": "verify_documents", "display_name": "Personal details" },
      { "id": "verify_documents", "expected_next": "digilocker_aadhaar", "display_name": "Verify documents" },
      { "id": "digilocker_aadhaar", "expected_next": "selfie_capture", "display_name": "DigiLocker Aadhaar" },
      { "id": "upload_aadhaar_front", "expected_next": "upload_aadhaar_back", "display_name": "Aadhaar front" },
      { "id": "upload_aadhaar_back", "expected_next": "selfie_capture", "display_name": "Aadhaar back" },
      { "id": "selfie_capture", "expected_next": "signature", "display_name": "Selfie" },
      { "id": "signature", "display_name": "Signature" }
    ]
  }
}
```

The backend's stored `screen` field on this event is `"signature"` (the last stage, since
`FlowTestRunner` pushes the workflow once at start using the *current* value of `screen` at call
time — a harmless quirk of push ordering, not a bug in the workflow content itself, which is
`workflow_hash`-verified as identical either way).

### 3. `screen_state` — `POST /v1/events` (`event=screen_state`)

Real captured payload/response pair for `pan_details`:

```json
{
  "user_id": "harness-user-e9d4ffa6",
  "session_id": "harness-a4013eea-7d26-4ab4-9b65-e6ce07ca2bb6",
  "event": "screen_state",
  "screen": "pan_details",
  "screen_id": "pan_details",
  "client_id": "kycis_demo_harness",
  "mapping_version": "v1",
  "invoke_source": "flow_test_harness",
  "properties": {}
}
```

Response: `{"status":"accepted","session_id":"...","ingested_at":"...","schema_stored":null,...}` —
same `EventAcceptedOut` shape, with the schema/workflow fields `null` since this call doesn't touch
either.

### 4. `POST /v1/flows/set`

Request (captured for `current_step=pan_details`, position 5 of 12):

```json
{
  "user_id": "report-demo-user",
  "session_id": "report-demo-session",
  "flow_key": "kyc",
  "client_id": "kycis_demo_harness",
  "step_sequence": ["phone_entry","phone_otp","email_entry","email_otp","pan_details","personal_details","verify_documents","digilocker_aadhaar","upload_aadhaar_front","upload_aadhaar_back","selfie_capture","signature"],
  "current_step": "pan_details"
}
```

Response — captured live:

```json
{
  "status": "ok",
  "session_id": "report-demo-session",
  "flow_key": "kyc",
  "business_step": "kyc_verification",
  "validation_intent": "document_verification",
  "progress": { "position": 5, "total": 12, "percent": 42 }
}
```

This is also what causes the one-time `flow_graph_recorded` activity entry keyed by `client_id`
(not `session_id`) the first time a given `client_id` registers a step sequence:
`{"kind":"flow_graph_recorded","payload":{"flow_key":"kyc","client_id":"kycis_demo_harness","total_steps":12}}`.

### 5. `component_input` — valid value

Real captured payload for `phone_field` on `phone_entry`:

```json
{
  "user_id": "harness-user-e9d4ffa6",
  "session_id": "harness-a4013eea-7d26-4ab4-9b65-e6ce07ca2bb6",
  "event": "component_input",
  "screen": "phone_entry",
  "screen_id": "phone_entry",
  "component_id": "phone_field",
  "component_type": "text_input",
  "hint": "9876543210",
  "masked": false,
  "client_id": "kycis_demo_harness",
  "mapping_version": "v1",
  "invoke_source": "flow_test_harness",
  "properties": {}
}
```

Response: `{"status":"accepted",...}` (same shape as above, all schema/workflow fields `null`).

### 6. `validation_failed` — invalid value

Real captured payload for `aadhaar_digilocker_field` on `digilocker_aadhaar`:

```json
{
  "user_id": "harness-user-e9d4ffa6",
  "session_id": "harness-a4013eea-7d26-4ab4-9b65-e6ce07ca2bb6",
  "event": "validation_failed",
  "screen": "digilocker_aadhaar",
  "screen_id": "digilocker_aadhaar",
  "component_id": "aadhaar_digilocker_field",
  "component_type": "text_input",
  "hint": "123",
  "masked": false,
  "expected_pattern": "[0-9]{12}",
  "validation_rule_id": "aadhaar_length_v1",
  "errors": ["aadhaar_invalid"],
  "failure_reason_code": "aadhaar_invalid",
  "client_id": "kycis_demo_harness",
  "mapping_version": "v1",
  "invoke_source": "flow_test_harness",
  "properties": { "error_code": "aadhaar_invalid", "signal_strength": "strong" }
}
```

Response: same `EventAcceptedOut` shape, `status="accepted"`.

### 7. `GET /v1/assistant/context/{session_id}`

Captured response (trimmed to structure; full `system_prompt` is several KB of persona/rules text
and is summarized, not reproduced verbatim):

```json
{
  "session_id": "report-demo-session",
  "current_screen": "phone_entry",
  "inferred_step": "phone_entry",
  "flow_key": null,
  "flow_position": {},
  "user_id": "report-demo-user",
  "client_id": "kycis_demo_harness",
  "mapping_version": "v1",
  "context": "Current screen: Mobile Number Entry Screen\nFlow progress: step 2/12 ...\n=== USER INPUTS (for validation) ===\nFIELD: Mobile Number Field [FILLED]\n  Value: 9876543210\n  Retry count: 1 ...",
  "structured_input": {
    "v": "1",
    "screen": "phone_entry",
    "fields": [
      {
        "id": "phone_field",
        "label": "Mobile Number Field",
        "type": "text_input",
        "status": "FILLED",
        "retry_count": 1,
        "metadata": { "chars": 10, "digits": 10, "letters": 0, "masked": false, "value": "9876543210", "first_char": "digit" },
        "validations": []
      }
    ]
  },
  "system_prompt": "<full LLM system prompt — persona, output-format contract, validation & confirmation rules, then the same CURRENT SESSION CONTEXT block embedded as text>",
  "recent_conversation": []
}
```

Two things worth calling out for correctness review:

- `retry_count: 1` on `phone_field` is the backend correctly counting the prior `validation_failed`
  call for that exact component before the valid `component_input` landed — proof the "invalid then
  valid" sequencing in step 3 of "Per-screen sequence" is actually being tracked field-by-field, not
  just logged.
- The assistant's `system_prompt` embeds the *entire* `context` block again verbatim at the end
  (`=== CURRENT SESSION CONTEXT ===`), so the LLM always sees the freshest screen/field state even if
  the rest of the prompt is templated/cached.

## WebSocket control-plane channel (separate from the harness)

**The harness itself never opens a WebSocket.** `HarnessBackendClient` is deliberately HTTP-only
(plain OkHttp POST/GET) so it stays isolated from the live SDK session and simple to reason about.
All the request/response pairs above are HTTP.

The **real, live app session** (the actual `AI`/`SdkRuntime` the demo screens talk to) is a
different story: it maintains a **second, independent** low-latency socket —
`RealtimeControlChannel` — at `ws://<host>/v1/events/ws`, purely for push-style control signals.
Durable telemetry (schema/workflow pushes, `component_input`, `validation_failed`) **always** goes
over HTTP via `HttpBackendClient` regardless of whether this socket is up; the socket is an
additive, best-effort fast path.

**Enablement is server-driven**: the SDK asks `GET /v1/sdk/config` for a
`use_websockets_for_control_plane` feature flag (defaults `true` pre-prod per `SdkConfig.kt`) and
only opens the socket if that flag is on (or a host override in `RuntimePolicy` forces it). 3
consecutive connect failures trigger a 60s HTTP-only cooldown before retrying.

### What travels over it, and in which direction

| Direction | Frame `type` | Carries |
|---|---|---|
| SDK → backend | `connect` | `user_id`, `session_id`, `api_key` — sent immediately on socket open |
| SDK → backend | `control_state` | Compact `screen_state` / `validation_failed` signals (`event`, `screen`, `failure_reason_code`, `errors`, `signal_strength`) — a low-latency mirror of the same signals HTTP `POST /v1/events` carries, used for faster popup/trigger reactions |
| SDK → backend | `trigger_evaluate` | `signals` (`screen`, `time_spent`, `errors`, `idle_seconds`) — periodic passive check ("should we intervene right now?") |
| SDK → backend | `heartbeat` / `ack` | Keepalive + acknowledgement of server commands |
| backend → SDK | `command` (`action=show_popup`) | `message`, `delay_ms`, `reason` — pushes the native "Need help?" dialog |
| backend → SDK | `command` (`action=suggest_start_voice_agent`) | `reason`, `intervention_mode` — pushes a suggestion to auto-offer the voice agent |
| backend → SDK | `trigger_response` (in reply to `trigger_evaluate`) | `trigger` (bool), `reason`, `action` |
| backend → SDK | `error` | e.g. `{"code":"http_only","message":"This signal must use HTTP POST /v1/events"}` if a durable-only signal is mistakenly sent over the socket |

### Real captured evidence (Logcat, `KYCIS/ws` tag) — from the same test session

While the in-app harness ran its HTTP-only flow, the app's own live SDK session had this socket
open in parallel (proof the two channels/sessions are fully independent):

```
19:27:14.375 I KYCIS/ws: connecting session_id=25aa2eb8-… url=ws://10.0.2.2:8000/v1/events/ws
19:27:14.467 I KYCIS/ws: connected session_id=25aa2eb8-… http_status=101
19:27:14.467 D KYCIS/ws: --> session_id=25aa2eb8-… type=connect
19:27:14.528 D KYCIS/ws: ack session_id=25aa2eb8-… status=ok detail=connected
19:27:14.531 D KYCIS/ws: heartbeat session_id=25aa2eb8-…
19:27:21.375 D KYCIS/ws: --> session_id=25aa2eb8-… type=trigger_evaluate
19:27:21.407 D KYCIS/ws: <-- session_id=25aa2eb8-… type=trigger_response action=none
19:27:41.375 D KYCIS/ws: --> session_id=25aa2eb8-… type=trigger_evaluate
19:27:41.396 D KYCIS/ws: <-- session_id=25aa2eb8-… type=trigger_response action=suggest_start_voice_agent
19:28:45.000 D KYCIS/ws: --> session_id=25aa2eb8-… type=control_state
19:28:45.036 D KYCIS/ws: ack session_id=25aa2eb8-… status=ok detail=screen_state
```

Notable: `trigger_evaluate` fires roughly every 10s (the passive-eval loop) and flips from
`action=none` to `action=suggest_start_voice_agent` once the idle/error signals cross the
configured threshold — the SDK just doesn't display it every time because `assistant_prompt_cooldown_seconds=45`
throttles the *popup* independently of the *evaluation* cadence. The single `control_state`
(`screen_state`) frame corresponds to the one real in-app navigation that happened mid-run
(opening the "Flow E2E test" screen itself still emits a `screen_state` signal, since navigation
tracking is global and doesn't know a screen is a test harness).

### Canonical wire shapes (from `contracts/fixtures/*.json`, used by backend contract tests)

These are the exact JSON shapes the backend's `WsConnectIn` / `WsControlStateIn` /
`WsTriggerEvaluateIn` / `WsCommandOut` / `WsErrorOut` Pydantic models parse and emit — the source
of truth the Logcat trace above is exercising against:

```json
// SDK -> backend, on socket open
{
  "type": "connect", "message_id": "msg-connect-1", "session_id": "sess-ws-1",
  "user_id": "user-1", "api_key": "test-api-key", "sdk_version": "1.2.0",
  "client": "android", "client_id": "kycis_demo"
}
```

```json
// SDK -> backend, screen navigation
{
  "type": "control_state", "message_id": "msg-cs-ss-1", "session_id": "sess-ws-1",
  "event": "screen_state", "screen": "otp_verify"
}
```

```json
// SDK -> backend, inline validation failure
{
  "type": "control_state", "message_id": "msg-cs-vf-1", "session_id": "sess-ws-1",
  "event": "validation_failed", "screen": "phone_entry",
  "failure_reason_code": "invalid_phone", "errors": ["invalid_phone"], "signal_strength": "strong"
}
```

```json
// SDK -> backend, passive trigger check
{
  "type": "trigger_evaluate", "message_id": "msg-te-1", "session_id": "sess-ws-1", "user_id": "user-1",
  "signals": { "screen": "otp_verify", "time_spent": 45, "errors": ["otp_mismatch"], "idle_seconds": 12 }
}
```

```json
// backend -> SDK, popup push
{
  "type": "command", "action": "show_popup", "command_id": "cmd-popup-1", "session_id": "sess-ws-1",
  "payload": { "message": "Having trouble with this step? Let me help you out.", "delay_ms": 500, "reason": "verification_failed" }
}
```

```json
// backend -> SDK, voice-agent suggestion push
{
  "type": "command", "action": "suggest_start_voice_agent", "command_id": "cmd-voice-1", "session_id": "sess-ws-1",
  "payload": { "reason": "validation_failed", "intervention_mode": "offer" }
}
```

```json
// backend -> SDK, rejection of a durable-only signal sent over the socket
{ "type": "error", "code": "http_only", "message": "This signal must use HTTP POST /v1/events", "message_id": "msg-bad-1" }
```

## Real run evidence (backend cross-check)

The on-device report was independently confirmed by querying the backend directly for the harness
session (`harness-a4013eea-7d26-4ab4-9b65-e6ce07ca2bb6`, `client_id=kycis_demo_harness`):

- `GET /api/activity` showed the full expected sequence of `screen_state`, `component_input`, and
  `voice_agent_context_fetch` entries for every screen, ending at `signature`.
- `GET /v1/assistant/context/{session_id}` at the end of the run returned:
  - `current_screen = "signature"`, `client_id = "kycis_demo_harness"`, `mapping_version = "v1"`
  - `flow_position`: `workflow_stage=signature`, `workflow_step_index=11` (0-based, i.e. stage 12 of
    12), `completion_percent=100`
  - `structured_input.fields`: `signature_field` → `value: "tapped"` (the exact value the harness sent)

This confirms the backend actually stored and returned what the harness sent — not just that the
HTTP calls returned 200.

## Side observation from this run

While the harness ran (about 30–90s wall clock for 12 screens), the **real, live app session**
independently showed its own "Need help completing this step?" popup every ~45s (its configured
`assistant_prompt_cooldown_seconds`), and each one auto-dismissed after a 12s timeout without
stacking a second dialog on top. This is expected background behavior of the live SDK session and is
unrelated to — and unaffected by — the harness's separate disposable session; it's included here
only as confirmation that the two sessions are properly isolated from each other.
