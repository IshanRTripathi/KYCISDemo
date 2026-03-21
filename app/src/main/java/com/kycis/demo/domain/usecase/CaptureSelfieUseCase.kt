package com.kycis.demo.domain.usecase

import com.kycis.demo.data.camera.ImageProcessor
import com.kycis.demo.domain.models.ImageData
import com.kycis.demo.domain.models.UploadResponse
import com.kycis.demo.domain.repository.KycRepository
import javax.inject.Inject

class CaptureSelfieUseCase @Inject constructor(
    private val kycRepository: KycRepository,
    private val imageProcessor: ImageProcessor
) {
    suspend operator fun invoke(selfie: ImageData): Result<UploadResponse> {
        // Validate image first
        val validationResult = imageProcessor.validateImage(selfie)
        if (validationResult is com.kycis.demo.data.camera.ValidationResult.Invalid) {
            return Result.failure(Exception(validationResult.errorMessage))
        }

        // Compress if needed
        val processedImage = imageProcessor.compressImage(selfie, 2048) // 2MB

        // Upload through repository
        return kycRepository.uploadSelfie(processedImage)
    }
}