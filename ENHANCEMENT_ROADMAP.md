# KYCIS SDK Enhancement Roadmap
## Comprehensive Implementation Plan Based on Competitive Analysis

**Version:** 2.0  
**Date:** April 1, 2026  
**Status:** Ready for Implementation  
**Source:** Reverse-engineered Revrag SDK analysis + KYCIS architecture review

---

## Executive Summary

This roadmap consolidates findings from reverse-engineering the Revrag Embed SDK Suite (React Native, Flutter, React) and provides a prioritized implementation plan to enhance the KYCIS Android SDK. The goal is to achieve feature parity while maintaining KYCIS's architectural advantages (multi-layer configuration, schema registry, passive evaluation).

**Key Insight:** KYCIS has superior configuration management and validation tracking, but lacks UI components, automatic navigation tracking, and real-time agent event callbacks that make Revrag SDKs easier to integrate.

---

## Strategic Priorities

### What KYCIS Does Better
- Multi-layer remote configuration with feature flags
- Declarative schema registry for voice context
- Passive evaluation and auto-trigger logic
- Structured validation tracking
- W3C trace propagation

### What Revrag Does Better
- Automatic navigation tracking (EmbedProvider)
- Pre-built UI components (EmbedButton)
- Real-time agent event callbacks
- Flow-based activation (multiple API keys)
- Dynamic backend-driven popups

### Critical Gaps in Both
- Certificate pinning (security vulnerability)
- Offline event queue (data reliability)
- Local encryption for sensitive data

---

## Phase 1: Foundation Enhancements (Weeks 1-2)

### 1.1 SDK Metadata in Event Payloads

**What:** Add nested `sdk` object to all event payloads with `sdk_name`, `sdk_version`, and `platform` fields.

**Why:** 
- Enables backend to differentiate between SDK versions and platforms
- Aligns with industry standard event payload structure
- Facilitates analytics and debugging across SDK versions

**How:**
```kotlin
// Create SdkMetadata data class
data class SdkMetadata(
    val sdk_name: String = "kycis-android",
    val sdk_version: String,
    val platform: String = "android"
)

// Modify event builder in HttpBackendClient.kt
data class SdkEvent(
    val user_id: String,
    val event: String,
    val sdk: SdkMetadata,
    val timestamp: Long,
    // ... existing fields
)
```

**Impact:**
- Backend schema update required (coordinate with backend team)
- Backward compatible (additive field)
- Effort: 2-3 days SDK + 1-2 days backend

**Success Metric:** 100% of events include `sdk` object within 1 release cycle

---

### 1.2 Event Ordering Validation

**What:** Enforce that `identity` event must be sent before other events, with automatic `user_id` enrichment.

**Why:**
- Prevents data integrity issues from missing user context
- Reduces host app boilerplate (auto-enrichment)
- Provides clear error messages for misconfiguration

**How:**
```kotlin
// New EventValidator.kt class
class EventValidator {
    private var identityEstablished = false
    private var cachedUserId: String? = null
    
    fun validateAndEnrich(event: String, payload: MutableMap<String, Any>): Map<String, Any> {
        when (event) {
            "identity" -> {
                val userId = payload["user_id"] as? String 
                    ?: throw IllegalStateException("user_id required for identity event")
                identityEstablished = true
                cachedUserId = userId
            }
            else -> {
                require(identityEstablished) { 
                    "identity event must be sent before $event" 
                }
                if (!payload.containsKey("user_id")) {
                    payload["user_id"] = cachedUserId!!
                }
            }
        }
        return payload
    }
}
```

**Migration Strategy:**
1. Phase 1: Log warnings only (no exceptions)
2. Phase 2: Throw in debug builds only
3. Phase 3: Enforce in all builds after 1 release cycle

**Impact:**
- Low risk (won't break unless apps have ordering issues)
- Effort: 1-2 days
- No dependencies

**Success Metric:** Zero events without user_id after enforcement

---

### 1.3 Analytics Event Type

**What:** Add dedicated `analytics_data` event type with required `event_name` field.

**Why:**
- Clearer event taxonomy for analytics pipelines
- Separates business metrics from SDK operational events
- Enables better backend processing and reporting

**How:**
```kotlin
// Add to AI.kt
object AI {
    fun trackAnalytics(eventName: String, data: Map<String, Any> = emptyMap()) {
        trackEvent("analytics_data", mapOf(
            "event_name" to eventName,
            "data" to data
        ))
    }
}

// Usage
AI.trackAnalytics(
    eventName = "kyc_completed",
    data = mapOf("duration_seconds" to 120, "steps_completed" to 5)
)
```

**Impact:**
- Backend may need new analytics pipeline
- Effort: 1-2 days
- No breaking changes

**Success Metric:** 50% of custom tracking migrates to analytics_data within 2 releases

---

## Phase 2: Agent Event System (Weeks 3-4)

### 2.1 Real-Time Agent Event Callbacks

**What:** Implement callbacks for voice agent lifecycle events: CONNECTED, DISCONNECTED, POPUP_VISIBLE.

**Why:**
- Enables rich UI feedback during voice sessions
- Allows tracking call durations for analytics
- Provides hooks for custom UI states (loading, active, ended)

**How:**
```kotlin
// New AgentEventListener.kt
enum class AgentEventType {
    CONNECTED,
    DISCONNECTED,
    POPUP_VISIBLE
}

data class AgentEvent(
    val type: AgentEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: AgentEventMetadata
)

data class AgentEventMetadata(
    val callDuration: Long? = null,
    val message: String? = null,
    val trigger: String? = null
)

// Add to AI.kt
object AI {
    fun setAgentEventListener(listener: (AgentEvent) -> Unit) {
        runtime?.agentEventListener = listener
    }
}

// Usage in host app
AI.setAgentEventListener { event ->
    when (event.type) {
        AgentEventType.CONNECTED -> updateUI(AgentStatus.ACTIVE)
        AgentEventType.DISCONNECTED -> {
            updateUI(AgentStatus.IDLE)
            logAnalytics("call_duration", event.metadata.callDuration)
        }
        AgentEventType.POPUP_VISIBLE -> showPopup(event.metadata.message)
    }
}
```

**Integration Points:**
- Hook into VoiceRoomConnector.kt LiveKit room events
- Dispatch events from SdkRuntime.kt

**Impact:**
- Improved developer experience
- Better UI/UX capabilities
- Effort: 3-4 days

**Success Metric:** 100% of voice calls trigger CONNECTED/DISCONNECTED events

---

### 2.2 Dynamic Popup Support

**What:** Backend-driven popup messages triggered by user behavior or conditions.

**Why:**
- Enables proactive user guidance
- Allows A/B testing of messaging
- Reduces need for app updates to change messaging

**How:**
```kotlin
// New PopupManager.kt
data class DynamicPopup(
    val show: Boolean,
    val message: String,
    val delayMs: Long?
)

class PopupManager(private val httpClient: HttpBackendClient) {
    suspend fun fetchPopup(userId: String): DynamicPopup? {
        return try {
            httpClient.getDynamicPopup(userId)
        } catch (e: Exception) {
            null
        }
    }
}

// Backend endpoint: GET /assistant/dynamic-popup?user_id=<id>
// Response: { "show": true, "message": "Need help?", "delay_ms": 5000 }
```

**Backend Requirements:**
- New endpoint: GET /assistant/dynamic-popup
- Logic to determine when to show popups
- Message configuration system

**Impact:**
- Backend dependency (coordinate early)
- Effort: 2-3 days SDK + 2-3 days backend
- High engagement potential

**Success Metric:** Popup display rate > 0%, measurable engagement lift

---

## Phase 3: Developer Experience (Weeks 5-6)

### 3.1 EmbedButton UI Component

**What:** Pre-built floating action button component for triggering voice assistant.

**Why:**
- Reduces integration time (no custom button needed)
- Consistent UX across KYCIS-powered apps
- Customizable but opinionated defaults

**How:**
```kotlin
// New EmbedButton.kt (Jetpack Compose)
@Composable
fun EmbedButton(
    modifier: Modifier = Modifier,
    positioning: EmbedPosition = EmbedPosition.FIXED,
    bottomOffset: Dp = 16.dp,
    rightOffset: Dp = 16.dp,
    size: Dp = 56.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    iconTint: Color = Color.White,
    onClick: () -> Unit = { AI.startAssistant() }
) {
    Box(
        modifier = modifier.then(
            when (positioning) {
                EmbedPosition.FIXED -> Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomOffset, end = rightOffset)
                EmbedPosition.EMBEDDED -> Modifier
            }
        )
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .align(if (positioning == EmbedPosition.FIXED) Alignment.BottomEnd else Alignment.Center),
            containerColor = backgroundColor,
            contentColor = iconTint
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Assistant"
            )
        }
    }
}

enum class EmbedPosition {
    FIXED,    // Floating bottom-right
    EMBEDDED  // Within container
}
```

**Design Considerations:**
- Accessibility: Content descriptions for screen readers
- Customization: Size, colors, positioning, icon
- Lifecycle: Auto-show/hide based on SDK state

**Impact:**
- Requires Jetpack Compose (document as optional)
- Effort: 3-4 days
- 50% reduction in "how to trigger assistant" support questions

**Success Metric:** 30% of integrations use EmbedButton within 3 months

---

### 3.2 Navigation Tracking Provider

**What:** Composable wrapper that automatically tracks navigation state for AndroidX Navigation.

**Why:**
- Eliminates manual `setKycStep()` calls
- Reduces boilerplate in host apps
- Automatic screen context for voice assistant

**How:**
```kotlin
// New EmbedProvider.kt
@Composable
fun EmbedProvider(
    navController: NavHostController,
    includeScreens: List<String> = emptyList(),
    excludeScreens: List<String> = emptyList(),
    onButtonVisible: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val route = destination.route ?: return@OnDestinationChangedListener
            
            val shouldShow = when {
                includeScreens.isNotEmpty() -> route in includeScreens
                excludeScreens.contains(route) -> false
                else -> true
            }
            
            onButtonVisible(shouldShow)
            AI.setKycStep(route)
        }
        
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    
    content()
}

// Usage
@Composable
fun MyApp() {
    val navController = rememberNavController()
    
    EmbedProvider(
        navController = navController,
        includeScreens = listOf("home", "profile", "kyc_flow")
    ) {
        NavHost(navController, startDestination = "home") {
            composable("home") { HomeScreen() }
            composable("profile") { ProfileScreen() }
        }
    }
}
```

**Impact:**
- AndroidX Navigation dependency
- Optional (existing apps unaffected)
- Effort: 2-3 days

**Success Metric:** 30% reduction in manual setKycStep calls

---

### 3.3 Flow-Based Activation

**What:** Support multiple API keys for different user journeys within the same app.

**Why:**
- Multi-tenant apps can use different assistant configurations
- Different flows (onboarding vs support) can have different behaviors
- Enables A/B testing of assistant strategies

**How:**
```kotlin
// New FlowConfig.kt
data class FlowConfig(
    val name: String,
    val apiKey: String,
    val baseUrl: String? = null
)

object SDKConfig {
    private val flowConfigs = mutableMapOf<String, FlowConfig>()
    private var currentFlowName: String? = null
    
    fun registerFlow(flowName: String, apiKey: String, baseUrl: String? = null) {
        flowConfigs[flowName] = FlowConfig(flowName, apiKey, baseUrl)
    }
    
    fun setCurrentFlow(flowName: String) {
        require(flowConfigs.containsKey(flowName)) { "Flow $flowName not registered" }
        currentFlowName = flowName
    }
    
    fun getCurrentFlowApiKey(): String? {
        return currentFlowName?.let { flowConfigs[it]?.apiKey }
    }
}

// Usage
SDKConfig.registerFlow("onboarding", "api-key-onboarding")
SDKConfig.registerFlow("support", "api-key-support")

// Switch flows
AI.setFlow("onboarding")
AI.setKycStep("welcome") // Uses onboarding API key
```

**Impact:**
- Medium complexity (flow context management)
- Backward compatible (default flow uses existing init)
- Effort: 3-4 days

**Success Metric:** Support 3+ flows in pilot app

---

## Phase 4: Security & Reliability (Weeks 7-8)

### 4.1 Certificate Pinning

**What:** Implement SSL certificate pinning to prevent man-in-the-middle attacks.

**Why:**
- Critical security gap in both KYCIS and Revrag
- Protects sensitive KYC data in transit
- Industry best practice for financial/identity apps

**How:**
```kotlin
// Update HttpBackendClient.kt
val certificatePinner = CertificatePinner.Builder()
    .add("api.kycis.com", "sha256/PRIMARY_CERT_HASH")
    .add("api.kycis.com", "sha256/BACKUP_CERT_HASH")
    .build()

val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()
```

**Considerations:**
- Certificate rotation strategy needed
- Backup pins for seamless rotation
- Emergency bypass mechanism for cert issues

**Impact:**
- High security value
- Medium maintenance burden
- Effort: 1-2 days

**Success Metric:** Zero MITM vulnerabilities in security audit

---

### 4.2 Offline Event Queue

**What:** Persist events locally when network unavailable, retry when connectivity returns.

**Why:**
- Prevents data loss in poor network conditions
- Critical for mobile apps with intermittent connectivity
- Improves data reliability and completeness

**How:**
```kotlin
// New OfflineEventQueue.kt
class OfflineEventQueue(context: Context) {
    private val prefs = context.getSharedPreferences("kycis_event_queue", MODE_PRIVATE)
    private val gson = Gson()
    
    fun enqueue(event: SdkEvent) {
        val queue = getQueue().toMutableList()
        queue.add(gson.toJson(event))
        prefs.edit().putStringSet("events", queue.toSet()).apply()
    }
    
    suspend fun flush(httpClient: HttpBackendClient) {
        val queue = getQueue()
        queue.forEach { eventJson ->
            try {
                val event = gson.fromJson(eventJson, SdkEvent::class.java)
                httpClient.sendEvent(event)
                removeFromQueue(eventJson)
            } catch (e: Exception) {
                return // Keep for next retry
            }
        }
    }
    
    private fun getQueue(): List<String> {
        return prefs.getStringSet("events", emptySet())?.toList() ?: emptyList()
    }
}
```

**Integration:**
- Hook into network state changes
- Periodic flush attempts
- Max queue size limits

**Impact:**
- High data reliability improvement
- Low storage overhead
- Effort: 2-3 days

**Success Metric:** <1% event loss rate in poor network conditions

---

### 4.3 Local Data Encryption

**What:** Encrypt sensitive data stored locally (user_id, session tokens).

**Why:**
- Security gap in both SDKs
- Protects against device compromise
- Compliance requirement for financial apps

**How:**
```kotlin
// Use Android EncryptedSharedPreferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "kycis_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**Impact:**
- High security value
- Minimal performance overhead
- Effort: 1-2 days

**Success Metric:** All sensitive data encrypted at rest

---

## Phase 5: Future Considerations (Weeks 9+)

### 5.1 Cross-Platform SDK Expansion

**What:** React Native and Flutter SDK implementations.

**Why:**
- Market demand for cross-platform support
- Competitive parity with Revrag
- Broader market reach

**Evaluation Criteria:**
- Client demand (survey existing/potential customers)
- Development resources available
- Maintenance burden vs revenue potential
- Time to market vs native quality

**Estimated Effort:** 8-12 weeks per platform

---

### 5.2 Advanced Route Matching

**What:** Support pattern-based route matching (exact, startsWith, contains).

**Why:**
- More flexible screen visibility rules
- Reduces configuration complexity for large apps
- Matches Revrag Flutter capability

**How:**
```kotlin
enum class RouteMatchMode {
    EXACT,
    STARTS_WITH,
    CONTAINS
}

data class RouteConfig(
    val patterns: List<String>,
    val mode: RouteMatchMode = RouteMatchMode.EXACT
)

// Usage
EmbedProvider(
    navController = navController,
    includeRoutes = RouteConfig(
        patterns = listOf("kyc_", "verification_"),
        mode = RouteMatchMode.STARTS_WITH
    )
)
```

**Impact:**
- Low priority (nice-to-have)
- Effort: 1-2 days

---

### 5.3 Session Persistence

**What:** Persist session_id across app restarts.

**Why:**
- Better analytics continuity
- Improved user journey tracking
- Reduced session fragmentation

**How:**
- Store session_id in EncryptedSharedPreferences
- Implement session timeout logic
- Generate new session after timeout or explicit logout

**Impact:**
- Improved analytics quality
- Effort: 1-2 days

---

### 5.4 Crash Reporting Integration

**What:** Optional integration with Firebase Crashlytics or similar.

**Why:**
- Better SDK error visibility
- Proactive issue detection
- Improved support experience

**How:**
```kotlin
// Optional crash reporter interface
interface CrashReporter {
    fun logException(throwable: Throwable, context: Map<String, String>)
    fun setUserIdentifier(userId: String)
}

// Host app provides implementation
AI.setCrashReporter(object : CrashReporter {
    override fun logException(throwable: Throwable, context: Map<String, String>) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }
})
```

**Impact:**
- Optional (no breaking changes)
- Effort: 2-3 days

---

## Implementation Timeline

```
Week 1-2: Foundation
├── SDK Metadata in Events (2-3 days)
├── Event Ordering Validation (1-2 days)
└── Analytics Event Type (1-2 days)

Week 3-4: Agent Events
├── Agent Event Callbacks (3-4 days)
└── Dynamic Popup Support (2-3 days SDK + backend)

Week 5-6: Developer Experience
├── EmbedButton Component (3-4 days)
├── Navigation Provider (2-3 days)
└── Flow-Based Activation (3-4 days)

Week 7-8: Security & Reliability
├── Certificate Pinning (1-2 days)
├── Offline Event Queue (2-3 days)
└── Local Data Encryption (1-2 days)

Week 9+: Future Enhancements
├── Cross-Platform SDKs (8-12 weeks each)
├── Advanced Route Matching (1-2 days)
├── Session Persistence (1-2 days)
└── Crash Reporting Integration (2-3 days)
```

**Total Core Implementation:** 26-36 days (5-7 weeks)

---

## Dependency Graph

```
SDK Metadata ────────┐
                     │
Event Ordering ──────┼──► Backend Coordination Required
                     │
Analytics Events ────┘
        │
        ▼
Agent Event Callbacks ◄──► Dynamic Popup Support
        │
        ▼
EmbedButton Component
        │
        ▼
Navigation Provider
        │
        ▼
Flow-Based Activation

Certificate Pinning ──────► Independent (can parallelize)
Offline Queue ────────────► Independent (can parallelize)
Local Encryption ─────────► Independent (can parallelize)
```

---

## Risk Assessment

### High-Risk Items

| Item | Risk | Mitigation Strategy |
|------|------|---------------------|
| Dynamic Popup | Backend dependency | Early coordination, mock endpoints for testing |
| SDK Metadata | Backend breaking change | Maintain backward compatibility, gradual rollout |
| Event Ordering | Could break existing apps | Phased enforcement (warnings → debug → production) |

### Medium-Risk Items

| Item | Risk | Mitigation Strategy |
|------|------|---------------------|
| EmbedButton | Compose dependency | Make optional, document requirements clearly |
| Navigation Provider | AndroidX specific | Document limitations, provide manual alternative |
| Flow Activation | Complex state management | Thorough testing, clear documentation |

### Low-Risk Items

| Item | Risk | Mitigation Strategy |
|------|------|---------------------|
| Agent Events | API surface change | Additive only, no breaking changes |
| Analytics Events | Backend processing | New pipeline, existing events unaffected |
| Certificate Pinning | Certificate rotation | Backup pins, emergency bypass mechanism |

---

## Success Metrics & KPIs

### Integration Metrics

| Metric | Baseline | Target | Timeline |
|--------|----------|--------|----------|
| Integration time (new apps) | 4-6 hours | 2-3 hours | 6 months |
| Lines of integration code | 150-200 | 50-100 | 6 months |
| Support tickets (integration) | 10/month | 3/month | 3 months |
| EmbedButton adoption | 0% | 30% | 3 months |
| Navigation Provider adoption | 0% | 25% | 6 months |

### Technical Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Event delivery success rate | >99.5% | Backend logs |
| SDK crash rate | <0.01% | Crash reporting |
| Event ordering violations | 0 | SDK validation logs |
| Certificate pinning coverage | 100% | Security audit |
| Offline event recovery | >99% | Queue flush logs |

### Business Metrics

| Metric | Target | Impact |
|--------|--------|--------|
| Voice session completion rate | +15% | Agent event tracking |
| User assistance engagement | +25% | Dynamic popup analytics |
| Developer satisfaction (NPS) | +20 points | Developer surveys |
| Time to first voice session | -40% | Integration analytics |

---

## Architecture Comparison Summary

### Revrag SDK Architecture
- **Paradigm:** Widget-centric, event-driven
- **Control:** SDK manages UI (EmbedButton)
- **Navigation:** Automatic via EmbedProvider
- **State:** Persistent (AsyncStorage)
- **Triggering:** User-initiated, event-based

### KYCIS SDK Architecture
- **Paradigm:** Host-driven, passive-tracking
- **Control:** Host app manages UI
- **Navigation:** Manual via setKycStep()
- **State:** Runtime in-memory
- **Triggering:** Passive evaluation, auto-trigger

### Hybrid Target Architecture
- **Paradigm:** Flexible (host-driven OR widget-centric)
- **Control:** Optional EmbedButton OR custom UI
- **Navigation:** Optional EmbedProvider OR manual
- **State:** Hybrid (critical data persisted, runtime cached)
- **Triggering:** Both passive evaluation AND event-based

---

## Feature Comparison Matrix

| Feature | Revrag | KYCIS Current | KYCIS Target | Priority |
|---------|--------|---------------|--------------|----------|
| SDK Metadata in Events | ✅ | ❌ | ✅ | High |
| Event Ordering Validation | ✅ | ❌ | ✅ | High |
| Agent Event Callbacks | ✅ | ❌ | ✅ | High |
| Dynamic Popup | ✅ | ❌ | ✅ | High |
| EmbedButton Component | ✅ | ❌ | ✅ | Medium |
| Navigation Provider | ✅ | ❌ | ✅ | Medium |
| Flow-Based Activation | ✅ | ❌ | ✅ | Medium |
| Analytics Event Type | ✅ | ❌ | ✅ | Medium |
| Certificate Pinning | ❌ | ❌ | ✅ | High |
| Offline Event Queue | ❌ | ❌ | ✅ | High |
| Local Data Encryption | ❌ | ❌ | ✅ | High |
| Remote Configuration | ❌ | ✅ | ✅ | - |
| Schema Registry | ❌ | ✅ | ✅ | - |
| Passive Evaluation | ❌ | ✅ | ✅ | - |
| Auto-Trigger Logic | ❌ | ✅ | ✅ | - |
| Validation Tracking | ❌ | ✅ | ✅ | - |
| W3C Trace Propagation | ❌ | ✅ | ✅ | - |

---

## API Contract Evolution

### Current KYCIS Event Payload
```json
{
  "user_id": "user123",
  "session_id": "sess_abc",
  "event": "validation_failed",
  "screen": "verification",
  "timestamp": 1712000000,
  "sdk_version": "1.0.0",
  "client": "android-sdk"
}
```

### Target Enhanced Payload
```json
{
  "user_id": "user123",
  "session_id": "sess_abc",
  "event": "validation_failed",
  "screen": "verification",
  "timestamp": 1712000000,
  "sdk": {
    "sdk_name": "kycis-android",
    "sdk_version": "2.0.0",
    "platform": "android"
  },
  "context": {
    "flow": "onboarding",
    "app_version": "1.5.0"
  },
  "traceparent": "00-abc123-def456-01"
}
```

---

## Technical Specifications

### Backend API Changes Required

#### New Endpoints

1. **GET /assistant/dynamic-popup**
   - Query params: `user_id`
   - Response: `{ "show": boolean, "message": string, "delay_ms": number }`
   - Purpose: Backend-driven popup content

2. **Enhanced POST /events**
   - Accept nested `sdk` object
   - Accept nested `context` object
   - Maintain backward compatibility with flat structure

#### Schema Updates

```json
// Event schema v2.0
{
  "user_id": "string (required)",
  "session_id": "string (optional)",
  "event": "string (required)",
  "screen": "string (optional)",
  "timestamp": "number (required)",
  "sdk": {
    "sdk_name": "string (required)",
    "sdk_version": "string (required)",
    "platform": "string (required)"
  },
  "context": {
    "flow": "string (optional)",
    "app_version": "string (optional)",
    "route_hierarchy": "array (optional)"
  },
  "properties": "object (optional)",
  "traceparent": "string (optional)"
}
```

---

## Documentation Requirements

For each implemented feature, deliver:

1. **KDoc API Documentation**
   - Public API surface documentation
   - Code examples in KDoc comments
   - Parameter descriptions and constraints

2. **Integration Guide Updates**
   - Step-by-step integration instructions
   - Migration guide for existing apps
   - Breaking changes clearly marked

3. **Sample App Updates**
   - Working examples for each new feature
   - Best practices demonstrated
   - Common pitfalls documented

4. **Changelog Entries**
   - Version 2.0.0 release notes
   - Breaking changes section
   - Deprecation notices

5. **Architecture Documentation**
   - Updated architecture diagrams
   - Sequence diagrams for new flows
   - Component interaction diagrams

---

## Testing Strategy

### Unit Tests
- Event validation logic
- Payload enrichment
- Flow switching
- Queue operations

### Integration Tests
- Backend API contract tests
- Event ordering enforcement
- Navigation tracking
- Agent event callbacks

### E2E Tests
- Full initialization flow
- Voice session lifecycle
- Offline queue recovery
- Multi-flow scenarios

### Performance Tests
- Event throughput
- Memory usage
- Battery impact
- Network efficiency

### Security Tests
- Certificate pinning validation
- Encryption verification
- Permission handling
- Data sanitization

---

## Competitive Analysis Summary

### What We Learned from Revrag

**Strengths to Adopt:**
- Automatic navigation tracking reduces integration friction
- Pre-built UI components accelerate time-to-market
- Real-time agent events enable richer UX
- Flow-based activation supports complex apps
- Simple initialization API (one-line setup)

**Weaknesses to Avoid:**
- No runtime configuration (inflexible)
- No feature flags (can't A/B test)
- No validation tracking (limited context)
- Static configuration only (requires app updates)
- Generic error handling (harder to debug)

### KYCIS Competitive Advantages

**Maintain These:**
- Multi-layer configuration system
- Remote feature flags and thresholds
- Schema registry for voice context
- Passive evaluation and auto-trigger
- Structured validation tracking
- W3C trace propagation
- Component-level input hints

**Add These from Revrag:**
- EmbedButton and EmbedProvider
- Agent event callbacks
- Dynamic popups
- Flow-based activation
- Analytics event type

**Add These Missing from Both:**
- Certificate pinning
- Offline event queue
- Local data encryption

---

## Key Architectural Decisions

### Decision 1: Optional vs Required Components

**Decision:** Make EmbedButton and EmbedProvider optional.

**Rationale:**
- Existing apps shouldn't be forced to refactor
- Some apps need custom UI
- Compose dependency shouldn't be mandatory

**Implementation:**
- Separate module: `kycis-sdk-ui` (optional dependency)
- Core SDK remains UI-agnostic
- Clear documentation on when to use each approach

---

### Decision 2: Event Ordering Enforcement

**Decision:** Enforce event ordering with phased rollout.

**Rationale:**
- Data integrity is critical
- Breaking existing apps is unacceptable
- Gradual migration reduces risk

**Implementation:**
- Phase 1: Warnings only (log, don't throw)
- Phase 2: Throw in debug builds
- Phase 3: Enforce in all builds after 1 release

---

### Decision 3: Backward Compatibility

**Decision:** All changes must be backward compatible or clearly versioned.

**Rationale:**
- Existing integrations must continue working
- Breaking changes require major version bump
- Migration path must be clear

**Implementation:**
- Backend accepts both old and new payload formats
- SDK version negotiation in headers
- Deprecation warnings for old APIs

---

### Decision 4: Security First

**Decision:** Prioritize security enhancements (pinning, encryption, offline queue).

**Rationale:**
- KYC data is highly sensitive
- Regulatory compliance requirements
- Competitive differentiator

**Implementation:**
- Certificate pinning in Phase 4
- Encryption for all stored data
- Offline queue with encryption

---

## Resource Requirements

### Development Team

| Role | Allocation | Duration |
|------|------------|----------|
| Android SDK Engineer | 100% | 8 weeks |
| Backend Engineer | 50% | 4 weeks |
| QA Engineer | 50% | 6 weeks |
| Technical Writer | 25% | 4 weeks |
| DevRel/Support | 25% | Ongoing |

### Infrastructure

- Staging environment for testing
- Certificate management system
- Analytics pipeline updates
- Documentation hosting

### External Dependencies

- Jetpack Compose (for UI components)
- AndroidX Navigation (for provider)
- OkHttp (for certificate pinning)
- Android Security Crypto (for encryption)

---

## Migration Guide for Existing Apps

### Version 1.x → 2.0 Migration

#### Breaking Changes
None - all changes are additive or opt-in.

#### Recommended Updates

1. **Add SDK Metadata Support**
```kotlin
// No code changes required - automatic
// Backend will receive enhanced payloads
```

2. **Adopt Event Ordering**
```kotlin
// Ensure identity is sent first
AI.setUser(userId = "user123", phone = "+1234567890") // Sends identity event
AI.setKycStep("verification") // Now safe to send other events
```

3. **Use Analytics Events**
```kotlin
// Old way (still works)
AI.trackError("custom_event", mapOf("action" to "completed"))

// New way (recommended)
AI.trackAnalytics("kyc_completed", mapOf("duration" to 120))
```

4. **Optional: Add EmbedButton**
```kotlin
// Add dependency
implementation("com.kycis:kycis-sdk-ui:2.0.0")

// Use in Compose
@Composable
fun MyScreen() {
    Box {
        // Your content
        EmbedButton()
    }
}
```

5. **Optional: Add Navigation Provider**
```kotlin
@Composable
fun MyApp() {
    val navController = rememberNavController()
    
    EmbedProvider(navController = navController) {
        // Your navigation setup
    }
}
```

---

## Rollout Strategy

### Beta Phase (Week 9-10)

**Participants:** 2-3 pilot customers

**Features:**
- SDK Metadata
- Event Ordering (warnings only)
- Analytics Events
- Agent Event Callbacks

**Goals:**
- Validate API changes
- Gather feedback on new APIs
- Identify integration issues

---

### Release Candidate (Week 11-12)

**Participants:** All existing customers (opt-in)

**Features:**
- All Phase 1-3 features
- EmbedButton and EmbedProvider
- Dynamic Popup support

**Goals:**
- Stress test at scale
- Validate documentation
- Measure performance impact

---

### General Availability (Week 13)

**Release:** Version 2.0.0

**Features:**
- All core enhancements
- Security features (pinning, encryption, queue)
- Complete documentation

**Support:**
- Migration webinar
- Office hours for integration support
- Dedicated Slack channel

---

## Detailed Implementation Specifications

### Spec 1: SDK Metadata Enhancement

**Files to Modify:**
- `HttpBackendClient.kt` - Add SdkMetadata to event builder
- `SdkRuntime.kt` - Provide SDK version from BuildConfig
- Backend event schema - Accept nested `sdk` object

**Implementation Steps:**
1. Create `SdkMetadata` data class
2. Update event payload builder to include `sdk` object
3. Extract SDK version from BuildConfig or resources
4. Update backend schema to accept nested structure
5. Add backward compatibility layer
6. Write unit tests for payload structure
7. Update API documentation

**Validation:**
- Unit test: Verify `sdk` object in all event types
- Integration test: Backend accepts and stores metadata
- Regression test: Old clients still work

---

### Spec 2: Event Ordering Validation

**Files to Create:**
- `EventValidator.kt` - Validation and enrichment logic

**Files to Modify:**
- `HttpBackendClient.kt` - Integrate validator
- `AI.kt` - Expose reset method if needed

**Implementation Steps:**
1. Create EventValidator with state management
2. Implement validateAndEnrich method
3. Integrate into event sending pipeline
4. Add configuration flag for enforcement level
5. Implement reset on logout/session end
6. Write comprehensive unit tests
7. Add logging for debugging

**Edge Cases:**
- Multiple users in same session
- App restart with cached user
- Logout and re-login flows

---

### Spec 3: Agent Event Callbacks

**Files to Create:**
- `AgentEventListener.kt` - Event types and data classes
- `AgentEventDispatcher.kt` - Event emission logic

**Files to Modify:**
- `VoiceRoomConnector.kt` - Hook LiveKit events
- `SdkRuntime.kt` - Register and manage listeners
- `AI.kt` - Public API for setting listener

**Implementation Steps:**
1. Define AgentEventType enum
2. Create AgentEvent and AgentEventMetadata data classes
3. Implement event dispatcher with thread safety
4. Hook into LiveKit room connection events
5. Add call duration tracking
6. Implement popup event support
7. Write integration tests with mock LiveKit

**LiveKit Integration Points:**
- `room.onConnected` → AGENT_CONNECTED
- `room.onDisconnected` → AGENT_DISCONNECTED
- Track connection timestamp for duration calculation

---

### Spec 4: Dynamic Popup Support

**Files to Create:**
- `PopupManager.kt` - Popup fetching and caching
- `DynamicPopup.kt` - Data class

**Files to Modify:**
- `HttpBackendClient.kt` - Add getDynamicPopup method
- `SdkRuntime.kt` - Integrate popup checking

**Implementation Steps:**
1. Create DynamicPopup data class
2. Implement PopupManager with caching
3. Add backend endpoint call
4. Implement delay logic
5. Trigger POPUP_VISIBLE agent event
6. Add configuration for polling interval
7. Write tests with mock backend

**Backend Coordination:**
- Define popup trigger rules
- Implement message configuration system
- Add analytics for popup effectiveness

---

### Spec 5: EmbedButton Component

**Files to Create:**
- `kycis-sdk-ui/EmbedButton.kt` - Composable component
- `kycis-sdk-ui/EmbedPosition.kt` - Positioning enum
- `kycis-sdk-ui/EmbedButtonDefaults.kt` - Default values

**Implementation Steps:**
1. Create new Gradle module `kycis-sdk-ui`
2. Add Jetpack Compose dependencies
3. Implement EmbedButton composable
4. Add customization parameters (size, colors, position)
5. Implement accessibility features
6. Add state management (loading, active, idle)
7. Write Compose UI tests
8. Create sample app demonstrating usage

**Customization Options:**
- Position: Fixed (floating) or Embedded
- Size: Small (40dp), Medium (56dp), Large (72dp)
- Colors: Background, icon tint
- Offsets: Bottom, right, top, left
- Icon: Custom icon support
- Animation: Pulse, ripple effects

---

### Spec 6: Navigation Provider

**Files to Create:**
- `kycis-sdk-ui/EmbedProvider.kt` - Provider composable
- `kycis-sdk-ui/RouteConfig.kt` - Route configuration

**Implementation Steps:**
1. Create EmbedProvider composable
2. Implement NavController listener
3. Add include/exclude screen logic
4. Implement route matching modes
5. Add button visibility callback
6. Integrate with AI.setKycStep()
7. Write integration tests
8. Document AndroidX Navigation requirement

**Configuration Options:**
```kotlin
EmbedProvider(
    navController = navController,
    includeScreens = listOf("kyc_*"), // Pattern matching
    excludeScreens = listOf("login", "splash"),
    matchMode = RouteMatchMode.STARTS_WITH,
    onButtonVisible = { visible -> /* custom logic */ }
) {
    // App content
}
```

---

### Spec 7: Flow-Based Activation

**Files to Create:**
- `FlowConfig.kt` - Flow configuration data class
- `FlowManager.kt` - Flow registration and switching

**Files to Modify:**
- `AI.kt` - Add flow management methods
- `HttpBackendClient.kt` - Use flow-specific API keys
- `SdkRuntime.kt` - Track current flow

**Implementation Steps:**
1. Create FlowConfig data class
2. Implement flow registration system
3. Add flow switching logic
4. Update API key resolution
5. Add flow context to events
6. Implement flow transition handling
7. Write tests for multi-flow scenarios
8. Document flow lifecycle

**API Design:**
```kotlin
// Registration (in Application.onCreate)
SDKConfig.registerFlow("onboarding", "api-key-1")
SDKConfig.registerFlow("support", "api-key-2")

// Switching flows
AI.setFlow("onboarding")
AI.setKycStep("welcome") // Uses onboarding API key

AI.setFlow("support")
AI.setKycStep("help") // Uses support API key
```

---

## Code Quality & Maintenance

### Code Standards

- Kotlin coding conventions
- KDoc for all public APIs
- Unit test coverage >80%
- Integration test coverage >60%
- No new lint warnings
- ProGuard rules documented

### Performance Budgets

| Metric | Budget | Measurement |
|--------|--------|-------------|
| SDK initialization time | <500ms | Profiler |
| Event send latency | <100ms | Network logs |
| Memory overhead | <10MB | Memory profiler |
| APK size increase | <500KB | Build output |
| Battery drain | <2% per hour | Battery historian |

### Monitoring & Observability

**SDK Metrics to Track:**
- Initialization success rate
- Event delivery success rate
- Voice session completion rate
- Agent event callback invocations
- Popup display rate
- Navigation tracking accuracy
- Offline queue size and flush rate

**Alerting Thresholds:**
- Event delivery failure >1%
- SDK crash rate >0.01%
- Initialization failure >0.5%
- Certificate pinning failures >0%

---

## Communication Plan

### Internal Stakeholders

**Engineering Team:**
- Weekly progress updates
- Architecture review sessions
- Code review requirements
- Testing coordination

**Product Team:**
- Feature demos at milestones
- Beta feedback sessions
- Success metrics review
- Roadmap adjustments

**Support Team:**
- Early access to documentation
- Migration guide training
- Common issues playbook
- Escalation procedures

### External Stakeholders

**Existing Customers:**
- Email announcement of v2.0 roadmap
- Beta program invitation
- Migration webinar
- Office hours for questions

**New Prospects:**
- Updated marketing materials
- Feature comparison matrix
- Demo app showcasing new features
- Integration time estimates

---

## Effort Estimation Summary

| Phase | Feature | Effort (Days) | Dependencies | Risk |
|-------|---------|---------------|--------------|------|
| **Phase 1** | SDK Metadata | 2-3 | Backend | Medium |
| | Event Ordering | 1-2 | None | Low |
| | Analytics Events | 1-2 | None | Low |
| **Phase 2** | Agent Callbacks | 3-4 | None | Low |
| | Dynamic Popup | 2-3 | Backend | Medium |
| **Phase 3** | EmbedButton | 3-4 | Compose | Low |
| | Navigation Provider | 2-3 | AndroidX | Low |
| | Flow Activation | 3-4 | None | Medium |
| **Phase 4** | Certificate Pinning | 1-2 | None | Low |
| | Offline Queue | 2-3 | None | Low |
| | Data Encryption | 1-2 | None | Low |

**Total Estimated Effort:** 22-32 development days (4.5-6.5 weeks)  
**With Testing & Documentation:** 35-45 days (7-9 weeks)

---

## Appendix A: Revrag vs KYCIS API Comparison

### Initialization

**Revrag React Native:**
```javascript
const { isInitialized, error } = useInitialize({
  apiKey: 'YOUR_API_KEY',
  embedUrl: 'https://embed.revrag.ai'
});
```

**KYCIS Current:**
```kotlin
AI.init(
    application = this,
    apiKey = "YOUR_API_KEY",
    userId = "user123"
)
```

**KYCIS Target (v2.0):**
```kotlin
AI.init(
    application = this,
    apiKey = "YOUR_API_KEY",
    userId = "user123",
    runtimePolicy = RuntimePolicy.Builder()
        .setBackendBaseUrl("https://api.kycis.com")
        .build()
)

// Optional: Register flows
SDKConfig.registerFlow("onboarding", "api-key-1")
SDKConfig.registerFlow("support", "api-key-2")
```

---

### Event Tracking

**Revrag:**
```javascript
// Must send USER_DATA first
await Embed.Event('user_data', { app_user_id: 'user123' });

// Other events auto-enriched
await Embed.Event('screen_state', { screen: 'home' });
await Embed.Event('analytics_data', { event_name: 'purchase' });
```

**KYCIS Current:**
```kotlin
AI.setKycStep("verification")
AI.trackValidationFailure("INVALID_FORMAT", "phone")
AI.trackError("custom_event", mapOf("key" to "value"))
```

**KYCIS Target (v2.0):**
```kotlin
// Identity enforced first (with warnings)
AI.setUser(userId = "user123", phone = "+1234567890")

// Auto-enriched with user_id
AI.setKycStep("verification")
AI.trackAnalytics("kyc_started", mapOf("flow" to "onboarding"))
```

---

### Navigation Tracking

**Revrag:**
```javascript
<EmbedProvider
  navigationRef={navigationRef}
  includeScreens={['Home', 'Profile']}
>
  <NavigationContainer>
    <RootNavigator />
  </NavigationContainer>
</EmbedProvider>
```

**KYCIS Current:**
```kotlin
// Manual tracking
override fun onResume() {
    super.onResume()
    AI.setKycStep("verification_screen")
}
```

**KYCIS Target (v2.0):**
```kotlin
// Option 1: Automatic (new)
@Composable
fun MyApp() {
    val navController = rememberNavController()
    EmbedProvider(navController = navController) {
        NavHost(navController, "home") { /* routes */ }
    }
}

// Option 2: Manual (still supported)
AI.setKycStep("verification_screen")
```

---

### Voice Agent Events

**Revrag:**
```javascript
Embed.event.on(AgentEvent.AGENT_CONNECTED, (data) => {
  console.log('Connected:', data.timestamp);
});
```

**KYCIS Current:**
```kotlin
AI.setVoiceSessionListener { result ->
    // Only called when credentials received
}
```

**KYCIS Target (v2.0):**
```kotlin
AI.setAgentEventListener { event ->
    when (event.type) {
        AgentEventType.CONNECTED -> showActiveUI()
        AgentEventType.DISCONNECTED -> {
            showIdleUI()
            logDuration(event.metadata.callDuration)
        }
        AgentEventType.POPUP_VISIBLE -> showPopup(event.metadata.message)
    }
}
```

---

## Appendix B: Backend API Specifications

### Endpoint 1: Enhanced Event Ingestion

**Current:**
```
POST /events
Content-Type: application/json
X-API-Key: <key>

{
  "user_id": "user123",
  "event": "validation_failed",
  "timestamp": 1712000000,
  "sdk_version": "1.0.0"
}
```

**Target (v2.0):**
```
POST /events
Content-Type: application/json
X-API-Key: <key>

{
  "user_id": "user123",
  "event": "validation_failed",
  "timestamp": 1712000000,
  "sdk": {
    "sdk_name": "kycis-android",
    "sdk_version": "2.0.0",
    "platform": "android"
  },
  "context": {
    "flow": "onboarding",
    "app_version": "1.5.0"
  }
}
```

---

### Endpoint 2: Dynamic Popup (New)

```
GET /assistant/dynamic-popup?user_id=<user_id>
X-API-Key: <key>

Response:
{
  "show": true,
  "message": "Need help with verification?",
  "delay_ms": 5000,
  "trigger": "high_time_spent"
}
```

**Backend Logic:**
- Check user's current screen and time spent
- Evaluate error count and patterns
- Apply business rules for popup display
- Return appropriate message

---

### Endpoint 3: Flow-Specific Configuration (Future)

```
GET /sdk/config?flow=<flow_name>
X-API-Key: <flow_api_key>

Response:
{
  "feature_flags": {
    "auto_trigger": true,
    "schema_registry": true
  },
  "thresholds": {
    "trigger_time_spent_seconds": 20
  },
  "flow_config": {
    "name": "onboarding",
    "screens": ["welcome", "personal_info", "verification"]
  }
}
```

---

## Appendix C: Reverse Engineering Insights

### Key Findings from Revrag SDK Analysis

**Architecture Insights:**
- Widget-centric design reduces integration code by ~60%
- Automatic navigation tracking eliminates manual calls
- Event enrichment reduces payload duplication
- LiveKit integration requires native setup (complexity)

**API Design Patterns:**
- Hook-based initialization (React/RN)
- Provider pattern for context (navigation, config)
- Event emitter pattern for agent lifecycle
- Strict event ordering for data integrity

**Security Observations:**
- No certificate pinning (vulnerability)
- AsyncStorage without encryption (risk)
- No offline queue (data loss risk)
- Basic permission handling only

**Performance Characteristics:**
- Minimal SDK footprint (~500KB)
- Event batching not implemented
- LiveKit polling impacts battery
- No worker thread optimization

---

### What We Reverse-Engineered

**Artifacts Analyzed:**
- React Native SDK package (@revrag-ai/embed-react-native@1.0.34)
- Source maps and compiled JavaScript
- Native Android bindings (Java/C++)
- Native iOS bindings (Objective-C++)
- Official documentation (docs.revrag.ai)
- API contracts and payloads

**Methods Used:**
- npm pack extraction
- Source map analysis
- Static code analysis (ripgrep)
- API endpoint discovery
- Mock backend replication
- E2E flow simulation

**Files Created:**
- Mock backend (server.js)
- Replicated SDK (index.js)
- E2E test runner (run-e2e.js)
- Comprehensive analysis documents

---

## Appendix D: Implementation Checklist

### Phase 1: Foundation (Weeks 1-2)

- [ ] Create SdkMetadata data class
- [ ] Update event payload builder
- [ ] Coordinate backend schema changes
- [ ] Write unit tests for metadata
- [ ] Create EventValidator class
- [ ] Implement validation logic with phased enforcement
- [ ] Add analytics_data event type
- [ ] Update AI.kt with trackAnalytics method
- [ ] Write integration tests
- [ ] Update documentation

### Phase 2: Agent Events (Weeks 3-4)

- [ ] Define AgentEventType enum
- [ ] Create AgentEvent data classes
- [ ] Implement event dispatcher
- [ ] Hook into VoiceRoomConnector
- [ ] Add call duration tracking
- [ ] Create PopupManager class
- [ ] Implement backend popup endpoint
- [ ] Add popup polling logic
- [ ] Write integration tests
- [ ] Update sample app

### Phase 3: Developer Experience (Weeks 5-6)

- [ ] Create kycis-sdk-ui module
- [ ] Implement EmbedButton composable
- [ ] Add customization options
- [ ] Implement accessibility features
- [ ] Create EmbedProvider composable
- [ ] Add navigation listener logic
- [ ] Implement route matching
- [ ] Create FlowConfig system
- [ ] Implement flow switching
- [ ] Write comprehensive tests
- [ ] Update documentation

### Phase 4: Security (Weeks 7-8)

- [ ] Implement certificate pinning
- [ ] Add certificate rotation strategy
- [ ] Create OfflineEventQueue class
- [ ] Implement queue persistence
- [ ] Add network state monitoring
- [ ] Implement encrypted storage
- [ ] Migrate sensitive data to encrypted prefs
- [ ] Write security tests
- [ ] Conduct security audit
- [ ] Update security documentation

### Documentation & Release

- [ ] Update API documentation
- [ ] Write migration guide
- [ ] Create video tutorials
- [ ] Update sample apps
- [ ] Write release notes
- [ ] Prepare beta announcement
- [ ] Create support materials
- [ ] Schedule webinar

---

## Appendix E: Glossary

**Agent Event:** Real-time callback triggered by voice agent lifecycle changes (connected, disconnected, popup).

**Certificate Pinning:** Security technique that validates server certificates against known values to prevent MITM attacks.

**Dynamic Popup:** Backend-driven message displayed to users based on behavior or conditions.

**EmbedButton:** Pre-built UI component for triggering voice assistant.

**EmbedProvider:** Composable wrapper that automatically tracks navigation and manages button visibility.

**Event Enrichment:** Automatic addition of context (user_id, flow, etc.) to event payloads.

**Event Ordering:** Requirement that identity event must be sent before other events.

**Flow-Based Activation:** Support for multiple API keys and configurations for different user journeys.

**Offline Queue:** Local persistence of events when network is unavailable, with automatic retry.

**Passive Evaluation:** Background monitoring of user behavior to determine when to trigger assistant.

**Schema Registry:** Declarative system for registering screen components and validation rules.

**SDK Metadata:** Nested object in event payloads containing sdk_name, sdk_version, and platform.

---

## Appendix F: References

### Source Documents
- `reverse-engineered-data/findings/enhancement-roadmap.md` - Original roadmap
- `reverse-engineered-data/findings/sdk-benchmark-analysis.md` - Detailed comparison
- `reverse-engineered-data/findings/initial-findings.md` - Extraction findings
- `reverse-engineered-data/findings/summary.md` - E2E reproduction summary
- `reverse-engineered-data/findings/vendor-docs-summary.md` - Official docs analysis
- `reverse-engineered-data/findings/top-symbol-mapping.md` - API specifications
- `reverse-engineered-data/findings/diagrams.md` - Architecture diagrams

### External References
- Revrag Documentation: https://docs.revrag.ai
- LiveKit Documentation: https://docs.livekit.io
- Auth0 SDK Best Practices
- Android Security Best Practices
- Jetpack Compose Guidelines
- AndroidX Navigation Documentation

---

## Next Actions

### Immediate (This Week)
1. Review roadmap with engineering team
2. Prioritize Phase 1 features
3. Coordinate with backend team on schema changes
4. Set up development environment
5. Create feature branches

### Short-Term (Next 2 Weeks)
1. Begin Phase 1 implementation
2. Write unit tests
3. Create mock backend endpoints
4. Update sample app
5. Draft migration guide

### Medium-Term (Next 2 Months)
1. Complete Phases 1-3
2. Beta program launch
3. Gather feedback
4. Iterate on APIs
5. Prepare for GA release

---

**Document Status:** Final  
**Approved By:** [Pending]  
**Review Date:** April 1, 2026  
**Next Review:** After Phase 1 completion

