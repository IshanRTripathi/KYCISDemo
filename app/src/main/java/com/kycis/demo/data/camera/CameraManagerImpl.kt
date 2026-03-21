package com.kycis.demo.data.camera

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.kycis.demo.domain.models.ImageData
import com.kycis.demo.domain.models.ImageFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementation of CameraManager using ActivityResultContracts.
 * Provides camera capture and gallery selection functionality.
 * 
 * Note: For UI integration, use [CameraManagerHelper] which provides
 * ActivityResultLauncher instances that can be used in Composables.
 */
@Singleton
class CameraManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraManager {

    // Default configuration values
    companion object {
        const val DEFAULT_MAX_SIZE_KB = 5 * 1024 // 5MB
        const val DEFAULT_COMPRESSION_THRESHOLD_KB = 2 * 1024 // 2MB
    }

    override suspend fun captureImage(): Result<ImageData> = withContext(Dispatchers.Main) {
        // This method requires UI integration - use CameraManagerHelper in Composables
        // The actual capture is handled through ActivityResultLauncher in the UI layer
        Result.failure(
            IllegalStateException(
                "captureImage() requires UI integration. Use CameraManagerHelper in Composables."
            )
        )
    }

    override suspend fun selectFromGallery(): Result<ImageData> = withContext(Dispatchers.Main) {
        // This method requires UI integration - use CameraManagerHelper in Composables
        // The actual selection is handled through ActivityResultLauncher in the UI layer
        Result.failure(
            IllegalStateException(
                "selectFromGallery() requires UI integration. Use CameraManagerHelper in Composables."
            )
        )
    }

    override suspend fun requestCameraPermission(): PermissionResult {
        // Permission handling is done through Accompanist in Composable context
        // This method provides the enum value for logic handling
        return PermissionResult.Denied
    }

    override suspend fun requestStoragePermission(): PermissionResult {
        // Permission handling is done through Accompanist in Composable context
        // This method provides the enum value for logic handling
        return PermissionResult.Denied
    }

    /**
     * Creates ImageData from a URI.
     * This is a utility method that can be used after image capture/selection.
     */
    fun createImageData(uri: Uri): ImageData? {
        return try {
            val contentResolver = context.contentResolver
            val sizeBytes = getFileSize(contentResolver, uri)
            val format = getImageFormat(contentResolver, uri)
            
            if (format != null) {
                ImageData(
                    uri = uri.toString(),
                    sizeBytes = sizeBytes,
                    format = format
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates ImageData from a file path.
     */
    fun createImageDataFromFile(file: File): ImageData? {
        return try {
            val format = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> ImageFormat.JPEG
                "png" -> ImageFormat.PNG
                else -> null
            }
            
            if (format != null) {
                ImageData(
                    uri = Uri.fromFile(file).toString(),
                    sizeBytes = file.length(),
                    format = format
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeIndex)
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getImageFormat(contentResolver: ContentResolver, uri: Uri): ImageFormat? {
        return try {
            val mimeType = contentResolver.getType(uri)
            when (mimeType) {
                "image/jpeg", "image/jpg" -> ImageFormat.JPEG
                "image/png" -> ImageFormat.PNG
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}