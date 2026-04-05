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
import com.kycis.sdk.AI
import kotlinx.coroutines.launch
import android.util.Log

private const val TAG = "KYCIS"

/**
 * QA / integrator harness for backend-assisted UX (Phase 5 roadmap).
 *
 * - **Popup path:** `validation_failed` on the server session → `checkForDynamicPopup()`.
 * - **Trigger path:** `trackError` / `trackValidationFailure` may call `trigger/evaluate`
 *   (see Logcat); with default [com.kycis.sdk.core.TriggerStartMode.CONFIRM_UI] the user
 *   gets a confirm step before voice starts.
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
        AI.setKycStep("sdk_diagnostics")
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
                text = "1) Send validation failure so the server stores last_validation_failure.\n" +
                    "2) Check popup — should call POST /assistant/popup/evaluate.\n" +
                    "3) Generic error — may run trigger/evaluate (confirm UI before voice).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    AI.trackValidationFailure(
                        failureReasonCode = "harness_invalid_field",
                        componentId = "harness_field",
                        componentType = "text_input",
                        hintMasked = true,
                    )
                    Toast.makeText(context, "Sent validation_failed to backend", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Harness: trackValidationFailure(harness_invalid_field)")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Send validation failure", fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = {
                    scope.launch {
                        runCatching { AI.checkForDynamicPopup() }
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
                    AI.trackError(
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
