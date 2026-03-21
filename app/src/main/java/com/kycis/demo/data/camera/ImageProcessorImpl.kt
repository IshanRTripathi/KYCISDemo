package com.kycis.demo.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.kycis.demo.domain.models.ImageData
import com.kycis.demo.domain.models.ImageFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ImageProcessor that handles image validation and compression.
 */
@Singleton
class ImageProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageProcessor {

    companion object {
        const val DEFAULT_MAX_SIZE_KB = 5 * 1024 // 5MB
        const val DEFAULT_COMPRESSION_THRESHOLD_KB = 2 * 1024 // 2MB
        const val JPEG_QUALITY = 85
    }

    override suspend fun validateImage(image: ImageData): ValidationResult = withContext(Dispatchers.IO) {
        // Validate format
        val validFormat = when (image.format) {
            ImageFormat.JPEG, ImageFormat.PNG -> true
        }
        
        if (!validFormat) {
            return@withContext ValidationResult.Invalid(
                "Unsupported image format. Please use JPEG or PNG."
            )
        }

        // Validate size (max 5MB by default)
        val maxSizeBytes = DEFAULT_MAX_SIZE_KB.toLong() * 1024
        if (image.sizeBytes > maxSizeBytes) {
            val sizeMB = image.sizeBytes / (1024.0 * 1024.0)
            return@withContext ValidationResult.Invalid(
                "Image size (${String.format("%.2f", sizeMB)}MB) exceeds maximum allowed size (5MB)."
            )
        }

        // Validate image integrity by attempting to decode
        try {
            val uri = Uri.parse(image.uri)
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext ValidationResult.Invalid(
                    "Invalid image file. The image could not be decoded."
                )
            }
        } catch (e: Exception) {
            return@withContext ValidationResult.Invalid(
                "Failed to validate image: ${e.message}"
            )
        }

        ValidationResult.Valid
    }

    override suspend fun compressImage(image: ImageData, maxSizeKB: Int): ImageData = withContext(Dispatchers.IO) {
        val currentSizeKB = image.sizeBytes / 1024
        
        // If already within size limit, return original
        if (currentSizeKB <= maxSizeKB) {
            return@withContext image
        }

        try {
            val uri = Uri.parse(image.uri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext image

            // Decode bitmap
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
            }
            var bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (bitmap == null) {
                return@withContext image
            }

            // Compress iteratively until size is acceptable
            var quality = JPEG_QUALITY
            val outputStream = ByteArrayOutputStream()
            
            // Determine format and compress
            val compressFormat = when (image.format) {
                ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            }

            bitmap.compress(compressFormat, quality, outputStream)
            var compressedBytes = outputStream.toByteArray()
            var compressedSizeKB = compressedBytes.size / 1024

            // Iteratively reduce quality if still too large
            while (compressedSizeKB > maxSizeKB && quality > 10) {
                quality -= 10
                outputStream.reset()
                bitmap.compress(compressFormat, quality, outputStream)
                compressedBytes = outputStream.toByteArray()
                compressedSizeKB = compressedBytes.size / 1024
            }

            // If still too large, reduce resolution
            while (compressedSizeKB > maxSizeKB && options.inSampleSize < 8) {
                options.inSampleSize *= 2
                val newInputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(newInputStream, null, options)
                newInputStream?.close()

                if (bitmap == null) {
                    break
                }

                outputStream.reset()
                bitmap.compress(compressFormat, quality, outputStream)
                compressedBytes = outputStream.toByteArray()
                compressedSizeKB = compressedBytes.size / 1024
            }

            // Create new ImageData with compressed size
            val compressedImage = image.copy(
                sizeBytes = compressedBytes.size.toLong()
            )

            // Clean up
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }

            compressedImage
        } catch (e: Exception) {
            // Return original if compression fails
            image
        }
    }

    /**
     * Validates image size without compression.
     */
    fun validateImageSize(image: ImageData, maxSizeKB: Int = DEFAULT_MAX_SIZE_KB): Boolean {
        return image.sizeBytes <= maxSizeKB.toLong() * 1024
    }

    /**
     * Checks if image should be compressed based on threshold.
     */
    fun shouldCompress(image: ImageData, thresholdKB: Int = DEFAULT_COMPRESSION_THRESHOLD_KB): Boolean {
        return image.sizeBytes > thresholdKB.toLong() * 1024
    }
}