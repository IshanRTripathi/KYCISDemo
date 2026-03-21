package com.kycis.demo.domain.models

data class UploadResponse(
    val success: Boolean,
    val documentId: String? = null,
    val message: String? = null
)