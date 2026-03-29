package com.kycis.demo.presentation.screens.legacy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kycis.demo.presentation.components.KycButton
import com.kycis.demo.presentation.components.OtpInputField
import com.kycis.demo.presentation.components.LoadingIndicator
import com.kycis.demo.presentation.state.OtpState

@Composable
fun LegacyOtpVerificationScreen(
    state: OtpState,
    onDigitChange: (Int, String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isVerifying) {
        LoadingIndicator()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OTP Verification",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the 6-digit OTP sent to your registered mobile",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OtpInputField(
            digits = state.digits,
            onDigitChange = onDigitChange,
            onComplete = onVerify,
            enabled = !state.isVerifying,
            error = state.otpError
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Timer
        if (!state.canResend) {
            Text(
                text = "Resend OTP in ${state.timeRemaining}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TextButton(
                onClick = onResend,
                enabled = state.canResend
            ) {
                Text("Resend OTP")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        KycButton(
            text = "Verify OTP",
            onClick = onVerify,
            enabled = state.digits.all { it.isNotEmpty() },
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

