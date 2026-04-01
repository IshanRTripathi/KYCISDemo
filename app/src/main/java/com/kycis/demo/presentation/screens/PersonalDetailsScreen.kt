package com.kycis.demo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.kycis.demo.presentation.form.DemoFormOptions
import com.kycis.demo.presentation.HintKind
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.demo.presentation.maskHint
import com.kycis.sdk.AI
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun PersonalDetailsScreen(
    onBack: () -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var maritalStatus by remember { mutableStateOf("") }
    var residencyStatus by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Personal Details",
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
                TextButton(onClick = onProceed) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            val isFormValid = name.isNotEmpty() && gender.isNotEmpty() && maritalStatus.isNotEmpty() && residencyStatus.isNotEmpty() && fatherName.isNotEmpty()

            LaunchedEffect(Unit) {
                snapshotFlow { name }
                    .debounce(600L)
                    .collectLatest { n ->
                        if (n.isBlank()) return@collectLatest
                        AI.reportComponentInput(
                            componentId = "n_full_name",
                            hint = maskHint(HintKind.NAME, n),
                            screen = "new_personal_details",
                            componentType = "text_input",
                        )
                    }
            }

            LaunchedEffect(Unit) {
                snapshotFlow { fatherName }
                    .debounce(600L)
                    .collectLatest { f ->
                        if (f.isBlank()) return@collectLatest
                        AI.reportComponentInput(
                            componentId = "n_father_name",
                            hint = maskHint(HintKind.FATHER_NAME, f),
                            screen = "new_personal_details",
                            componentType = "text_input",
                        )
                    }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Enter Your Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isEmpty() && fatherName.isNotEmpty(), // Simple heuristic for error state
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedPickListField(
                label = "Gender",
                value = gender,
                placeholder = "Select Gender",
                options = DemoFormOptions.GENDER,
                sdkComponentId = "n_gender",
                onValueChange = { gender = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedPickListField(
                label = "Marital Status",
                value = maritalStatus,
                placeholder = "Select Marital Status",
                options = DemoFormOptions.MARITAL_STATUS,
                sdkComponentId = "n_marital_status",
                onValueChange = { maritalStatus = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedPickListField(
                label = "Residency Status",
                value = residencyStatus,
                placeholder = "Select Residency Status",
                options = DemoFormOptions.RESIDENCY_STATUS,
                sdkComponentId = "n_residency_status",
                onValueChange = { residencyStatus = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fatherName,
                onValueChange = { fatherName = it },
                label = { Text("Father Name") },
                placeholder = { Text("Enter Father's name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onProceed,
                enabled = isFormValid,
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
                    text = "Proceed",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedPickListField(
    label: String,
    value: String,
    placeholder: String,
    options: List<String>,
    sdkComponentId: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                        AI.reportComponentInput(
                            componentId = sdkComponentId,
                            hint = option,
                            screen = "new_personal_details",
                            componentType = "dropdown",
                        )
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PersonalDetailsScreenPreview() {
    KycDemoTheme {
        PersonalDetailsScreen(onBack = {}, onProceed = {})
    }
}

