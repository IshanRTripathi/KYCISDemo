package com.kycis.demo.kycis

/**
 * Maps Navigation Compose routes to KYCIS step ids (must match [com.kycis.sdk.core.ScreenSchema.screenId]).
 * Parameterized routes such as `phone_otp/{phone}` must not be sent verbatim.
 */
object KycisStepMap {

    fun normalize(route: String?): String {
        if (route.isNullOrBlank()) return ""
        return when {
            route.startsWith("phone_otp") -> "phone_otp"
            route.startsWith("email_otp") -> "email_otp"
            else -> route
        }
    }
}
