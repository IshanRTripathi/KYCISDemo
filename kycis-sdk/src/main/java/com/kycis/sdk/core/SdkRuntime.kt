package com.kycis.sdk.core

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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

private const val ACTION_START_VOICE_AGENT = "start_voice_agent"

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
    private var voiceSessionListener: ((com.kycis.sdk.VoiceSessionResult) -> Unit)? = null
    private var pendingVoiceProceed: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

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

    fun setVoiceSessionListener(listener: ((com.kycis.sdk.VoiceSessionResult) -> Unit)?) {
        voiceSessionListener = listener
    }

    fun setUser(id: String, phone: String?) {
        if (!started) return
        context = context?.copy(userId = id)
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
        val signals = buildTriggerSignals(
            passiveSnapshot = passiveSnapshot,
            errors = listOf(code),
            includeErrors = policy.triggerSettings.includeErrorSignals,
        )
        backendClient.evaluateTrigger(
            userId = userContext.userId,
            sessionId = userContext.sessionId,
            signals = signals,
            onResult = { decision ->
                if (decision.trigger && decision.action == ACTION_START_VOICE_AGENT) {
                    mainHandler.post { onTriggerDecision(decision) }
                }
            },
        )
    }

    private fun onTriggerDecision(decision: TriggerDecision) {
        when (policy.triggerStartMode) {
            TriggerStartMode.IMMEDIATE -> startAssistantSession()
            TriggerStartMode.COOLDOWN -> {
                val now = System.currentTimeMillis() / 1000
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
        val activity = uiBridge.getCurrentActivity()
        val hasPermission = activity != null &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        fun proceedWithBackend() {
            backendClient.startAssistant(
                userId = userContext.userId,
                sessionId = userContext.sessionId,
                screen = kycStep,
            ) { result ->
                mainHandler.post {
                    if (result != null && result.isValid) {
                        voiceSessionListener?.invoke(result)
                    }
                }
            }
        }

        when {
            hasPermission -> proceedWithBackend()
            activity is FragmentActivity -> {
                val fragmentActivity = activity
                if (fragmentActivity.supportFragmentManager.findFragmentByTag(PermissionRequestFragment.TAG) != null) {
                    statusListener?.invoke(SdkStatus(code = SdkStatusCode.ERROR, message = "Permission request already in progress."))
                    return
                }
                VoicePermissionCallbackHolder.callback = { granted ->
                    if (granted) proceedWithBackend()
                    else {
                        statusListener?.invoke(
                            SdkStatus(
                                code = SdkStatusCode.ERROR,
                                message = "RECORD_AUDIO permission denied. Voice session requires microphone access.",
                            ),
                        )
                    }
                }
                fragmentActivity.runOnUiThread {
                    fragmentActivity.supportFragmentManager.beginTransaction()
                        .add(PermissionRequestFragment(), PermissionRequestFragment.TAG)
                        .commit()
                }
            }
            activity != null -> {
                pendingVoiceProceed = { proceedWithBackend() }
                activity.runOnUiThread {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_CODE_RECORD_AUDIO,
                    )
                }
            }
            else -> {
                statusListener?.invoke(
                    SdkStatus(
                        code = SdkStatusCode.ERROR,
                        message = "No Activity for RECORD_AUDIO permission. Call AI.attach(application) and ensure an Activity is visible.",
                    ),
                )
            }
        }
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode != REQUEST_CODE_RECORD_AUDIO) return false
        pendingVoiceProceed?.let { proceed ->
            pendingVoiceProceed = null
            val idx = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
            val granted = idx >= 0 && grantResults.getOrElse(idx) { PackageManager.PERMISSION_DENIED } == PackageManager.PERMISSION_GRANTED
            if (granted) proceed()
            else {
                statusListener?.invoke(
                    SdkStatus(
                        code = SdkStatusCode.ERROR,
                        message = "RECORD_AUDIO permission denied. Voice session requires microphone access.",
                    ),
                )
            }
            return true
        }
        return false
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
        val signals = buildTriggerSignals(
            passiveSnapshot = snapshot,
            errors = emptyList(),
            includeErrors = false,
        )
        if (signals.screen == null && signals.timeSpent == null && signals.idleSeconds == null) return

        backendClient.evaluateTrigger(
            userId = userContext.userId,
            sessionId = userContext.sessionId,
            signals = signals,
            onResult = { decision ->
                if (decision.trigger && decision.action == ACTION_START_VOICE_AGENT) {
                    mainHandler.post { onTriggerDecision(decision) }
                }
            },
        )
    }

    private fun buildTriggerSignals(
        passiveSnapshot: PassiveSnapshot,
        errors: List<String>,
        includeErrors: Boolean,
    ): TriggerSignals {
        val errorSignals = if (includeErrors) errors else emptyList()
        val screenSignal = if (policy.triggerSettings.includeStepHints) {
            kycStep ?: passiveSnapshot.currentScreen
        } else null
        val timeSpentSignal = if (policy.triggerSettings.includeTimeSpentSignals) passiveSnapshot.timeSpentSeconds else null
        val idleSignal = if (policy.triggerSettings.includeIdleSignals) passiveSnapshot.idleSeconds else null
        return TriggerSignals(
            screen = screenSignal,
            timeSpent = timeSpentSignal,
            errors = errorSignals,
            idleSeconds = idleSignal,
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
