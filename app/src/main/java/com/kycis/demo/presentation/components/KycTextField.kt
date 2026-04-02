package com.kycis.demo.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kycis.sdk.ui.HintKind
import com.kycis.sdk.ui.KycTextField
import com.kycis.sdk.ui.EmbedProviderState

/**
 * Wrapper for the SDK's KycTextField with demo-specific defaults.
 */
@Composable
fun KycTextField(
    value: String,
    onValueChange: (String) -> Unit,
    componentId: String,
    hintKind: HintKind,
    providerState: EmbedProviderState?,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    // Delegate to SDK's KycTextField
    com.kycis.sdk.ui.KycTextField(
        value = value,
        onValueChange = onValueChange,
        componentId = componentId,
        hintKind = hintKind,
        providerState = providerState,
        label = label,
        modifier = modifier,
        error = error,
        enabled = enabled,
        readOnly = readOnly,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeAction = onImeAction,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        maxLines = maxLines,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}
