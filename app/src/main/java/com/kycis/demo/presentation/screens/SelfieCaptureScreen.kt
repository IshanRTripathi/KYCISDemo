package com.kycis.demo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kycis.demo.presentation.components.KycButton
import com.kycis.demo.presentation.components.UploadCard
import com.kycis.demo.presentation.state.UploadState

@Composable
fun SelfieCaptureScreen(
    state: UploadState,
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRetryClick: () -> Unit,
    onContinueClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            onCaptureClick = onCaptureClick,
            onGalleryClick = onGalleryClick,
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