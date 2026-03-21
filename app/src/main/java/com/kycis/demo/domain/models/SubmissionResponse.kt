package com.kycis.demo.domain.models

data class SubmissionResponse(
    val success: Boolean,
    val message: String,
    val sessionId: String? = null
)