package com.kycis.demo.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kycis.demo.R
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.demo.kycis.KycisIntegration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEntryScreen(
    onBack: () -> Unit,
    onGetOtp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }

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
                TextButton(onClick = { onGetOtp("9999999999") }) {
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
                text = "Please enter your Phone Number",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your phone number",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            val isPhoneValid = phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }

            // Auto-capture using SDK's KycEvent with debounce (reduced for faster voice response)
            var debouncedPhone by remember { mutableStateOf("") }
            LaunchedEffect(phoneNumber) {
                kotlinx.coroutines.delay(200)  // Reduced from 600ms for faster voice response
                debouncedPhone = phoneNumber
            }
            LaunchedEffect(debouncedPhone) {
                if (debouncedPhone.isNotEmpty()) {
                    // Send unmasked value so the agent can see what the user actually typed
                    // The masked=false flag tells the backend this is unmasked
                    KycisIntegration.reportComponentInput(
                        componentId = "phone_field",
                        hint = debouncedPhone,  // Send unmasked value
                        screen = "phone_entry",
                        componentType = "phone_number",
                        sdkKb = KycisIntegration.ComponentKb(
                            displayName = "Phone Number",
                            validations = listOf(
                                "Must be exactly 10 digits",
                                "Must start with 6, 7, 8, or 9",
                                "Do not include country code (+91 or 0)"
                            ),
                            commonIssues = listOf(
                                "User adds +91 or 0 prefix",
                                "User enters 11 digits",
                                "User enters letters or special characters"
                            ),
                            faqs = listOf(
                                "Enter 10-digit mobile number without +91",
                                "Example: 9876543210",
                                "Do not use spaces or dashes"
                            )
                        )
                    )
                }
            }

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    if (it.length <= 10) {
                        phoneNumber = it
                    }
                },
                label = { Text("10-digit mobile number") },
                placeholder = { Text("Phone no.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = phoneNumber.isNotEmpty() && phoneNumber.length < 10,
                supportingText = {
                    if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                        Text("Enter a valid 10-digit number")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.phoneinput2),
                contentDescription = "Phone number illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "By Proceeding, you agree with Zynnex terms and conditions",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (isPhoneValid) {
                        onGetOtp(phoneNumber)
                    } else {
                        KycisIntegration.onValidationFailed(
                            code = "phone_invalid_format",
                            componentId = "phone_field",
                            componentType = "phone_number",
                            hint = phoneNumber,
                            businessStep = "phone_entry"
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
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Get OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Trusted By 1.5Cr+ Indians",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PhoneEntryScreenPreview() {
    KycDemoTheme {
        PhoneEntryScreen(onBack = {}, onGetOtp = {})
    }
}
