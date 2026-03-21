package com.kycis.demo.domain.models

data class Document(
    val type: DocumentType,
    val imageData: ImageData,
    val uploadedAt: Long
)