package com.kycis.demo.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kycis.demo.BackendUrlStore
import com.kycis.demo.DemoSdkSettings
import com.kycis.demo.kycis.KycisIntegration
import com.kycis.sdk.core.TriggerStartMode
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
    onOpenSdkHarness: () -> Unit = {},
    onOpenFlowTestHarness: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog = DemoSdkSettings.Catalog

    var inputUrl by remember(currentBaseUrl) { mutableStateOf(currentBaseUrl) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var presetExpanded by remember { mutableStateOf(false) }

    var guidedMode by remember {
        mutableStateOf(BackendUrlStore.getHandholdingPreference(context))
    }
    var triggerStartMode by remember {
        mutableStateOf(DemoSdkSettings.getTriggerStartMode(context))
    }
    var boolStates by remember {
        mutableStateOf(
            (catalog.triggerGroup + catalog.privacyGroup + catalog.captureGroup)
                .associate { it.key to DemoSdkSettings.getBool(context, it) },
        )
    }

    fun updateBool(setting: DemoSdkSettings.BoolSetting, value: Boolean) {
        DemoSdkSettings.setBool(context, setting, value)
        boolStates = boolStates + (setting.key to value)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SettingsSection(
                title = "Developer tools",
                subtitle = "SDK diagnostics and automated flow walk — kept here so Home stays clean.",
            ) {
                OutlinedButton(
                    onClick = onOpenSdkHarness,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("SDK ↔ backend harness")
                }
                Text(
                    text = "Popup + trigger paths against the live backend (disposable QA session).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onOpenFlowTestHarness,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Flow E2E test")
                }
                Text(
                    text = "Walk every workflow screen against the backend; does not use the real user session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(
                title = "Connection",
                subtitle = "Where the demo SDK talks. Save applies URL + policy and restarts the activity.",
            ) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Backend URL") },
                    singleLine = true,
                    placeholder = { Text("https://api.kycis.zynnex.in/v1") },
                    supportingText = {
                        Text("Paste ngrok/local/prod. `/v1` is appended if missing.")
                    },
                )

                ExposedDropdownMenuBox(
                    expanded = presetExpanded,
                    onExpandedChange = { presetExpanded = it },
                ) {
                    OutlinedTextField(
                        value = BackendUrlStore.presets
                            .firstOrNull { BackendUrlStore.normalizeBaseUrl(it.second) == BackendUrlStore.normalizeBaseUrl(inputUrl) }
                            ?.first
                            ?: "Custom / paste above",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Environment preset") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = presetExpanded,
                        onDismissRequest = { presetExpanded = false },
                    ) {
                        BackendUrlStore.presets.forEach { (label, url) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(label, fontWeight = FontWeight.Medium)
                                        Text(
                                            BackendUrlStore.normalizeBaseUrl(url),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = {
                                    inputUrl = BackendUrlStore.normalizeBaseUrl(url)
                                    presetExpanded = false
                                },
                            )
                        }
                    }
                }

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
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                    } else {
                        Text("Test connection (/health)")
                    }
                }
                if (!testResult.isNullOrBlank()) {
                    Text(
                        text = testResult ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsSection(
                title = "Guided speaking",
                subtitle = "Session handholding preference (SDK → server). Settle/cooldown/caps stay backend-owned.",
            ) {
                SettingHint(
                    meaning = "Controls whether the voice agent speaks proactively while filling the form.",
                    supported = "passive | hybrid | active | inherit",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "passive" to "Passive",
                        "hybrid" to "Hybrid",
                        "active" to "Active",
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = guidedMode == value,
                            onClick = {
                                guidedMode = value
                                KycisIntegration.setHandholdingPreference(context, value)
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    text = when (guidedMode) {
                        "passive" -> "Passive: speak only when the user asks."
                        "active" -> "Active: milestones plus ambient nudges when stuck."
                        else -> "Hybrid: speak on field/screen milestones; no ambient nudges."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(
                title = "Triggers",
                subtitle = "When the SDK offers voice help. Applied on Save (re-init).",
            ) {
                EnumChipRow(
                    title = catalog.triggerStartMode.title,
                    meaning = catalog.triggerStartMode.meaning,
                    supported = catalog.triggerStartMode.options.joinToString(" | ") { it.first },
                    options = catalog.triggerStartMode.options,
                    selected = triggerStartMode.name,
                    onSelect = { name ->
                        val mode = runCatching { TriggerStartMode.valueOf(name) }.getOrNull() ?: return@EnumChipRow
                        triggerStartMode = mode
                        DemoSdkSettings.setTriggerStartMode(context, mode)
                    },
                )
                catalog.triggerGroup.forEach { setting ->
                    BoolSettingRow(
                        setting = setting,
                        checked = boolStates[setting.key] ?: setting.default,
                        onCheckedChange = { updateBool(setting, it) },
                    )
                }
            }

            SettingsSection(
                title = "Privacy & hints",
                subtitle = "What component values the SDK sends into voice context.",
            ) {
                catalog.privacyGroup.forEach { setting ->
                    BoolSettingRow(
                        setting = setting,
                        checked = boolStates[setting.key] ?: setting.default,
                        onCheckedChange = { updateBool(setting, it) },
                    )
                }
            }

            SettingsSection(
                title = "Capture & debug",
                subtitle = "Local SDK behavior for this demo build.",
            ) {
                catalog.captureGroup.forEach { setting ->
                    BoolSettingRow(
                        setting = setting,
                        checked = boolStates[setting.key] ?: setting.default,
                        onCheckedChange = { updateBool(setting, it) },
                    )
                }
            }

            SettingsSection(
                title = "Backend-owned (read-only)",
                subtitle = "Not editable in the app. Configure on Cloud Run / voice agent env. Shown so you know supported values.",
            ) {
                DemoSdkSettings.backendOwnedRefs.forEach { ref ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(ref.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(ref.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Supports: ${ref.supported}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            Button(
                onClick = {
                    val saved = BackendUrlStore.save(context, inputUrl)
                    // Handholding already applied live; policy bools/mode need re-init.
                    Toast.makeText(context, "Saved — restarting to apply SDK policy", Toast.LENGTH_SHORT).show()
                    onBackendUrlSaved(saved)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text("Save and apply", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        HorizontalDivider()
    }
}

@Composable
private fun SettingHint(meaning: String, supported: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Supports: $supported",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BoolSettingRow(
    setting: DemoSdkSettings.BoolSetting,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(setting.title, fontWeight = FontWeight.Medium)
            Text(setting.meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Supports: ${setting.supported}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EnumChipRow(
    title: String,
    meaning: String,
    supported: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        SettingHint(meaning = meaning, supported = supported)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                )
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
