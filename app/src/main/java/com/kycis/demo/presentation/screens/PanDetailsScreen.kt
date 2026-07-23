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
import com.kycis.demo.kycis.KycisIntegration
import com.kycis.demo.kycis.KycisTrackInput
import kotlinx.coroutines.launch

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
    var isRemoteCheckRunning by remember { mutableStateOf(false) }
    var verifiedPan by remember { mutableStateOf<String?>(null) }
    var remoteCheckMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    KycisTrackInput(
        value = panNumber.filter { it.isLetterOrDigit() }.uppercase(),
        componentId = "pan_field",
        screen = "pan_details",
        componentType = "pan",
    )
    KycisTrackInput(dob, "dob_field", "pan_details", "date")
    KycisTrackInput(
        value = if (agreedToTerms) "accepted" else "",
        componentId = "terms_checkbox",
        screen = "pan_details",
        componentType = "checkbox",
    )

    LaunchedEffect(panNumber) {
        if (verifiedPan != null && verifiedPan != panNumber) {
            verifiedPan = null
            remoteCheckMessage = null
        }
    }

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

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isRemoteCheckRunning -> "Tenant API: verifying PAN (3-second simulation)…"
                    verifiedPan == panNumber && verifiedPan != null ->
                        remoteCheckMessage ?: "PAN verified. Tap Next to continue."
                    remoteCheckMessage != null -> remoteCheckMessage.orEmpty()
                    else -> "Demo: AAAAA0000A simulates rejection; any other valid PAN passes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isRemoteCheckRunning -> MaterialTheme.colorScheme.primary
                    verifiedPan == panNumber && verifiedPan != null -> MaterialTheme.colorScheme.primary
                    remoteCheckMessage != null -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
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
                    onCheckedChange = { agreedToTerms = it },
                )
                Text(
                    text = "I agree with Zynnex T&C and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (canProceed) {
                        if (verifiedPan == panNumber) {
                            onNext()
                        } else if (!isRemoteCheckRunning) {
                            coroutineScope.launch {
                                isRemoteCheckRunning = true
                                remoteCheckMessage = null
                                val result = KycisIntegration.simulateTenantPanVerification(panNumber)
                                isRemoteCheckRunning = false
                                remoteCheckMessage = result.message
                                if (result.passed) {
                                    verifiedPan = panNumber
                                }
                            }
                        }
                    } else {
                        if (panNumber.isEmpty()) {
                            KycisIntegration.onValidationFailed("pan_required", "pan_field", "pan", businessStep = "pan_details")
                        } else if (!isPanValid) {
                            KycisIntegration.onValidationFailed(
                                code = "pan_invalid_format",
                                componentId = "pan_field",
                                componentType = "pan",
                                hint = panNumber,
                                expectedPattern = "[A-Z]{5}[0-9]{4}[A-Z]",
                                businessStep = "pan_details"
                            )
                        } else if (dob.isEmpty()) {
                            KycisIntegration.onValidationFailed("dob_required", "dob_field", "date", businessStep = "pan_details")
                        } else if (!agreedToTerms) {
                            KycisIntegration.onValidationFailed("terms_not_accepted", "terms_checkbox", "checkbox", businessStep = "pan_details")
                        }
                    }
                },
                enabled = !isRemoteCheckRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                if (isRemoteCheckRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        isRemoteCheckRunning -> "Verifying…"
                        verifiedPan == panNumber && verifiedPan != null -> "Next"
                        else -> "Verify & Continue"
                    },
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

