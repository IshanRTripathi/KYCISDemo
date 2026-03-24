package com.kycis.demo.presentation.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kycis.sdk.AI
import com.kycis.sdk.InvokeSources
import com.kycis.sdk.voice.VoiceDebugState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// KYCIS SDK does not provide DebugLog; use stub for compatibility
private data class DebugEventStub(val kind: String, val message: String, val timeFormatted: String, val details: Map<String, Any>)

private const val BACKEND_BASE = "http://10.0.2.2:8000"

private fun HttpURLConnection.applyKycisTraceHeaders(invokeSource: String) {
    try {
        AI.traceHeadersForRequest(invokeSource).forEach { (key, value) ->
            setRequestProperty(key, value)
        }
    } catch (_: IllegalStateException) {
        // AI.init not called — skip tracing
    } catch (_: IllegalArgumentException) {
        // blank invokeSource
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdkDebugScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var backendHealth by remember { mutableStateOf<ConnectionStatus>(ConnectionStatus.Loading) }
    var backendActivity by remember { mutableStateOf<List<BackendActivityItem>>(emptyList()) }
    var activityLoading by remember { mutableStateOf(false) }
    var sdkLog by remember { mutableStateOf<List<DebugEventStub>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }

    fun refresh() {
        refreshKey++
    }

    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            try {
                val conn = URL("$BACKEND_BASE/health").openConnection() as HttpURLConnection
                conn.applyKycisTraceHeaders(InvokeSources.HOST_DEBUG_HEALTH)
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val code = conn.responseCode
                backendHealth = if (code in 200..299) ConnectionStatus.Ok else ConnectionStatus.Error("HTTP $code")
            } catch (e: Exception) {
                backendHealth = ConnectionStatus.Error(e.message ?: "Connection failed")
            }
        }
    }

    LaunchedEffect(refreshKey) {
        activityLoading = true
        withContext(Dispatchers.IO) {
            try {
                val conn = URL("$BACKEND_BASE/api/activity").openConnection() as HttpURLConnection
                conn.applyKycisTraceHeaders(InvokeSources.HOST_DEBUG_ACTIVITY)
                conn.connectTimeout = 3000
                conn.readTimeout = 5000
                val text = if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().readText() else "[]"
                val json = org.json.JSONObject(text)
                val items = json.optJSONArray("items") ?: org.json.JSONArray()
                backendActivity = (0 until items.length()).map { i ->
                    val o = items.getJSONObject(i)
                    BackendActivityItem(
                        kind = o.optString("kind", "?"),
                        sessionId = o.optString("session_id", ""),
                        timestamp = o.optLong("timestamp", 0),
                        payload = o.optJSONObject("payload")?.toString() ?: "{}",
                    )
                }.reversed()
            } catch (e: Exception) {
                backendActivity = emptyList()
            }
            activityLoading = false
        }
    }

    LaunchedEffect(refreshKey) {
        // KYCIS SDK does not expose DebugLog; keep empty
        sdkLog = emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SDK Debug") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = "SDK ↔ Backend Connection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (backendHealth) {
                        is ConnectionStatus.Ok -> Color(0xFF166534).copy(alpha = 0.2f)
                        is ConnectionStatus.Error -> Color(0xFF991B1B).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (val s = backendHealth) {
                        is ConnectionStatus.Ok -> {
                            Text("●", color = Color(0xFF22C55E), modifier = Modifier.padding(end = 8.dp))
                            Text("Backend: Connected", color = Color(0xFF22C55E))
                        }
                        is ConnectionStatus.Error -> {
                            Text("●", color = Color(0xFFEF4444), modifier = Modifier.padding(end = 8.dp))
                            Text("Backend: ${s.message}", color = Color(0xFFEF4444))
                        }
                        ConnectionStatus.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.padding(8.dp))
                            Text("Checking...")
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = BACKEND_BASE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        AI.trackError("debug_test")
                        refresh()
                    },
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Test Event")
                }
                OutlinedButton(onClick = { refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
                OutlinedButton(onClick = {
                    VoiceDebugState.clear()
                    refresh()
                }) {
                    Text("Clear Log")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Backend Activity (last received)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (activityLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (backendActivity.isEmpty()) {
                Text(
                    text = "No activity yet. Navigate KYC screens or tap 'Send Test Event'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                backendActivity.take(15).forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = item.kind,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(item.timestamp * 1000),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                text = item.sessionId.take(12) + "...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                            if (item.payload.length < 200) {
                                Text(text = item.payload, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "Voice / LiveKit (during call)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            run {
                val voice = VoiceDebugState.getState()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (voice.connectionState) {
                            "connected" -> Color(0xFF166534).copy(alpha = 0.15f)
                            "connecting" -> Color(0xFFCA8A04).copy(alpha = 0.15f)
                            "error" -> Color(0xFF991B1B).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Connection:", style = MaterialTheme.typography.labelMedium)
                            Text(voice.connectionState, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (voice.roomName != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Room:", style = MaterialTheme.typography.labelMedium)
                                Text(voice.roomName!!, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Mic enabled:", style = MaterialTheme.typography.labelMedium)
                            Text(if (voice.micEnabled) "Yes" else "No", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Track published:", style = MaterialTheme.typography.labelMedium)
                            Text(if (voice.localAudioTrackPublished) "Yes" else "No", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Active speakers:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                voice.lastActiveSpeakers,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                maxLines = 2,
                            )
                        }
                        if (voice.lastUserTranscript != "-") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Last user:", style = MaterialTheme.typography.labelSmall)
                            Text(voice.lastUserTranscript, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                        if (voice.lastAgentTranscript != "-") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Last agent:", style = MaterialTheme.typography.labelSmall)
                            Text(voice.lastAgentTranscript, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recent events:", style = MaterialTheme.typography.labelSmall)
                        voice.events.takeLast(8).forEach { ev ->
                            Text(
                                text = "${ev.timeFormatted} [${ev.kind}] ${ev.message}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SDK Debug Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (sdkLog.isEmpty()) {
                Text(
                    text = "No SDK events yet. SDK logs init, setKycStep, trackError, trigger eval, session start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                sdkLog.take(50).forEach { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = event.kind,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = event.timeFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                text = event.message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            event.details.takeIf { it.isNotEmpty() }?.let { d ->
                                Text(
                                    text = d.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class ConnectionStatus {
    data object Ok : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
    data object Loading : ConnectionStatus()
}

private data class BackendActivityItem(
    val kind: String,
    val sessionId: String,
    val timestamp: Long,
    val payload: String,
)
