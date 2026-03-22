package com.kycis.sdk.core

import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

data class RuntimeContext(
    val apiKey: String,
    val userId: String,
    val sessionId: String,
)

data class TriggerSignals(
    val screen: String?,
    val timeSpent: Int?,
    val errors: List<String>,
    val idleSeconds: Int?,
)

data class TriggerDecision(
    val trigger: Boolean,
    val reason: String,
    val action: String,
)

internal class SdkRuntime {
    private var context: RuntimeContext? = null
    private var kycStep: String? = null
    private var started = false
    private var policy = RuntimePolicy()
    private var lastTriggerAtEpochSeconds: Long = 0
    private val passiveTracker = PassiveTracker()
    private var backendClient: BackendClient = NoOpBackendClient()
    private val uiBridge = AndroidUiBridge()
    private var lifecycleAttached = false
    private var reducedModeReported = false
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var passiveEvalTask: ScheduledFuture<*>? = null
    private var statusListener: ((SdkStatus) -> Unit)? = null

    fun initialize(context: RuntimeContext, policy: RuntimePolicy) {
        this.context = context
        this.policy = policy
        this.backendClient = HttpBackendClient(baseUrl = policy.backendBaseUrl)
        this.started = true
        startPassiveEvaluationLoop()
    }

    fun setStatusListener(listener: (SdkStatus) -> Unit) {
        statusListener = listener
    }

    fun setUser(id: String, phone: String?) {
        if (!started) return
        backendClient.setUser(id = id, phone = phone)
    }

    fun setKycStep(step: String) {
        if (!started) return
        if (policy.kycStepStrategy == KycStepStrategy.INFER_ONLY) return
        kycStep = step
    }

    fun onScreenObserved(screen: String) {
        if (!started) return
        if (screen.isBlank()) return
        passiveTracker.onScreenChanged(screen)
    }

    fun onUserInteractionObserved() {
        if (!started) return
        passiveTracker.onInteraction()
    }

    fun trackError(code: String, properties: Map<String, Any?>) {
        if (!started) return
        val userContext = context ?: return
        reportReducedTrackingModeIfNeeded()
        val passiveSnapshot = passiveTracker.snapshot()
        backendClient.trackErrorEvent(
            userId = userContext.userId,
            sessionId = userContext.sessionId,
            screen = kycStep ?: passiveSnapshot.currentScreen,
            code = code,
        )
        if (!policy.triggerSettings.autoTriggerEnabled) return
        val errorSignals = if (policy.triggerSettings.includeErrorSignals) listOf(code) else emptyList()
        val screenSignal = if (policy.triggerSettings.includeStepHints) {
            kycStep ?: passiveSnapshot.currentScreen
        } else {
            null
        }
        val timeSpentSignal = if (policy.triggerSettings.includeTimeSpentSignals) {
            passiveSnapshot.timeSpentSeconds
        } else {
            null
        }
        val idleSignal = if (policy.triggerSettings.includeIdleSignals) {
            passiveSnapshot.idleSeconds
        } else {
            null
        }
        backendClient.evaluateTrigger(
            userId = userContext.userId,
            sessionId = userContext.sessionId,
            signals = TriggerSignals(
                screen = screenSignal,
                timeSpent = timeSpentSignal,
                errors = errorSignals,
                idleSeconds = idleSignal,
            ),
            onResult = { decision ->
                if (decision.trigger && decision.action == "start_voice_agent") {
                    onTriggerDecision(decision)
                }
            }
        )
        if (properties.isNotEmpty()) {
            // Placeholder for richer signal mapping.
        }
    }

    private fun onTriggerDecision(decision: TriggerDecision) {
        when (policy.triggerStartMode) {
            TriggerStartMode.IMMEDIATE -> startAssistantSession()
            TriggerStartMode.COOLDOWN -> {
                val now = Instant.now().epochSecond
                if (now - lastTriggerAtEpochSeconds >= policy.minTriggerIntervalSeconds) {
                    lastTriggerAtEpochSeconds = now
                    startAssistantSession()
                }
            }
            TriggerStartMode.CONFIRM_UI -> {
                val approved = showSdkOverlayPrompt(decision.reason)
                if (approved) startAssistantSession()
            }
        }
    }

    private fun showSdkOverlayPrompt(reason: String): Boolean {
        if (reason.isNotEmpty()) {
            // Keep reason available for upcoming SDK overlay copy.
        }
        return uiBridge.confirm(policy.confirmUiText)
    }

    fun startAssistantSession() {
        if (!started) return
        val userContext = context ?: return
        backendClient.startAssistant(
            userId = userContext.userId,
            sessionId = userContext.sessionId,
            screen = kycStep,
        )
    }

    fun stopAssistantSession() {
        if (!started) return
        val userContext = context ?: return
        backendClient.stopAssistant(
            userId = userContext.userId,
            sessionId = userContext.sessionId,
        )
    }

    fun bindCurrentActivity(activity: android.app.Activity) {
        if (!started) return
        uiBridge.updateCurrentActivity(activity)
        if (!lifecycleAttached) {
            lifecycleAttached = true
            statusListener?.invoke(
                SdkStatus(
                    code = SdkStatusCode.LIFECYCLE_ATTACHED,
                    message = "Lifecycle tracking attached successfully.",
                )
            )
        }
    }

    private fun startPassiveEvaluationLoop() {
        passiveEvalTask?.cancel(false)
        if (!policy.passiveEvalEnabled) return
        if (policy.passiveEvalIntervalSeconds <= 0) return
        passiveEvalTask = scheduler.scheduleAtFixedRate(
            {
                evaluatePassiveSignals()
            },
            policy.passiveEvalIntervalSeconds,
            policy.passiveEvalIntervalSeconds,
            TimeUnit.SECONDS,
        )
    }

    private fun evaluatePassiveSignals() {
        if (!started) return
        if (!policy.triggerSettings.autoTriggerEnabled) return
        if (!policy.triggerSettings.includeTimeSpentSignals && !policy.triggerSettings.includeIdleSignals) return
        val userContext = context ?: return
        val snapshot = passiveTracker.snapshot()
        val screenSignal = if (policy.triggerSettings.includeStepHints) {
            kycStep ?: snapshot.currentScreen
        } else {
            null
        }
        val timeSpentSignal = if (policy.triggerSettings.includeTimeSpentSignals) snapshot.timeSpentSeconds else null
        val idleSignal = if (policy.triggerSettings.includeIdleSignals) snapshot.idleSeconds else null
        if (screenSignal == null && timeSpentSignal == null && idleSignal == null) return

        backendClient.evaluateTrigger(
            userId = userContext.userId,
            sessionId = userContext.sessionId,
            signals = TriggerSignals(
                screen = screenSignal,
                timeSpent = timeSpentSignal,
                errors = emptyList(),
                idleSeconds = idleSignal,
            ),
            onResult = { decision ->
                if (decision.trigger && decision.action == "start_voice_agent") {
                    onTriggerDecision(decision)
                }
            },
        )
    }

    private fun reportReducedTrackingModeIfNeeded() {
        if (lifecycleAttached || reducedModeReported) return
        reducedModeReported = true
        statusListener?.invoke(
            SdkStatus(
                code = SdkStatusCode.REDUCED_TRACKING_MODE,
                message = "AI.attach(application) not called. Running in reduced auto-tracking mode.",
            )
        )
    }
}
