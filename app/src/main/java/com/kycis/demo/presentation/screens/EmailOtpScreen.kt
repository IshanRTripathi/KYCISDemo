package com.kycis.demo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.sdk.ui.HintKind
import com.kycis.demo.kycis.KycisIntegration
import com.kycis.sdk.ui.maskHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailOtpScreen(
    email: String,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onResendProvider: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var otpValue by remember { mutableStateOf("") }
    val otpLength = 4
    var resendCooldownSeconds by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds <= 0) return@LaunchedEffect
        kotlinx.coroutines.delay(1000)
        resendCooldownSeconds -= 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Please check email id",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We've sent a code to $email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            LaunchedEffect(otpValue) {
                if (otpValue.length != otpLength) return@LaunchedEffect
                KycisIntegration.reportComponentInput(
                    componentId = "email_otp_field",
                    hint = maskHint(HintKind.OTP, otpValue),
                    screen = "email_otp",
                    componentType = "otp_input",
                    properties = mapOf("digits_filled" to otpLength.toString()),
                    sdkKb = KycisIntegration.ComponentKb(
                        displayName = "Email OTP",
                        validations = listOf("Must be exactly 4 digits"),
                        commonIssues = listOf("User enters wrong OTP", "OTP expired", "Check spam folder")
                    )
                )
            }

            // Custom OTP digits row
            BasicTextField(
                value = otpValue,
                onValueChange = {
                    if (it.length <= otpLength) {
                        otpValue = it
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 0 until otpLength) {
                            val char = otpValue.getOrNull(i)?.toString() ?: ""
                            val isFocused = otpValue.length == i
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't get a code? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        if (resendCooldownSeconds > 0) return@TextButton
                        onResendProvider()
                        resendCooldownSeconds = 30
                        android.widget.Toast.makeText(
                            context,
                            "OTP resent (demo). Check email / use any 4 digits.",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    },
                    enabled = resendCooldownSeconds == 0,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (resendCooldownSeconds > 0) {
                            "Resend in ${resendCooldownSeconds}s"
                        } else {
                            "Click to resend."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (resendCooldownSeconds > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    if (otpValue.length == otpLength) {
                        onVerify(otpValue) 
                    } else {
                        KycisIntegration.onValidationFailed(
                            code = "otp_incomplete",
                            componentId = "email_otp_field",
                            componentType = "otp_input",
                            hint = otpValue,
                            businessStep = "email_otp"
                        )
                    }
                },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Verify",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmailOtpScreenPreview() {
    KycDemoTheme {
        EmailOtpScreen(
            email = "ishan@zynnex.in",
            onBack = {},
            onVerify = {},
            onResendProvider = {},
            onSkip = {},
        )
    }
}
