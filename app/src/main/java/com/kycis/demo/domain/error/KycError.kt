package com.kycis.demo.domain.error

sealed class KycError {
    data class ValidationFailed(val message: String) : KycError()
    data class NetworkFailed(val message: String) : KycError()
    data class UploadFailed(val message: String) : KycError()
    data class OtpFailed(val message: String) : KycError()
    data class Unknown(val message: String) : KycError()
}