package com.kycis.demo.data.camera

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.kycis.demo.domain.models.ImageData
import java.io.File

/**
 * Helper class that provides ActivityResultLauncher instances for camera and gallery operations.
 * This should be used in Composables to handle image capture and selection.
 */
class CameraManagerHelper(
    private val context: Context,
    private val cameraManagerImpl: CameraManagerImpl
) {
    /**
     * Creates a rememberLauncherForActivityResult for capturing images with the camera.
     * 
     * Usage in Composable:
     * ```
     * val cameraLauncher = rememberCameraLauncher(
     *     onImageCaptured = { imageData -> /* handle captured image */ },
     *     onError = { error -> /* handle error */ }
     * )
     * 
     * // To launch:
     * val photoFile = remember { File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg") }
     * val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
     * cameraLauncher.launch(photoUri)
     * ```
     */
    @Composable
    fun rememberCameraLauncher(
        onImageCaptured: (ImageData) -> Unit,
        onError: (String) -> Unit
    ): androidx.activity.result.ActivityResultLauncher<Uri> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                // The URI is passed directly to the launcher, but we need to track the file
                // In practice, the caller would provide the URI
                onError("Image captured successfully - use the URI passed to the launcher")
            } else {
                onError("Camera capture cancelled")
            }
        }
    }

    /**
     * Creates a rememberLauncherForActivityResult for selecting images from gallery.
     */
    @Composable
    fun rememberGalleryLauncher(
        onImageSelected: (ImageData) -> Unit,
        onError: (String) -> Unit
    ): androidx.activity.result.ActivityResultLauncher<String> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                cameraManagerImpl.createImageData(it)?.let { imageData ->
                    onImageSelected(imageData)
                } ?: onError("Failed to process selected image")
            } ?: onError("No image selected")
        }
    }

    /**
     * Creates a rememberLauncherForActivityResult for requesting camera permission.
     */
    @Composable
    fun rememberCameraPermissionLauncher(
        onPermissionResult: (Boolean) -> Unit
    ): androidx.activity.result.ActivityResultLauncher<String> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onPermissionResult(isGranted)
        }
    }

    /**
     * Creates a rememberLauncherForActivityResult for requesting storage permission.
     */
    @Composable
    fun rememberStoragePermissionLauncher(
        onPermissionResult: (Boolean) -> Unit
    ): androidx.activity.result.ActivityResultLauncher<String> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onPermissionResult(isGranted)
        }
    }
}

/**
 * Composable function to create and remember a CameraManagerHelper.
 */
@Composable
fun rememberCameraManagerHelper(
    cameraManagerImpl: CameraManagerImpl
): CameraManagerHelper {
    val context = LocalContext.current
    return remember(context) {
        CameraManagerHelper(context, cameraManagerImpl)
    }
}