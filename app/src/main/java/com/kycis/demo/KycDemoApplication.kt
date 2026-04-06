package com.kycis.demo

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.kycis.sdk.AI
import com.kycis.sdk.core.ComponentSchema
import com.kycis.sdk.core.ComponentType
import com.kycis.sdk.core.ConfigDuration
import com.kycis.sdk.core.KycisConfig
import com.kycis.sdk.core.ScreenSchema
import com.kycis.sdk.core.SdkStatusCode
import com.kycis.sdk.core.ValidationRule
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KycDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        registerScreenSchemas()
    }

    private fun registerScreenSchemas() {
        AI.registerScreenSchemas(listOf(
            ScreenSchema(
                screenId = "pan_entry",
                displayName = "PAN Number Entry",
                nextScreenId = "pan_upload",
                flowOrder = 1,
                components = listOf(
                    ComponentSchema(
                        id = "pan_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "PAN Number",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "pan_format_v1",
                                intent = "PAN_FORMAT",
                                pattern = "[A-Z]{5}[0-9]{4}[A-Z]",
                                errorCodes = listOf("pan_invalid", "pan_format_error"),
                                recoveryPlaybookId = "retry_pan_01",
                                description = "10 chars: 5 uppercase letters, 4 digits, 1 uppercase letter. Example: ABCDE1234F",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "pan_upload",
                displayName = "Upload PAN Card",
                nextScreenId = "aadhaar_entry",
                flowOrder = 3,
                components = listOf(
                    ComponentSchema(
                        id = "pan_photo",
                        type = ComponentType.CAMERA,
                        displayName = "PAN Card Photo",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "pan_photo_quality_v1",
                                intent = "DOCUMENT_QUALITY",
                                errorCodes = listOf("photo_blurry", "photo_glare", "photo_incomplete"),
                                recoveryPlaybookId = "retake_pan_photo_01",
                                description = "Clear, flat, glare-free photo of the full PAN card",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "aadhaar_entry",
                displayName = "Aadhaar Number Entry",
                nextScreenId = "otp_verify",
                flowOrder = 4,
                components = listOf(
                    ComponentSchema(
                        id = "aadhaar_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "Aadhaar Number",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "aadhaar_format_v1",
                                intent = "AADHAAR_FORMAT",
                                pattern = "[0-9]{12}",
                                errorCodes = listOf("aadhaar_invalid", "aadhaar_format_error"),
                                recoveryPlaybookId = "retry_aadhaar_01",
                                description = "12-digit Aadhaar number (no spaces or dashes)",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "otp_verify",
                displayName = "OTP Verification",
                nextScreenId = "selfie_capture",
                flowOrder = 5,
                components = listOf(
                    ComponentSchema(
                        id = "otp_field",
                        type = ComponentType.OTP_INPUT,
                        displayName = "OTP",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "otp_format_v1",
                                intent = "OTP_VERIFY",
                                pattern = "[0-9]{6}",
                                errorCodes = listOf("otp_invalid", "otp_expired", "otp_mismatch"),
                                recoveryPlaybookId = "resend_otp_01",
                                description = "6-digit OTP sent to your Aadhaar-linked mobile number",
                            ),
                        ),
                    ),
                ),
            ),
            // Single `selfie_capture` schema (below, new flow) — avoids duplicate screenId in registry.
            ScreenSchema(
                screenId = "home",
                displayName = "Home",
                nextScreenId = "phone_entry",
                flowOrder = 10,
                components = emptyList(),
            ),
            ScreenSchema(
                screenId = "phone_entry",
                displayName = "Mobile Number Entry Screen",
                nextScreenId = "phone_otp",
                flowOrder = 11,
                components = listOf(
                    ComponentSchema(
                        id = "phone_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "Mobile Number Field",
                        required = true,
                        validations = listOf(
                            ValidationRule(
                                ruleId = "phone_format_validation_v1",
                                intent = "PHONE_IN",
                                pattern = "[0-9]{10}",
                                errorCodes = listOf("phone_invalid", "phone_too_short", "phone_length_mismatch"),
                                recoveryPlaybookId = "retry_phone_number",
                                description = "Please enter your 10-digit Indian mobile number. Do not include +91 or any country code.",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "phone_otp",
                displayName = "OTP Verification Screen",
                nextScreenId = "email_entry",
                flowOrder = 12,
                components = listOf(
                    ComponentSchema(
                        id = "phone_otp_field",
                        type = ComponentType.OTP_INPUT,
                        displayName = "OTP Field",
                        required = true,
                        validations = listOf(
                            ValidationRule(
                                ruleId = "phone_otp_verify_v1",
                                intent = "OTP_VERIFY",
                                pattern = "[0-9]{4}",
                                errorCodes = listOf("phone_otp_invalid", "phone_otp_expired"),
                                recoveryPlaybookId = "retry_phone_otp",
                                description = "Please enter the 4-digit code sent to your mobile phone. Check your messages.",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "email_entry",
                displayName = "Email",
                nextScreenId = "email_otp",
                flowOrder = 13,
                components = listOf(
                    ComponentSchema(
                        id = "email_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "Email",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "email_format_v1",
                                intent = "EMAIL_IN",
                                errorCodes = listOf("email_invalid"),
                                description = "Valid email address",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "email_otp",
                displayName = "Email OTP",
                nextScreenId = "pan_details",
                flowOrder = 14,
                components = listOf(
                    ComponentSchema(
                        id = "email_otp_field",
                        type = ComponentType.OTP_INPUT,
                        displayName = "OTP",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "otp_email_v1",
                                intent = "OTP_VERIFY",
                                errorCodes = listOf("email_otp_invalid", "otp_expired"),
                                description = "OTP sent to email",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "pan_details",
                displayName = "PAN details (new flow)",
                nextScreenId = "personal_details",
                flowOrder = 15,
                components = listOf(
                    ComponentSchema(
                        id = "pan_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "PAN",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "pan_format_v1",
                                intent = "PAN_FORMAT",
                                pattern = "[A-Z]{5}[0-9]{4}[A-Z]",
                                errorCodes = listOf("pan_invalid", "pan_format_error"),
                                recoveryPlaybookId = "retry_pan_01",
                                description = "10-character PAN",
                            ),
                        ),
                    ),
                    ComponentSchema(
                        id = "dob_field",
                        type = ComponentType.DATE_PICKER,
                        displayName = "DOB",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "dob_required_v1",
                                intent = "DOB_FORMAT",
                                errorCodes = listOf("dob_invalid"),
                                description = "Date of birth",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "personal_details",
                displayName = "Personal details (new flow)",
                nextScreenId = "verify_documents",
                flowOrder = 15,
                components = listOf(
                    ComponentSchema(
                        id = "name_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "Full Name",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "name_required_v1",
                                intent = "NAME_REQUIRED",
                                errorCodes = listOf("name_empty"),
                                description = "Full name as on identity documents",
                            ),
                        ),
                    ),
                    ComponentSchema(
                        id = "father_name_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "Father Name",
                        validations = emptyList(),
                    ),
                    ComponentSchema(
                        id = "gender_field",
                        type = ComponentType.DROPDOWN,
                        displayName = "Gender",
                        validations = emptyList(),
                    ),
                    ComponentSchema(
                        id = "marital_status_field",
                        type = ComponentType.DROPDOWN,
                        displayName = "Marital Status",
                        validations = emptyList(),
                    ),
                    ComponentSchema(
                        id = "residency_status_field",
                        type = ComponentType.DROPDOWN,
                        displayName = "Residency Status",
                        validations = emptyList(),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "verify_documents",
                displayName = "Verify documents",
                nextScreenId = "upload_aadhaar_front",
                flowOrder = 16,
                components = listOf(
                    ComponentSchema(
                        id = "verify_documents_button",
                        type = ComponentType.BUTTON,
                        displayName = "Continue",
                        required = false,
                        validations = emptyList(),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "digilocker_aadhaar",
                displayName = "DigiLocker Aadhaar",
                nextScreenId = "selfie_capture",
                flowOrder = 16,
                components = listOf(
                    ComponentSchema(
                        id = "aadhaar_digilocker_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "Aadhaar number",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "aadhaar_length_v1",
                                intent = "AADHAAR_LENGTH",
                                pattern = "[0-9]{12}",
                                errorCodes = listOf("aadhaar_invalid"),
                                description = "12-digit Aadhaar",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "upload_aadhaar_front",
                displayName = "Aadhaar front",
                nextScreenId = "upload_aadhaar_back",
                flowOrder = 17,
                components = listOf(
                    ComponentSchema(
                        id = "aadhaar_front_field",
                        type = ComponentType.FILE_UPLOAD,
                        displayName = "Aadhaar front",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "doc_upload_v1",
                                intent = "DOCUMENT_UPLOAD",
                                errorCodes = listOf("upload_failed", "file_too_large"),
                                description = "Clear image of Aadhaar front",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "upload_aadhaar_back",
                displayName = "Aadhaar back",
                nextScreenId = "selfie_capture",
                flowOrder = 18,
                components = listOf(
                    ComponentSchema(
                        id = "aadhaar_back_field",
                        type = ComponentType.FILE_UPLOAD,
                        displayName = "Aadhaar back",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "doc_upload_v1",
                                intent = "DOCUMENT_UPLOAD",
                                errorCodes = listOf("upload_failed", "file_too_large"),
                                description = "Clear image of Aadhaar back",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "selfie_capture",
                displayName = "Selfie (new flow)",
                nextScreenId = "signature",
                flowOrder = 19,
                components = listOf(
                    ComponentSchema(
                        id = "selfie_capture",
                        type = ComponentType.CAMERA,
                        displayName = "Selfie",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "selfie_quality_v1",
                                intent = "LIVENESS_CHECK",
                                errorCodes = listOf("face_not_detected", "liveness_failed"),
                                recoveryPlaybookId = "retake_selfie_01",
                                description = "Selfie for KYC",
                            ),
                        ),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "signature",
                displayName = "Signature",
                nextScreenId = null,
                flowOrder = 20,
                components = listOf(
                    ComponentSchema(
                        id = "signature_field",
                        type = ComponentType.BUTTON,
                        displayName = "Sign",
                        required = false,
                        validations = emptyList(),
                    ),
                ),
            ),
            ScreenSchema(
                screenId = "sdk_diagnostics",
                displayName = "SDK ↔ backend harness",
                nextScreenId = null,
                flowOrder = 99,
                components = listOf(
                    ComponentSchema(
                        id = "harness_field",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "Harness field",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "harness_validation_v1",
                                intent = "HARNESS",
                                errorCodes = listOf("harness_invalid_field"),
                                description = "Used by SdkDiagnosticsScreen validation telemetry",
                            ),
                        ),
                    ),
                ),
            ),
        ))
    }
}
