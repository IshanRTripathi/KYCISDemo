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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kycis.demo.BackendUrlStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendSettingsScreen(
    currentBaseUrl: String,
    onBack: () -> Unit,
    onBackendUrlSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputUrl by remember(currentBaseUrl) { mutableStateOf(currentBaseUrl) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Backend Settings") },
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
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Paste backend URL (ngrok/local). `/v1` is auto-handled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Backend URL") },
                singleLine = true,
                placeholder = { Text("https://<ngrok-domain>/v1") },
            )

            Button(
                onClick = {
                    val normalized = BackendUrlStore.normalizeBaseUrl(inputUrl)
                    testing = true
                    testResult = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { testHealth(normalized) }
                        testing = false
                        testResult = result
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !testing,
            ) {
                if (testing) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.height(0.dp))
                } else {
                    Text("Test connection (/health)", fontWeight = FontWeight.Medium)
                }
            }

            if (!testResult.isNullOrBlank()) {
                Text(
                    text = testResult ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val saved = BackendUrlStore.save(context, inputUrl)
                    Toast.makeText(context, "Saved: $saved", Toast.LENGTH_SHORT).show()
                    onBackendUrlSaved(saved)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save and apply", fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun testHealth(baseUrl: String): String {
    val healthUrl = BackendUrlStore.toHealthUrl(baseUrl)
    val (code, bodySnippet) = httpGet(healthUrl)
    return if (code in 200..299) {
        "Connected: $healthUrl ($code)"
    } else {
        "Failed: $healthUrl ($code) ${bodySnippet ?: ""}".trim()
    }
}

private fun httpGet(url: String): Pair<Int, String?> {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 4000
        readTimeout = 4000
    }
    return try {
        val code = conn.responseCode
        val body = runCatching {
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText().take(160) }
        }.getOrNull()
        code to body
    } catch (e: Exception) {
        -1 to e.message
    } finally {
        conn.disconnect()
    }
}

