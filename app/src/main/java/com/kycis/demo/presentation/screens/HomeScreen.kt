package com.kycis.demo.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class ActivityItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

val kycActivities = listOf(
    ActivityItem(
        id = "kyc",
        title = "Full KYC Flow",
        description = "Complete identity verification with PAN, Aadhaar & selfie",
        icon = Icons.Default.VerifiedUser,
        route = "kyc_flow"
    ),
    ActivityItem(
        id = "mutual_fund",
        title = "Mutual Fund KYC",
        description = "Complete KYC for mutual fund investment",
        icon = Icons.Default.TrendingUp,
        route = "mutual_fund_kyc"
    ),
    ActivityItem(
        id = "pan",
        title = "PAN Verification",
        description = "Verify PAN card details and upload document",
        icon = Icons.Default.Badge,
        route = "pan_verification"
    ),
    ActivityItem(
        id = "aadhaar",
        title = "Aadhaar Verification",
        description = "Aadhaar number verification with OTP",
        icon = Icons.Default.AssignmentInd,
        route = "aadhaar_verification"
    ),
    ActivityItem(
        id = "selfie",
        title = "Selfie Capture",
        description = "Capture selfie for biometric verification",
        icon = Icons.Default.Face,
        route = "selfie_capture"
    ),
    ActivityItem(
        id = "document",
        title = "Document Upload",
        description = "Upload any document for verification",
        icon = Icons.Default.Description,
        route = "document_upload"
    ),
    ActivityItem(
        id = "video",
        title = "Video KYC",
        description = "Video-based identity verification",
        icon = Icons.Default.Videocam,
        route = "video_kyc"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onActivityClick: (ActivityItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "KYCis Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select an activity to get started",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(kycActivities) { activity ->
                ActivityCard(
                    activity = activity,
                    onClick = { onActivityClick(activity) }
                )
            }
        }
    }
}

@Composable
fun ActivityCard(
    activity: ActivityItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = activity.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}