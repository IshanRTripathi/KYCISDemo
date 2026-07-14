package com.kycis.demo.kycis

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.kycis.sdk.VoiceSessionResult
import com.kycis.sdk.core.AgentEvent
import com.kycis.sdk.core.AgentEventType
import com.kycis.sdk.core.DynamicPopup

/**
 * Popup / voice / identity callbacks used by MainActivity demo shell.
 * Keeps raw SDK listener wiring out of feature screens.
 */
object KycisHandlers {
    private const val TAG = "KYCIS"

    fun onStatusChange(statusCode: String) {
        Log.d(TAG, "Status changed: $statusCode")
    }

    /** Called when useKycis reports ready — identity + default flow. */
    fun onSdkReady() {
        KycisIntegration.setUser(userId = "demo-user")
        KycisIntegration.setFlow("onboarding")
        KycisIntegration.setAgentEventListener { event -> onAgentEvent(event) }
    }

    fun onSdkTeardown() {
        KycisIntegration.setAgentEventListener(null)
    }

    fun onAgentEvent(event: AgentEvent) {
        when (event.type) {
            AgentEventType.CONNECTED -> {
                KycisIntegration.trackAnalytics(
                    "voice_session_started",
                    mapOf("source" to "agent_event"),
                )
            }
            AgentEventType.DISCONNECTED -> {
                KycisIntegration.trackAnalytics(
                    "voice_session_ended",
                    mapOf(
                        "source" to "agent_event",
                        "call_duration_ms" to event.metadata.callDuration,
                    ),
                )
            }
            AgentEventType.POPUP_VISIBLE -> {
                KycisIntegration.trackAnalytics(
                    "popup_displayed",
                    mapOf(
                        "source" to "agent_event",
                        "popup_reason_code" to event.metadata.popupReasonCode,
                        "message" to event.metadata.message,
                    ),
                )
            }
            AgentEventType.TRANSCRIPTION_RECEIVED -> Unit
        }
    }

    fun logPopupShown(popup: DynamicPopup) {
        Log.d(
            TAG,
            "Dynamic popup: message=${popup.message} popup_reason_code=${popup.popupReasonCode} delayMs=${popup.delayMs}",
        )
        KycisIntegration.trackAnalytics(
            "popup_shown",
            mapOf(
                "popup_reason_code" to popup.popupReasonCode,
                "message" to popup.message,
                "delay_ms" to popup.delayMs,
            ),
        )
    }

    fun onPopupAccepted(popup: DynamicPopup) {
        KycisIntegration.trackAnalytics(
            "popup_accepted",
            mapOf("popup_reason_code" to popup.popupReasonCode),
        )
        KycisIntegration.startAssistant()
    }

    fun onPopupDismissed(popup: DynamicPopup) {
        KycisIntegration.trackAnalytics(
            "popup_dismissed",
            mapOf("popup_reason_code" to popup.popupReasonCode),
        )
    }

    fun logVoiceConnectFailed(context: Context, message: String) {
        Log.e(TAG, "Voice LiveKit connect failed: $message")
        KycisIntegration.trackError(
            "voice_connect_failed",
            mapOf("message" to message),
        )
        Toast.makeText(context, "Voice connection failed. $message", Toast.LENGTH_LONG).show()
    }

    fun isValidVoiceSession(result: VoiceSessionResult): Boolean = result.isValid
}
