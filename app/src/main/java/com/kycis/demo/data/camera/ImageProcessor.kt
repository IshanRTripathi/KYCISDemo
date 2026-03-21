package com.kycis.demo.data.camera

import com.kycis.demo.domain.models.ImageData

/**
 * Interface for processing and validating images.
 */
interface ImageProcessor {
    /**
     * Validates an image for format and size requirements.
     * @param image The ImageData to validate.
     * @return ValidationResult indicating whether the image is valid or invalid with an error message.
     */
    suspend fun validateImage(image: ImageData): ValidationResult

    /**
     * Compresses an image if it exceeds the specified maximum size.
     * @param image The ImageData to compress.
     * @param maxSizeKB The maximum allowed size in kilobytes.
     * @return The compressed ImageData if compression was needed, or the original if within limits.
     */
    suspend fun compressImage(image: ImageData, maxSizeKB: Int): ImageData
}

/**
 * Validation result for image processing.
 */
sealed class ValidationResult {
    /** Image is valid. */
    data object Valid : ValidationResult()

    /** Image is invalid with an error message. */
    data class Invalid(val errorMessage: String) : ValidationResult()
}