package com.kycis.sdk.core

enum class SdkStatusCode {
    REDUCED_TRACKING_MODE,
    LIFECYCLE_ATTACHED,
}

data class SdkStatus(
    val code: SdkStatusCode,
    val message: String,
)
