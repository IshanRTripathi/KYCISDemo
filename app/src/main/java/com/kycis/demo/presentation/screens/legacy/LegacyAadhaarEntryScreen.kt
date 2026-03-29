package com.kycis.demo.presentation.screens.legacy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kycis.demo.presentation.components.KycButton
import com.kycis.demo.presentation.components.KycTextField
import com.kycis.demo.presentation.components.LoadingIndicator
import com.kycis.demo.presentation.state.AadhaarState

@Composable
fun LegacyAadhaarEntryScreen(
    state: AadhaarState,
    isLoading: Boolean,
    onAadhaarChanged: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        LoadingIndicator()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Aadhaar Details",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your Aadhaar number",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        KycTextField(
            value = state.aadhaarNumber,
            onValueChange = onAadhaarChanged,
            label = "Aadhaar Number",
            error = state.aadhaarError,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Format: XXXX XXXX XXXX (12 digits)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        KycButton(
            text = "Continue",
            onClick = onContinue,
            enabled = state.aadhaarNumber.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
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

