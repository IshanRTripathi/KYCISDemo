package com.kycis.demo.domain.error

sealed class ErrorState {
    data class ValidationError(val message: String) : ErrorState()
    data class NetworkError(val message: String) : ErrorState()
    data class UploadError(val message: String) : ErrorState()
    data class OtpError(val message: String) : ErrorState()
}