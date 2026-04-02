package com.kycis.demo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.sdk.ui.KycEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanDetailsScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    var panNumber by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var agreedToTerms by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "PAN Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
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
                TextButton(onClick = onNext) {
                    Text("Skip", color = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enter PAN number",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            val panRegex = "[A-Z]{5}[0-9]{4}[A-Z]".toRegex()
            val isPanValid = panNumber.matches(panRegex)
            val isDobValid = dob.isNotEmpty()
            val canProceed = isPanValid && isDobValid && agreedToTerms

            var debouncedPan by remember { mutableStateOf("") }
            LaunchedEffect(panNumber) {
                kotlinx.coroutines.delay(600)
                debouncedPan = panNumber
            }
            LaunchedEffect(debouncedPan) {
                if (debouncedPan.isNotBlank()) {
                    val filtered: String = debouncedPan.filter { c -> c.isLetterOrDigit() }
                    if (filtered.isNotEmpty()) {
                        // Send unmasked value so the agent can see what the user actually typed
                        KycEvent.componentInput(
                            componentId = "pan_field",
                            hint = filtered.uppercase(),  // Send unmasked, normalized value
                            screen = "pan_details",
                            componentType = "pan",
                            sdkKb = KycEvent.ComponentKb(
                                displayName = "PAN Number",
                                validations = listOf(
                                    "Must be exactly 10 characters",
                                    "Format: 5 letters, 4 digits, 1 letter",
                                    "Example: ABCDE1234F",
                                    "All uppercase, no spaces"
                                ),
                                commonIssues = listOf(
                                    "User enters lowercase letters",
                                    "User adds spaces or dashes",
                                    "Confusion between O and 0, I and 1"
                                ),
                                faqs = listOf(
                                    "Find your PAN on the front of your PAN card",
                                    "First 3 letters indicate IT department",
                                    "4th letter is P for person",
                                    "Last letter is a checksum"
                                )
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = panNumber,
                onValueChange = { panNumber = it.uppercase() },
                placeholder = { Text("ABCDE1234F") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = panNumber.isNotEmpty() && !isPanValid,
                supportingText = {
                    if (panNumber.isNotEmpty() && !isPanValid) {
                        Text("Enter a valid 10-character PAN")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enter DOB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                placeholder = { Text("DD/MM/YYYY") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PAN Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val imageLoader = coil.ImageLoader.Builder(context)
                    .components {
                        if (android.os.Build.VERSION.SDK_INT >= 28) {
                            add(coil.decode.ImageDecoderDecoder.Factory())
                        } else {
                            add(coil.decode.GifDecoder.Factory())
                        }
                    }
                    .build()

                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data("file:///android_asset/PanCard.gif")
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it }
                )
                Text(
                    text = "I agree with Zynnex T&C and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00308F),
                    disabledContainerColor = Color(0xFF00308F).copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Next",
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
fun PanDetailsScreenPreview() {
    KycDemoTheme {
        PanDetailsScreen(onBack = {}, onNext = {})
    }
}

