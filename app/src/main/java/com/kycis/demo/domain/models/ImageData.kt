package com.kycis.demo.domain.models

data class ImageData(
    val uri: String,
    val sizeBytes: Long,
    val format: ImageFormat
)