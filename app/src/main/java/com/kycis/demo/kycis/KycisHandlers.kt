package com.kycis.demo.kycis

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.kycis.sdk.VoiceSessionResult
import com.kycis.sdk.core.DynamicPopup

/**
 * Popup / voice / identity callbacks used by MainActivity demo shell.
 * Keeps raw SDK listener wiring out of feature screens.
 */
object KycisHandlers {
    private const val TAG = "KYCIS"

    fun onPopup(context: Context, popup: DynamicPopup) {
        if (!popup.show) return
        Log.d(
            TAG,
            "Dynamic popup: message=${popup.message} popup_reason_code=${popup.popupReasonCode} delayMs=${popup.delayMs}",
        )
        val suffix = popup.popupReasonCode?.let { "\n[$it]" } ?: ""
        Toast.makeText(context, popup.message + suffix, Toast.LENGTH_LONG).show()
    }

    fun onStatusChange(statusCode: String) {
        Log.d(TAG, "Status changed: $statusCode")
    }

    /** Called when useKycis reports ready — identity + default flow. */
    fun onSdkReady() {
        KycisIntegration.setUser(userId = "demo-user")
        KycisIntegration.setFlow("onboarding")
    }

    fun logVoiceConnectFailed(context: Context, message: String) {
        Log.e(TAG, "Voice LiveKit connect failed: $message")
        Toast.makeText(context, "Voice connection failed. $message", Toast.LENGTH_LONG).show()
    }

    fun isValidVoiceSession(result: VoiceSessionResult): Boolean = result.isValid
}
