package com.kycis.demo.examples

import com.kycis.sdk.AI

/**
 * Examples demonstrating Phase 1 enhancements (SDK v2.0):
 * - SDK Metadata (automatic)
 * - Event Ordering (best practices)
 * - Analytics Events (new API)
 */
object Phase1Examples {

    /**
     * Example 1: Proper event ordering
     * Always send identity first, then other events
     */
    fun properEventOrdering() {
        // Step 1: Send identity event first
        AI.setUser(
            id = "user123",
            phone = "+919876543210",
            phoneMasked = false
        )
        
        // Step 2: Now safe to send other events
        AI.setKycStep("pan_entry")
        AI.trackValidationFailure(
            failureReasonCode = "pan_invalid",
            componentId = "pan_field"
        )
    }

    /**
     * Example 2: Using the new analytics API
     * Track business metrics with semantic event names
     */
    fun trackBusinessMetrics() {
        // Track KYC completion
        AI.trackAnalytics("kyc_completed", mapOf(
            "duration_seconds" to 180,
            "steps_completed" to 6,
            "flow" to "onboarding",
            "document_types" to listOf("pan", "aadhaar", "selfie")
        ))
        
        // Track document upload
        AI.trackAnalytics("document_uploaded", mapOf(
            "document_type" to "pan_card",
            "file_size_kb" to 245,
            "upload_duration_ms" to 1200
        ))
        
        // Track user drop-off
        AI.trackAnalytics("user_dropped_off", mapOf(
            "last_screen" to "aadhaar_entry",
            "time_spent_seconds" to 45,
            "errors_encountered" to 2
        ))
    }

    /**
     * Example 3: SDK Metadata is automatic
     * All events now include sdk metadata - no code changes needed
     */
    fun sdkMetadataAutomatic() {
        // This event will automatically include:
        // "sdk": {
        //   "sdk_name": "kycis-android",
        //   "sdk_version": "2.0.0",
        //   "platform": "android"
        // }
        AI.trackError("network_error", mapOf(
            "endpoint" to "/api/verify",
            "status_code" to 500
        ))
    }

    /**
     * Example 4: Combining old and new APIs
     * Both approaches work, but analytics API is recommended for metrics
     */
    fun mixedApproach() {
        // Use trackError for operational errors
        AI.trackError("api_timeout", mapOf(
            "endpoint" to "/verify",
            "timeout_ms" to 30000
        ))
        
        // Use trackAnalytics for business events
        AI.trackAnalytics("verification_started", mapOf(
            "verification_type" to "aadhaar",
            "user_segment" to "premium"
        ))
        
        // Use trackValidationFailure for form validation
        AI.trackValidationFailure(
            failureReasonCode = "aadhaar_format_error",
            componentId = "aadhaar_field",
            hint = "12 digits required"
        )
    }

    /**
     * Example 5: Analytics for user journey tracking
     */
    fun trackUserJourney() {
        // Journey start
        AI.trackAnalytics("kyc_journey_started", mapOf(
            "entry_point" to "home_screen",
            "user_type" to "new_customer"
        ))
        
        // Journey milestones
        AI.trackAnalytics("kyc_step_completed", mapOf(
            "step_name" to "pan_verification",
            "step_number" to 1,
            "total_steps" to 6
        ))
        
        // Journey completion
        AI.trackAnalytics("kyc_journey_completed", mapOf(
            "total_duration_seconds" to 240,
            "retry_count" to 1,
            "success" to true
        ))
    }

    /**
     * Example 6: Error tracking vs analytics
     * When to use which API
     */
    fun errorVsAnalytics() {
        // Use trackError for SDK/system errors
        AI.trackError("permission_denied", mapOf(
            "permission" to "CAMERA",
            "rationale_shown" to true
        ))
        
        // Use trackValidationFailure for form validation
        AI.trackValidationFailure(
            failureReasonCode = "invalid_format",
            componentId = "phone_field"
        )
        
        // Use trackAnalytics for business events
        AI.trackAnalytics("form_abandoned", mapOf(
            "form_name" to "personal_details",
            "fields_completed" to 3,
            "fields_total" to 5
        ))
    }
}
