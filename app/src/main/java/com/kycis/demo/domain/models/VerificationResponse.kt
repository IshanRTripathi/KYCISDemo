package com.kycis.demo.domain.models

data class VerificationResponse(
    val isValid: Boolean,
    val message: String? = null
)