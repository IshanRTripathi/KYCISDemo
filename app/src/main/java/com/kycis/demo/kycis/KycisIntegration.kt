package com.kycis.demo.kycis

import android.app.Activity
import android.app.Application
import android.util.Log
import com.kycis.demo.BackendUrlStore
import com.kycis.demo.BuildConfig
import com.kycis.demo.VoiceUiSnapshotHolder
import com.kycis.sdk.AI
import com.kycis.sdk.AsyncCheckStatus
import com.kycis.sdk.VoiceSessionResult
import com.kycis.sdk.VoiceUiSnapshot
import com.kycis.sdk.core.AgentEventListener
import com.kycis.sdk.core.ConfirmUiText
import com.kycis.sdk.core.KycStepStrategy
import com.kycis.sdk.core.RuntimePolicy
import com.kycis.sdk.core.TriggerSettings
import com.kycis.sdk.core.TriggerStartMode
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Isolated near-zero KYCIS glue for the demo app.
 * Host files call this object only — do not scatter raw [AI] usage in screens.
 */
object KycisIntegration {
    private const val TAG = "KYCIS"
    private const val FALLBACK_API_KEY = "demo-api-key"
    private const val DEFAULT_USER_ID = "demo-user"

    data class ComponentKb(
        val displayName: String? = null,
        val validations: List<String> = emptyList(),
        val commonIssues: List<String> = emptyList(),
        val faqs: List<String> = emptyList(),
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "display_name" to displayName,
            "validations" to validations.ifEmpty { null },
            "common_issues" to commonIssues.ifEmpty { null },
            "faqs" to faqs.ifEmpty { null },
        ).filterValues { it != null }
    }

    data class RemoteCheckResult(
        val passed: Boolean,
        val message: String,
    )

    /** Demo RuntimePolicy shared with MainActivity [com.kycis.sdk.ui.KycisOptions]. */
    fun demoPolicy(application: Application): RuntimePolicy {
        val backendBaseUrl = BackendUrlStore.get(application)
        return RuntimePolicy(
            backendBaseUrl = backendBaseUrl,
            clientId = "kycis_demo",
            mappingVersion = "v1",
            appVersion = BuildConfig.VERSION_NAME,
            triggerStartMode = TriggerStartMode.CONFIRM_UI,
            kycStepStrategy = KycStepStrategy.HINT_THEN_INFER,
            triggerSettings = TriggerSettings(
                autoTriggerEnabled = true,
                includeErrorSignals = true,
                includeTimeSpentSignals = true,
                includeIdleSignals = true,
                includeStepHints = true,
            ),
            confirmUiText = ConfirmUiText(
                title = "Need help completing this step?",
                startCta = "Start",
                dismissCta = "Not now",
            ),
            passiveEvalEnabled = true,
            passiveEvalIntervalSeconds = 10,
            componentInputHintsMasked = false,
            autoCaptureEnabled = false,
            debugEnabled = true,
        )
    }

    fun demoApiKey(): String {
        val fromBuild = BuildConfig.KYCIS_API_KEY.trim()
        return fromBuild.ifBlank { FALLBACK_API_KEY }
    }

    fun demoUserId(): String = DEFAULT_USER_ID

    /**
     * Critical order: registerScreenSchemas → AI.init → AI.attach → registerWorkflowModel.
     */
    fun init(application: Application) {
        Log.d(TAG, "KycisIntegration.init: registering schemas")
        AI.registerScreenSchemas(KycisScreenSchemas.all)

        val apiKey = demoApiKey()
        if (apiKey == FALLBACK_API_KEY) {
            Log.w(
                TAG,
                "Using fallback API key. Set kycis.api.key in local.properties to match backend KYCIS_API_KEY.",
            )
        }

        AI.init(
            application = application,
            apiKey = apiKey,
            userId = DEFAULT_USER_ID,
            policy = demoPolicy(application),
        )
        AI.attach(application)
        AI.registerWorkflowModel(KycisWorkflow.model)
        Log.d(TAG, "KycisIntegration.init: complete (workflow registered)")
    }

    fun bindActivity(activity: Activity) {
        AI.bindActivity(activity)
    }

    fun refreshVoiceAudioFocus() {
        AI.refreshVoiceAudioFocus()
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ): Boolean = AI.onRequestPermissionsResult(requestCode, permissions, grantResults)

    fun setStep(step: String) {
        if (step.isBlank()) return
        AI.setKycStep(step)
    }

    fun setFlow(flowName: String?) {
        AI.setFlow(flowName)
    }

    fun setUser(userId: String, phone: String? = null, phoneMasked: Boolean = true) {
        AI.setUser(id = userId, phone = phone, phoneMasked = phoneMasked)
    }

    fun onValidationFailed(
        code: String,
        componentId: String? = null,
        componentType: String? = null,
        expectedPattern: String? = null,
        validationRuleId: String? = null,
        hint: String? = null,
        businessStep: String? = null,
    ) {
        AI.trackValidationFailure(
            failureReasonCode = code,
            componentId = componentId,
            componentType = componentType,
            expectedPattern = expectedPattern,
            validationRuleId = validationRuleId,
            hint = hint,
            hintMasked = true,
            businessStep = businessStep,
        )
    }

    fun onValidationFailedDetailed(
        failureReasonCode: String,
        componentId: String,
        expectedPattern: String? = null,
        validationRuleId: String? = null,
        hint: String? = null,
        componentType: String? = null,
        businessStep: String? = null,
        validationIntent: String? = null,
        recoveryPlaybookId: String? = null,
    ) {
        AI.trackValidationFailure(
            failureReasonCode = failureReasonCode,
            componentId = componentId,
            componentType = componentType,
            expectedPattern = expectedPattern,
            validationRuleId = validationRuleId,
            hint = hint,
            hintMasked = true,
            businessStep = businessStep,
            validationIntent = validationIntent,
            recoveryPlaybookId = recoveryPlaybookId,
        )
    }

    fun reportComponentInput(
        componentId: String,
        hint: String,
        screen: String? = null,
        componentType: String? = null,
        sdkKb: ComponentKb? = null,
        properties: Map<String, String> = emptyMap(),
    ) {
        // Keep voice UI snapshot in sync for every report (all screens, not just personal_details).
        if (!screen.isNullOrBlank()) {
            VoiceUiSnapshotHolder.setCurrentScreen(screen)
        }
        VoiceUiSnapshotHolder.upsertField(componentId, hint, componentType)
        AI.reportComponentInput(
            componentId = componentId,
            hint = hint,
            screen = screen,
            componentType = componentType,
            sdkKb = sdkKb?.toMap(),
            properties = properties,
        )
    }

    /**
     * Demo-only tenant API adapter for exercising Tier-3 remote validation.
     *
     * A real tenant would replace [delay] with its own repository/API call. Keeping this
     * orchestration inside the isolated integration package leaves the feature screen with
     * one call and mirrors the intended third-party integration shape.
     *
     * `AAAAA0000A` deterministically simulates a tenant-backend rejection; every other
     * locally valid PAN succeeds after three seconds.
     */
    suspend fun simulateTenantPanVerification(pan: String): RemoteCheckResult {
        val checkId = UUID.randomUUID().toString()
        AI.reportAsyncCheck(
            componentId = "pan_field",
            status = AsyncCheckStatus.STARTED,
            message = "Checking PAN with the tenant verification service",
            checkId = checkId,
        )

        // Simulated tenant-owned network request. Do not block the Android main thread.
        delay(3_000)

        val passed = !pan.equals("AAAAA0000A", ignoreCase = true)
        val message = if (passed) {
            "PAN verified successfully by the tenant service"
        } else {
            "The tenant service could not verify this PAN. Please check it and try again."
        }
        AI.reportAsyncCheck(
            componentId = "pan_field",
            status = if (passed) AsyncCheckStatus.PASSED else AsyncCheckStatus.FAILED,
            message = message,
            checkId = checkId,
        )
        return RemoteCheckResult(passed = passed, message = message)
    }

    fun trackStepStarted(stepId: String) {
        trackAnalytics("kyc_step_started", mapOf("step" to stepId))
    }

    fun trackStepCompleted(stepId: String, nextStep: String? = null) {
        trackAnalytics(
            "kyc_step_completed",
            mapOf("step" to stepId, "next_step" to nextStep),
        )
    }

    fun trackOtpResend(channel: String) {
        trackAnalytics("otp_resend_requested", mapOf("channel" to channel))
    }

    fun setAgentEventListener(listener: AgentEventListener?) {
        AI.setAgentEventListener(listener)
    }

    fun trackAnalytics(eventName: String, data: Map<String, Any?> = emptyMap()) {
        AI.trackAnalytics(eventName = eventName, data = data)
    }

    fun trackError(code: String, properties: Map<String, Any?> = emptyMap()) {
        AI.trackError(code = code, properties = properties)
    }

    fun completeKycFlow(data: Map<String, Any?> = emptyMap()) {
        AI.completeKycFlow(data)
    }

    fun startAssistant() {
        AI.startAssistant()
    }

    fun stopAssistant() {
        AI.stopAssistant()
    }

    suspend fun checkForDynamicPopup() {
        AI.checkForDynamicPopup()
    }

    fun setVoiceSessionListener(listener: ((VoiceSessionResult) -> Unit)?) {
        AI.setVoiceSessionListener(listener)
    }

    fun setVoiceUiSnapshotProvider(provider: (() -> VoiceUiSnapshot?)?) {
        AI.setVoiceUiSnapshotProvider(provider)
    }

    fun isFeatureEnabled(featureName: String): Boolean = AI.isFeatureEnabled(featureName)

    fun setOnPopupCheck(providerStateSet: (suspend () -> Unit) -> Unit) {
        providerStateSet { checkForDynamicPopup() }
    }
}
