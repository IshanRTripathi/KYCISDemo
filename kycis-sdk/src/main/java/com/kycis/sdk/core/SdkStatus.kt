package com.kycis.sdk.core

enum class SdkStatusCode {
    REDUCED_TRACKING_MODE,
    LIFECYCLE_ATTACHED,
    ERROR,
}

data class SdkStatus(
    val code: SdkStatusCode,
    val message: String,
)
