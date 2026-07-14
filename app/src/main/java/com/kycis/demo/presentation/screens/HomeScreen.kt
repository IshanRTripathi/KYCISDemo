package com.kycis.demo.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kycis.demo.kycis.KycisIntegration
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.demo.presentation.theme.ThemePrimaryLight
import com.kycis.demo.presentation.theme.ThemePrimary

@Composable
fun HomeScreen(
    onStartFlow: () -> Unit,
    onOpenSdkHarness: () -> Unit = {},
    onOpenBackendSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedFlow by remember { mutableStateOf("kyc") }

    fun applyFlowSelection(flowKey: String) {
        selectedFlow = flowKey
        val sdkFlow = when (flowKey) {
            "mfd" -> "mfd_support"
            else -> "onboarding"
        }
        KycisIntegration.setFlow(sdkFlow)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Zynnex Demo",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Welcome to Zynnex demo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // KYC Flow Card
        FlowSelectionCard(
            title = "KYC Flow",
            subtitle = "Help increase KYC Conversion",
            icon = {
                Icon(
                    imageVector = Icons.Default.Hexagon,
                    contentDescription = null,
                    tint = if (selectedFlow == "kyc") ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            },
            isSelected = selectedFlow == "kyc",
            onClick = { applyFlowSelection("kyc") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // MFD Support Card
        FlowSelectionCard(
            title = "MFD Support",
            subtitle = "Helping MFD to create orders through call",
            icon = {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = if (selectedFlow == "mfd") ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            },
            isSelected = selectedFlow == "mfd",
            onClick = { applyFlowSelection("mfd") }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Start Button
        Button(
            onClick = {
                android.util.Log.d("KYCIS", "HomeScreen: Start button clicked")
                applyFlowSelection(selectedFlow)
                onStartFlow()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = "Start",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        TextButton(
            onClick = onOpenSdkHarness,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "SDK ↔ backend harness (popup + trigger)",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        TextButton(
            onClick = onOpenBackendSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Backend settings (URL + health test)",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FlowSelectionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) ThemePrimaryLight else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) ThemePrimary else MaterialTheme.colorScheme.outlineVariant

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon space
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = ThemePrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Circle,
                    contentDescription = "Unselected",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KycDemoTheme {
        HomeScreen(onStartFlow = {}, onOpenSdkHarness = {}, onOpenBackendSettings = {})
    }
}

