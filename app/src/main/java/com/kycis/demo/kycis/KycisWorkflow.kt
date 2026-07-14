package com.kycis.demo.kycis

import com.kycis.sdk.core.WorkflowModel
import com.kycis.sdk.core.WorkflowStage

/**
 * Compact journey matching the demo NavGraph order (including DigiLocker / offline upload branches).
 */
object KycisWorkflow {
    val model = WorkflowModel(
        workflowName = "kyc_onboarding",
        stages = listOf(
            WorkflowStage(id = "phone_entry", expectedNext = "phone_otp", displayName = "Phone entry"),
            WorkflowStage(id = "phone_otp", expectedNext = "email_entry", displayName = "Phone OTP"),
            WorkflowStage(id = "email_entry", expectedNext = "email_otp", displayName = "Email entry"),
            WorkflowStage(id = "email_otp", expectedNext = "pan_details", displayName = "Email OTP"),
            WorkflowStage(id = "pan_details", expectedNext = "personal_details", displayName = "PAN details"),
            WorkflowStage(id = "personal_details", expectedNext = "verify_documents", displayName = "Personal details"),
            WorkflowStage(id = "verify_documents", expectedNext = "digilocker_aadhaar", displayName = "Verify documents"),
            WorkflowStage(id = "digilocker_aadhaar", expectedNext = "selfie_capture", displayName = "DigiLocker Aadhaar"),
            WorkflowStage(id = "upload_aadhaar_front", expectedNext = "upload_aadhaar_back", displayName = "Aadhaar front"),
            WorkflowStage(id = "upload_aadhaar_back", expectedNext = "selfie_capture", displayName = "Aadhaar back"),
            WorkflowStage(id = "selfie_capture", expectedNext = "signature", displayName = "Selfie"),
            WorkflowStage(id = "signature", displayName = "Signature"),
        ),
    )
}
