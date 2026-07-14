package com.kycis.demo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.demo.kycis.KycisIntegration
import kotlinx.coroutines.delay

@Composable
fun SelfieCaptureScreen(
    onCaptured: () -> Unit,
    modifier: Modifier = Modifier
) {
    var instruction by remember { mutableStateOf("Turn your head to the right") }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(1000)
        progress = 0.3f
        delay(1000)
        instruction = "Blink your eyes"
        progress = 0.6f
        delay(1000)
        instruction = "Smile for the camera"
        progress = 1.0f
        delay(500)
        
        // Track selfie capture
        KycisIntegration.reportComponentInput(
            componentId = "selfie_capture",
            hint = "selfie_captured.jpg",
            screen = "selfie_capture",
            componentType = "camera_capture",
            sdkKb = KycisIntegration.ComponentKb(
                displayName = "Selfie Capture",
                validations = listOf("Face must be clearly visible", "Sufficient lighting required", "No sunglasses or hats"),
                commonIssues = listOf("Room is too dark", "Face is partially outside the frame", "Multiple faces detected")
            )
        )
        
        onCaptured()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2C)) // Dark background from mockup
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Place your face inside the frame & follow instructions below",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Circular Frame
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(Color.Transparent, CircleShape)
                .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Mock Viewfinder
            Box(
                modifier = Modifier
                    .fillMaxSize(0.95f)
                    .background(Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera Feed Placeholder", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = instruction,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analyzing...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.width(12.dp))
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
    }
}

@Preview
@Composable
fun SelfieCaptureScreenPreview() {
    KycDemoTheme {
        SelfieCaptureScreen(onCaptured = {})
    }
}

