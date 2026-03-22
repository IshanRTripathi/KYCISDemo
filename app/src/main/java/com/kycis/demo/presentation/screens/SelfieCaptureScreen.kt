package com.kycis.demo.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kycis.demo.data.camera.CameraManagerImpl
import com.kycis.demo.domain.models.ImageData
import com.kycis.demo.presentation.components.KycButton
import com.kycis.demo.presentation.components.UploadCard
import com.kycis.demo.presentation.state.UploadState
import java.io.File

@Composable
fun SelfieCaptureScreen(
    state: UploadState,
    onImageCaptured: (ImageData) -> Unit,
    onRetryClick: () -> Unit,
    onContinueClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cameraManager: CameraManagerImpl? = null
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            cameraManager?.createImageData(photoUri!!)?.let { imageData ->
                onImageCaptured(imageData)
            }
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            cameraManager?.createImageData(it)?.let { imageData ->
                onImageCaptured(imageData)
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Selfie Verification",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Take a selfie for biometric verification",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        UploadCard(
            imageData = state.imageData,
            isUploading = state.isUploading,
            error = state.uploadError,
            onCaptureClick = {
                // Create temp file for camera
                val photoFile = File(context.cacheDir, "selfie_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                photoUri = uri
                cameraLauncher.launch(uri)
            },
            onGalleryClick = {
                galleryLauncher.launch("image/*")
            },
            onRetryClick = onRetryClick,
            onContinueClick = onContinueClick,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}