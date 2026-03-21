package com.kycis.demo.data.camera

import com.kycis.demo.domain.models.ImageData

/**
 * Interface for managing camera and gallery operations for document capture.
 */
interface CameraManager {
    /**
     * Captures an image using the device camera.
     * @return Result containing ImageData on success or an error on failure.
     */
    suspend fun captureImage(): Result<ImageData>

    /**
     * Selects an image from the device gallery.
     * @return Result containing ImageData on success or an error on failure.
     */
    suspend fun selectFromGallery(): Result<ImageData>

    /**
     * Requests camera permission.
     * @return PermissionResult indicating whether permission was granted, denied, or permanently denied.
     */
    suspend fun requestCameraPermission(): PermissionResult

    /**
     * Requests storage permission for gallery access.
     * @return PermissionResult indicating whether permission was granted, denied, or permanently denied.
     */
    suspend fun requestStoragePermission(): PermissionResult
}

/**
 * Result of a permission request.
 */
sealed class PermissionResult {
    /** Permission was granted. */
    data object Granted : PermissionResult()

    /** Permission was denied but can be requested again. */
    data object Denied : PermissionResult()

    /** Permission was permanently denied and user needs to enable it in settings. */
    data object PermanentlyDenied : PermissionResult()
}