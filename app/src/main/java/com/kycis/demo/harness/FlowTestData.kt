package com.kycis.demo.harness

import com.kycis.sdk.core.ComponentSchema
import com.kycis.sdk.core.ComponentType

/**
 * Sample values for the flow-test harness.
 *
 * This is the *one* tenant-specific config a harness run needs beyond the app's existing
 * [com.kycis.demo.kycis.KycisScreenSchemas] / [com.kycis.demo.kycis.KycisWorkflow] — those two
 * files already define screens/components/flow order per tenant build. A different tenant's app
 * would ship its own version of just this file (or rely entirely on the generic per-[ComponentType]
 * fallback below) to make the same [FlowTestRunner] / harness screen test their flow unmodified.
 */
object FlowTestData {

    /**
     * [valid] is sent via the real `component_input` path. [invalid] (when non-null) is sent via
     * the real `validation_failed` path first, to exercise that path too — `null` means "skip the
     * invalid-value check for this component" (e.g. buttons with no validation).
     */
    data class SampleValue(val valid: String, val invalid: String?)

    private fun defaultFor(type: ComponentType): SampleValue = when (type) {
        ComponentType.TEXT_INPUT -> SampleValue(valid = "Test12345", invalid = "")
        ComponentType.OTP_INPUT -> SampleValue(valid = "1234", invalid = "0")
        ComponentType.DATE_PICKER -> SampleValue(valid = "2000-01-01", invalid = "")
        ComponentType.CHECKBOX -> SampleValue(valid = "true", invalid = "false")
        ComponentType.DROPDOWN -> SampleValue(valid = "option_1", invalid = "")
        ComponentType.RADIO_BUTTON -> SampleValue(valid = "option_1", invalid = "")
        ComponentType.BUTTON -> SampleValue(valid = "tapped", invalid = null)
        // Binary-capture components can't be simulated with a text value — FlowTestRunner skips
        // these entirely via isSimulatable(); the values here are unused placeholders.
        ComponentType.FILE_UPLOAD, ComponentType.CAMERA -> SampleValue(valid = "", invalid = null)
        else -> SampleValue(valid = "test-value", invalid = "")
    }

    /** Per-component overrides for realistic values, keyed by [ComponentSchema.id]. */
    private val overrides: Map<String, SampleValue> = mapOf(
        "phone_field" to SampleValue(valid = "9876543210", invalid = "12345"),
        "phone_otp_field" to SampleValue(valid = "1234", invalid = "0"),
        "email_field" to SampleValue(valid = "harness.tester@example.com", invalid = "not-an-email"),
        "email_otp_field" to SampleValue(valid = "1234", invalid = "0"),
        "pan_field" to SampleValue(valid = "ABCDE1234F", invalid = "INVALIDPAN"),
        "dob_field" to SampleValue(valid = "1995-06-15", invalid = ""),
        "terms_checkbox" to SampleValue(valid = "true", invalid = "false"),
        "name_field" to SampleValue(valid = "Harness Tester", invalid = ""),
        "father_name_field" to SampleValue(valid = "Test Father", invalid = ""),
        "gender_field" to SampleValue(valid = "male", invalid = ""),
        "marital_status_field" to SampleValue(valid = "single", invalid = ""),
        "residency_status_field" to SampleValue(valid = "resident", invalid = ""),
        "aadhaar_digilocker_field" to SampleValue(valid = "123456789012", invalid = "123"),
        "harness_field" to SampleValue(valid = "harness-ok", invalid = ""),
    )

    /** True for component types the harness can simulate with a synthetic text value. */
    fun isSimulatable(component: ComponentSchema): Boolean =
        component.type != ComponentType.FILE_UPLOAD && component.type != ComponentType.CAMERA

    fun sampleFor(component: ComponentSchema): SampleValue =
        overrides[component.id] ?: defaultFor(component.type)
}
