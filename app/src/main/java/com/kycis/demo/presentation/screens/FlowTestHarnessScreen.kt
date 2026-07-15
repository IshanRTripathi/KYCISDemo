package com.kycis.demo.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kycis.demo.harness.CheckStatus
import com.kycis.demo.harness.FlowStepResult
import com.kycis.demo.harness.FlowTestRunner
import kotlinx.coroutines.launch

private const val TAG = "KYCIS"

/**
 * Multi-tenant-capable flow E2E harness: walks every screen in [com.kycis.demo.kycis.KycisWorkflow]
 * against the live backend using its own disposable session — does not touch the real app session.
 * Swap [com.kycis.demo.kycis.KycisScreenSchemas] / [com.kycis.demo.kycis.KycisWorkflow] for a
 * different tenant's app and this same screen tests that tenant's flow unmodified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowTestHarnessScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val runner = remember { FlowTestRunner(context) }
    val report by runner.report.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Flow E2E harness") },
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
                text = "Walks every screen in KycisWorkflow against the live backend using its own " +
                    "disposable session (client_id=kycis_demo_harness) — does not touch the real app session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { scope.launch { runner.run() } },
                enabled = !report.running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (report.running) "Running… (${report.currentScreenId ?: ""})" else "Run full flow test",
                    fontWeight = FontWeight.Medium,
                )
            }

            if (report.steps.isNotEmpty() || report.fatalError != null) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("flow_test_report", report.toClipboardText()))
                        Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                        android.util.Log.i(TAG, "FlowTestHarness report:\n${report.toClipboardText()}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Copy report (also logged to Logcat)")
                }
            }

            report.fatalError?.let { err ->
                Text(
                    text = "Fatal: $err",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(report.steps) { step ->
                    FlowStepRow(step)
                }
            }
        }
    }
}

@Composable
private fun FlowStepRow(step: FlowStepResult) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIcon(step.status)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${step.screenId}${step.displayName?.let { " – $it" } ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(start = 28.dp, top = 4.dp)) {
                step.componentResults.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusIcon(c.status, size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${c.componentId}: ${c.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                step.notes.forEach { note ->
                    Text(
                        text = "note: $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: CheckStatus, size: Dp = 20.dp) {
    val icon = when (status) {
        CheckStatus.PASS -> Icons.Filled.CheckCircle
        CheckStatus.FAIL -> Icons.Filled.Error
        CheckStatus.SKIPPED -> Icons.Filled.RemoveCircleOutline
    }
    val tint = when (status) {
        CheckStatus.PASS -> Color(0xFF2E7D32)
        CheckStatus.FAIL -> MaterialTheme.colorScheme.error
        CheckStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(icon, contentDescription = status.name, tint = tint, modifier = Modifier.size(size))
}
