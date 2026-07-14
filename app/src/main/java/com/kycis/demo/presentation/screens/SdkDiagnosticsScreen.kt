package com.kycis.demo.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kycis.demo.kycis.KycisIntegration
import kotlinx.coroutines.launch
import android.util.Log

private const val TAG = "KYCIS"

/**
 * QA / integrator harness for backend-assisted UX (Phase 5 roadmap).
 *
 * - **Popup path:** validation_failed → checkForDynamicPopup → popup/evaluate
 * - **Trigger path:** trackError / validation may call trigger/evaluate
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdkDiagnosticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        KycisIntegration.setStep("sdk_diagnostics")
        Log.d(TAG, "SdkDiagnosticsScreen: kyc step set to sdk_diagnostics")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("SDK ↔ backend harness") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Requires KYCIS backend (e.g. emulator: http://10.0.2.2:8000). Filter Logcat by \"$TAG\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "1) Send validation failure → server stores last_validation_failure.\n" +
                    "2) Check popup → POST /v1/assistant/popup/evaluate (see popup_reason_code in Logcat).\n" +
                    "3) Generic error → may run POST /v1/assistant/trigger/evaluate (confirm UI before voice).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    KycisIntegration.onValidationFailed(
                        code = "harness_invalid_field",
                        componentId = "harness_field",
                        componentType = "text_input",
                    )
                    Toast.makeText(context, "Sent validation_failed to backend", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Harness: onValidationFailed(harness_invalid_field)")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Send validation failure", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    scope.launch {
                        runCatching { KycisIntegration.checkForDynamicPopup() }
                            .onSuccess {
                                Toast.makeText(context, "popup/evaluate completed (see Logcat / popup UI)", Toast.LENGTH_SHORT).show()
                                Log.i(TAG, "Harness: checkForDynamicPopup() finished")
                            }
                            .onFailure { e ->
                                Toast.makeText(context, "Popup check failed: ${e.message}", Toast.LENGTH_LONG).show()
                                Log.e(TAG, "Harness: checkForDynamicPopup failed", e)
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Check dynamic popup", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    KycisIntegration.trackError(
                        "harness_demo_error",
                        mapOf("source" to "sdk_diagnostics_screen"),
                    )
                    Toast.makeText(context, "Sent error_reported; may trigger evaluate", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Harness: trackError(harness_demo_error)")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Track generic error (trigger path)", fontWeight = FontWeight.Medium)
            }
        }
    }
}
