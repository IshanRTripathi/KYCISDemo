package com.kycis.demo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kycis.demo.presentation.components.KycButton
import com.kycis.demo.presentation.components.KycTextField
import com.kycis.demo.presentation.components.LoadingIndicator
import com.kycis.demo.presentation.state.PersonalDetailsState

@Composable
fun PersonalDetailsScreen(
    state: PersonalDetailsState,
    isLoading: Boolean,
    error: String?,
    onFullNameChanged: (String) -> Unit,
    onDateOfBirthChanged: (String) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        LoadingIndicator()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Personal Details",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please enter your personal information",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        KycTextField(
            value = state.fullName,
            onValueChange = onFullNameChanged,
            label = "Full Name",
            error = state.fullNameError,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        KycTextField(
            value = state.dateOfBirth,
            onValueChange = onDateOfBirthChanged,
            label = "Date of Birth (YYYY-MM-DD)",
            error = state.dateOfBirthError,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        KycTextField(
            value = state.phoneNumber,
            onValueChange = onPhoneNumberChanged,
            label = "Phone Number",
            error = state.phoneNumberError,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        KycTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = "Email",
            error = state.emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        KycButton(
            text = "Continue",
            onClick = onContinue,
            enabled = state.fullName.isNotEmpty() && state.dateOfBirth.isNotEmpty() && 
                      state.phoneNumber.isNotEmpty() && state.email.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}