package com.kycis.demo

import android.app.Application
import android.util.Log
import com.kycis.sdk.AI
import com.kycis.sdk.core.ConfirmUiText
import com.kycis.sdk.core.KycStepStrategy
import com.kycis.sdk.core.RuntimePolicy
import com.kycis.sdk.core.TriggerSettings
import com.kycis.sdk.core.TriggerStartMode
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KycDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKycSdk()
    }

    private fun initKycSdk() {
        AI.setStatusListener { status ->
            Log.d("KYCIS", "SDK status: ${status.code} - ${status.message}")
        }
        AI.init(
            apiKey = "demo-api-key",
            userId = "demo-user",
            policy = RuntimePolicy(
                backendBaseUrl = "http://10.0.2.2:8000/v1",
                triggerStartMode = TriggerStartMode.CONFIRM_UI,
                kycStepStrategy = KycStepStrategy.HINT_THEN_INFER,
                triggerSettings = TriggerSettings(
                    autoTriggerEnabled = true,
                    includeErrorSignals = true,
                    includeTimeSpentSignals = false,
                    includeIdleSignals = false,
                    includeStepHints = true,
                ),
                confirmUiText = ConfirmUiText(
                    title = "Need help completing this step?",
                    startCta = "Start",
                    dismissCta = "Not now",
                ),
                passiveEvalEnabled = true,
                passiveEvalIntervalSeconds = 10,
            ),
        )
        AI.attach(this)
    }
}