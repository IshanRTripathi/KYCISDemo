package com.kycis.demo.domain.models

data class ValidationResponse(
    val isValid: Boolean,
    val message: String? = null
)