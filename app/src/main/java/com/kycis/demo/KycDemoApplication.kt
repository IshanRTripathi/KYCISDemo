package com.kycis.demo

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.kycis.sdk.AI
import com.kycis.sdk.core.ComponentSchema
import com.kycis.sdk.core.ComponentType
import com.kycis.sdk.core.ConfirmUiText
import com.kycis.sdk.core.KycStepStrategy
import com.kycis.sdk.core.RuntimePolicy
import com.kycis.sdk.core.ScreenSchema
import com.kycis.sdk.core.SdkStatusCode
import com.kycis.sdk.core.TriggerSettings
import com.kycis.sdk.core.TriggerStartMode
import com.kycis.sdk.core.ValidationRule
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KycDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKycSdk()
    }

    private fun initKycSdk() {
        AI.setStatusListener { status ->
            Log.d("KYCIS", "SDK status: ${status.code} - ${status.message}")
            if (status.code == SdkStatusCode.ERROR) {
                Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
            }
        }

        // Register all screen schemas BEFORE AI.init() so they are pushed immediately at init.
        // screenId must match the string passed to AI.setKycStep() on each screen.
        // mappingVersion below is "v1" — bump to "v2" if any schema or validation rule changes.
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
                screenId = "personal_details",
                displayName = "Personal Details",
                nextScreenId = "pan_upload",
                flowOrder = 2,
                components = listOf(
                    ComponentSchema(
                        id = "full_name",
                        type = ComponentType.TEXT_INPUT,
                        displayName = "Full Name",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "name_required_v1",
                                intent = "NAME_REQUIRED",
                                errorCodes = listOf("name_empty", "name_invalid"),
                                description = "Full name as on PAN card",
                            ),
                        ),
                    ),
                    ComponentSchema(
                        id = "dob_field",
                        type = ComponentType.DATE_PICKER,
                        displayName = "Date of Birth",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "dob_format_v1",
                                intent = "DOB_FORMAT",
                                pattern = "DD/MM/YYYY",
                                errorCodes = listOf("dob_invalid", "dob_future"),
                                description = "Date of birth as on PAN card. Format: DD/MM/YYYY",
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
            ScreenSchema(
                screenId = "selfie_capture",
                displayName = "Selfie Capture",
                nextScreenId = "success",
                flowOrder = 6,
                components = listOf(
                    ComponentSchema(
                        id = "selfie_photo",
                        type = ComponentType.CAMERA,
                        displayName = "Selfie",
                        validations = listOf(
                            ValidationRule(
                                ruleId = "selfie_quality_v1",
                                intent = "LIVENESS_CHECK",
                                errorCodes = listOf("face_not_detected", "multiple_faces", "liveness_failed"),
                                recoveryPlaybookId = "retake_selfie_01",
                                description = "Clear front-facing selfie in good lighting, no glasses",
                            ),
                        ),
                    ),
                ),
            ),
        ))

        AI.init(
            application = this,
            apiKey = "demo-api-key",
            userId = "demo-user",
            policy = RuntimePolicy(
                // backendBaseUrl defaults to BuildConfig.KYCIS_BACKEND_URL
                // For emulator: http://10.0.2.2:8000/v1
                clientId = "kycis_demo",
                mappingVersion = "v1",
                appVersion = "1.0.0",
                minTriggerIntervalSeconds = 15,
                triggerStartMode = TriggerStartMode.CONFIRM_UI,
                kycStepStrategy = KycStepStrategy.HINT_THEN_INFER,
                triggerSettings = TriggerSettings(
                    autoTriggerEnabled = true,
                    includeErrorSignals = true,
                    includeTimeSpentSignals = true,
                    includeIdleSignals = false,
                    includeStepHints = true,
                ),
                confirmUiText = ConfirmUiText(
                    title = "Need help completing this step?",
                    startCta = "Start",
                    dismissCta = "Not now",
                ),
                passiveEvalEnabled = true,
                passiveEvalIntervalSeconds = 10,
            ),
        )
        AI.attach(this)
    }
}