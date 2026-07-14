package com.kycis.demo.kycis

import android.app.Activity
import android.app.Application
import android.util.Log
import com.kycis.demo.BackendUrlStore
import com.kycis.demo.BuildConfig
import com.kycis.sdk.AI
import com.kycis.sdk.VoiceSessionResult
import com.kycis.sdk.VoiceUiSnapshot
import com.kycis.sdk.core.ConfirmUiText
import com.kycis.sdk.core.KycStepStrategy
import com.kycis.sdk.core.RuntimePolicy
import com.kycis.sdk.core.TriggerSettings
import com.kycis.sdk.core.TriggerStartMode

/**
 * Isolated near-zero KYCIS glue for the demo app.
 * Host files call this object only — do not scatter raw [AI] usage in screens.
 */
object KycisIntegration {
    private const val TAG = "KYCIS"
    private const val API_KEY = "demo-api-key"
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

    fun demoApiKey(): String = API_KEY
    fun demoUserId(): String = DEFAULT_USER_ID

    /**
     * Critical order: registerScreenSchemas → AI.init → AI.attach → registerWorkflowModel.
     */
    fun init(application: Application) {
        Log.d(TAG, "KycisIntegration.init: registering schemas")
        AI.registerScreenSchemas(KycisScreenSchemas.all)

        AI.init(
            application = application,
            apiKey = API_KEY,
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
        AI.reportComponentInput(
            componentId = componentId,
            hint = hint,
            screen = screen,
            componentType = componentType,
            sdkKb = sdkKb?.toMap(),
            properties = properties,
        )
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
