package com.kycis.demo.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kycis.demo.R
import com.kycis.demo.VoiceUiSnapshotHolder
import com.kycis.demo.presentation.form.DemoFormOptions
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.demo.kycis.KycisIntegration

@OptIn(ExperimentalMaterial3Api::class)
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

            // Send ALL fields to backend so LLM knows what's filled vs empty
            LaunchedEffect(name, gender, maritalStatus, residencyStatus, fatherName) {
                VoiceUiSnapshotHolder.setCurrentScreen("personal_details")
                val status = if (name.isBlank()) "REQUIRED" else "FILLED"
                val fatherStatus = if (fatherName.isBlank()) "REQUIRED" else "FILLED"
                val genderStatus = if (gender.isBlank()) "REQUIRED" else "FILLED"
                val maritalStatusField = if (maritalStatus.isBlank()) "REQUIRED" else "FILLED"
                val residencyStatusField = if (residencyStatus.isBlank()) "REQUIRED" else "FILLED"
                VoiceUiSnapshotHolder.upsertField("name_field", name)
                VoiceUiSnapshotHolder.upsertField("father_name_field", fatherName)
                VoiceUiSnapshotHolder.upsertField("gender_field", gender)
                VoiceUiSnapshotHolder.upsertField("marital_status_field", maritalStatus)
                VoiceUiSnapshotHolder.upsertField("residency_status_field", residencyStatus)
                
                // Send each field individually with filled status via properties map
                KycisIntegration.reportComponentInput(
                    componentId = "name_field",
                    hint = name.ifBlank { "" },
                    screen = "personal_details",
                    componentType = "text_input",
                    properties = mapOf("field_status" to status, "semantic_slot" to "full_name"),
                    sdkKb = KycisIntegration.ComponentKb(
                        displayName = "Full Name",
                        validations = listOf("Required field", "Must contain only letters and spaces")
                    )
                )
                KycisIntegration.reportComponentInput(
                    componentId = "father_name_field",
                    hint = fatherName.ifBlank { "" },
                    screen = "personal_details",
                    componentType = "text_input",
                    properties = mapOf("field_status" to fatherStatus, "semantic_slot" to "father_name"),
                    sdkKb = KycisIntegration.ComponentKb(
                        displayName = "Father's Name",
                        validations = listOf("Required field", "Must contain only letters and spaces")
                    )
                )
                KycisIntegration.reportComponentInput(
                    componentId = "gender_field",
                    hint = gender.ifBlank { "" },
                    screen = "personal_details",
                    componentType = "dropdown",
                    properties = mapOf("field_status" to genderStatus, "semantic_slot" to "gender"),
                    sdkKb = KycisIntegration.ComponentKb(
                        displayName = "Gender",
                        validations = listOf("Required field", "Select from dropdown")
                    )
                )
                KycisIntegration.reportComponentInput(
                    componentId = "marital_status_field",
                    hint = maritalStatus.ifBlank { "" },
                    screen = "personal_details",
                    componentType = "dropdown",
                    properties = mapOf("field_status" to maritalStatusField, "semantic_slot" to "marital_status"),
                    sdkKb = KycisIntegration.ComponentKb(
                        displayName = "Marital Status",
                        validations = listOf("Required field", "Select from dropdown")
                    )
                )
                KycisIntegration.reportComponentInput(
                    componentId = "residency_status_field",
                    hint = residencyStatus.ifBlank { "" },
                    screen = "personal_details",
                    componentType = "dropdown",
                    properties = mapOf("field_status" to residencyStatusField, "semantic_slot" to "residency_status"),
                    sdkKb = KycisIntegration.ComponentKb(
                        displayName = "Residency Status",
                        validations = listOf("Required field", "Select from dropdown")
                    )
                )
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
                sdkComponentId = "gender_field",
                semanticSlot = "gender",
                onValueChange = { gender = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedPickListField(
                label = "Marital Status",
                value = maritalStatus,
                placeholder = "Select Marital Status",
                options = DemoFormOptions.MARITAL_STATUS,
                sdkComponentId = "marital_status_field",
                semanticSlot = "marital_status",
                onValueChange = { maritalStatus = it },
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedPickListField(
                label = "Residency Status",
                value = residencyStatus,
                placeholder = "Select Residency Status",
                options = DemoFormOptions.RESIDENCY_STATUS,
                sdkComponentId = "residency_status_field",
                semanticSlot = "residency_status",
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
            Spacer(modifier = Modifier.height(12.dp))

            Image(
                painter = painterResource(id = R.drawable.personaldetails2),
                contentDescription = "Personal details illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        onProceed()
                    } else {
                        if (name.isEmpty()) KycisIntegration.onValidationFailed("required_field", "name_field", "text_input", businessStep = "personal_details")
                        else if (gender.isEmpty()) KycisIntegration.onValidationFailed("required_field", "gender_field", "dropdown", businessStep = "personal_details")
                        else if (maritalStatus.isEmpty()) KycisIntegration.onValidationFailed("required_field", "marital_status_field", "dropdown", businessStep = "personal_details")
                        else if (residencyStatus.isEmpty()) KycisIntegration.onValidationFailed("required_field", "residency_status_field", "dropdown", businessStep = "personal_details")
                        else if (fatherName.isEmpty()) KycisIntegration.onValidationFailed("required_field", "father_name_field", "text_input", businessStep = "personal_details")
                    }
                },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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
    semanticSlot: String? = null,
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
                        KycisIntegration.reportComponentInput(
                            componentId = sdkComponentId,
                            hint = option,
                            screen = "personal_details",
                            componentType = "dropdown",
                            properties = if (semanticSlot != null) mapOf("semantic_slot" to semanticSlot) else emptyMap(),
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

