package com.kycis.demo.presentation.state

import com.kycis.demo.domain.models.ImageData

data class UploadState(
    val imageData: ImageData? = null,
    val isUploading: Boolean = false,
    val uploadError: String? = null,
    val uploadSuccess: Boolean = false
)