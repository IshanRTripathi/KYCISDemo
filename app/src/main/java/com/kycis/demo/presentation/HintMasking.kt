package com.kycis.demo.presentation

import com.kycis.sdk.AI

enum class HintKind {
    PHONE,
    EMAIL,
    PAN,
    OTP,
    AADHAAR,
    NAME,
    FATHER_NAME,
}

private const val COMPONENT_INPUT_HINTS_MASKED_FEATURE = "component_input_hints_masked"

/**
 * Demo helper: keep the demo semantics aligned with the SDK's `component_input_hints_masked`
 * feature flag.
 *
 * - If `component_input_hints_masked == false`, we send raw values for demo visibility.
 * - If `component_input_hints_masked == true`, we mask/truncate the hint string before sending.
 */
fun componentInputHintsAreMasked(): Boolean {
    return runCatching {
        AI.getConfig().isFeatureEnabled(COMPONENT_INPUT_HINTS_MASKED_FEATURE)
    }.getOrDefault(true) // Fail closed: prefer masking if config isn't available.
}

fun maskHint(kind: HintKind, raw: String): String {
    if (!componentInputHintsAreMasked()) return raw

    val v = raw.trim()
    if (v.isEmpty()) return v

    return when (kind) {
        HintKind.PHONE -> maskDigits(v, keepLastDigits = 4)
        HintKind.AADHAAR -> maskDigits(v, keepLastDigits = 4)
        HintKind.PAN -> maskTrailingKeepLast(v, keepLast = 4)
        HintKind.OTP -> "*".repeat(v.length)
        HintKind.EMAIL -> maskEmail(v)
        HintKind.NAME, HintKind.FATHER_NAME -> maskName(v)
    }
}

private fun maskDigits(raw: String, keepLastDigits: Int): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return ""

    val keep = keepLastDigits.coerceAtMost(digits.length)
    val maskedLen = digits.length - keep
    return "*".repeat(maskedLen) + digits.takeLast(keep)
}

private fun maskTrailingKeepLast(raw: String, keepLast: Int): String {
    if (raw.length <= keepLast) return "*".repeat(raw.length)
    return "*".repeat(raw.length - keepLast) + raw.takeLast(keepLast)
}

private fun maskEmail(raw: String): String {
    val parts = raw.split("@", limit = 2)
    if (parts.size != 2) return "*".repeat(raw.length)

    val local = parts[0]
    val domain = parts[1]

    if (local.isEmpty()) return "*@$domain"
    if (local.length == 1) return "*" + "@" + domain

    // Keep first char only; mask the rest of the local-part.
    val maskedLocal = local.first() + "*".repeat(local.length - 1)
    return maskedLocal + "@" + domain
}

private fun maskName(raw: String): String {
    // Mask each word independently to avoid exposing full names.
    return raw.split(Regex("\\s+")).joinToString(" ") { part ->
        if (part.isBlank()) part
        else if (part.length <= 2) "*".repeat(part.length)
        else part.first() + "*".repeat(part.length - 2) + part.last()
    }
}

