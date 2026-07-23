package com.kycis.demo.kycis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

/**
 * One-line Compose progressive-input hook.
 *
 * ```
 * KycisTrackInput(phoneNumber, "phone_field", "phone_entry", "phone_number")
 * ```
 *
 * Host debounce defaults to 0 — the SDK applies end-of-typing debounce
 * ([com.kycis.sdk.core.RuntimePolicy.componentInputDebounceMs], default 500ms).
 * Field KB is resolved inside [KycisIntegration.reportComponentInput] via [KycisFieldKb].
 */
@Composable
fun KycisTrackInput(
    value: String,
    componentId: String,
    screen: String,
    componentType: String? = null,
    properties: Map<String, String> = emptyMap(),
    debounceMs: Long = 0L,
) {
    LaunchedEffect(value, componentId, screen, componentType, properties) {
        if (debounceMs > 0L) {
            delay(debounceMs)
        }
        KycisIntegration.reportComponentInput(
            componentId = componentId,
            hint = value,
            screen = screen,
            componentType = componentType,
            properties = properties,
        )
    }
}
