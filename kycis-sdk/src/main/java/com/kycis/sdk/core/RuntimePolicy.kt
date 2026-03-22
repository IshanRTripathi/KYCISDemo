package com.kycis.sdk.core

enum class TriggerStartMode {
    IMMEDIATE,
    COOLDOWN,
    CONFIRM_UI,
}

enum class KycStepStrategy {
    HINT_THEN_INFER,
    INFER_ONLY,
}

data class RuntimePolicy(
    val triggerStartMode: TriggerStartMode = TriggerStartMode.CONFIRM_UI,
    val kycStepStrategy: KycStepStrategy = KycStepStrategy.HINT_THEN_INFER,
    val minTriggerIntervalSeconds: Long = 60,
    val triggerSettings: TriggerSettings = TriggerSettings(),
    val backendBaseUrl: String = "http://localhost:8000/v1",
    val confirmUiText: ConfirmUiText = ConfirmUiText(),
    val passiveEvalEnabled: Boolean = true,
    val passiveEvalIntervalSeconds: Long = 10,
)

data class TriggerSettings(
    val autoTriggerEnabled: Boolean = true,
    val includeErrorSignals: Boolean = true,
    val includeTimeSpentSignals: Boolean = false,
    val includeIdleSignals: Boolean = false,
    val includeStepHints: Boolean = true,
)

data class ConfirmUiText(
    val title: String = "Need help completing this step?",
    val startCta: String = "Start",
    val dismissCta: String = "Not now",
)
