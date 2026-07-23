package com.kycis.demo.kycis

/**
 * Field KB owned by the integration package — not screens.
 *
 * [KycisTrackInput] / [KycisIntegration.reportComponentInput] attach these
 * automatically so host UI stays one-liners while voice gets validations / issues / FAQs.
 * SDK applies end-of-typing debounce ([com.kycis.sdk.core.RuntimePolicy.componentInputDebounceMs]).
 */
internal object KycisFieldKb {
    fun forComponent(componentId: String): KycisIntegration.ComponentKb? = catalog[componentId]

    private val catalog: Map<String, KycisIntegration.ComponentKb> = mapOf(
        "phone_field" to KycisIntegration.ComponentKb(
            displayName = "Phone Number",
            validations = listOf(
                "Must be exactly 10 digits",
                "Must start with 6, 7, 8, or 9",
                "Do not include country code (+91 or 0)",
            ),
            commonIssues = listOf(
                "User adds +91 or 0 prefix",
                "User enters 11 digits",
                "User enters letters or special characters",
            ),
            faqs = listOf(
                "Enter 10-digit mobile number without +91",
                "Example: 9876543210",
                "Do not use spaces or dashes",
            ),
        ),
        "phone_otp_field" to KycisIntegration.ComponentKb(
            displayName = "Phone OTP",
            validations = listOf("Must be exactly 4 digits"),
            commonIssues = listOf("User enters wrong OTP", "OTP expired"),
        ),
        "email_field" to KycisIntegration.ComponentKb(
            displayName = "Email",
            validations = listOf("Must be a valid email address"),
            commonIssues = listOf("Missing @ or domain", "Typos in common domains"),
            faqs = listOf("Example: name@gmail.com"),
        ),
        "email_otp_field" to KycisIntegration.ComponentKb(
            displayName = "Email OTP",
            validations = listOf("Must be exactly 4 digits"),
            commonIssues = listOf("User enters wrong OTP", "OTP expired", "Check spam folder"),
        ),
        "pan_field" to KycisIntegration.ComponentKb(
            displayName = "PAN Number",
            validations = listOf(
                "Must be exactly 10 characters",
                "Format: 5 letters, 4 digits, 1 letter",
                "Example: ABCDE1234F",
                "All uppercase, no spaces",
            ),
            commonIssues = listOf(
                "User enters lowercase letters",
                "User adds spaces or dashes",
                "Confusion between O and 0, I and 1",
            ),
            faqs = listOf(
                "Find your PAN on the front of your PAN card",
                "First 3 letters indicate IT department",
                "4th letter is P for person",
                "Last letter is a checksum",
            ),
        ),
        "dob_field" to KycisIntegration.ComponentKb(
            displayName = "Date of Birth",
            validations = listOf("Must be a valid date", "Format: DD/MM/YYYY"),
            commonIssues = listOf("User enters wrong format", "User enters future date"),
        ),
        "terms_checkbox" to KycisIntegration.ComponentKb(
            displayName = "Terms & Privacy",
            validations = listOf("Must accept terms and privacy policy"),
        ),
        "name_field" to KycisIntegration.ComponentKb(
            displayName = "Full Name",
            validations = listOf("Required field", "Must contain only letters and spaces"),
        ),
        "father_name_field" to KycisIntegration.ComponentKb(
            displayName = "Father's Name",
            validations = listOf("Required field", "Must contain only letters and spaces"),
        ),
        "gender_field" to KycisIntegration.ComponentKb(
            displayName = "Gender",
            validations = listOf("Required field", "Select from dropdown"),
        ),
        "marital_status_field" to KycisIntegration.ComponentKb(
            displayName = "Marital Status",
            validations = listOf("Required field", "Select from dropdown"),
        ),
        "residency_status_field" to KycisIntegration.ComponentKb(
            displayName = "Residency Status",
            validations = listOf("Required field", "Select from dropdown"),
        ),
        "aadhaar_digilocker_field" to KycisIntegration.ComponentKb(
            displayName = "Aadhaar Number",
            validations = listOf("Must be exactly 12 digits"),
            commonIssues = listOf("User enters spaces", "Aadhaar not linked to mobile number"),
        ),
        "verify_documents_button" to KycisIntegration.ComponentKb(
            displayName = "Verify documents choice",
            faqs = listOf(
                "Choosing DigiLocker verifies Aadhaar online",
                "Offline process requires uploading Aadhaar photos",
            ),
        ),
        "aadhaar_front_field" to KycisIntegration.ComponentKb(
            displayName = "Aadhaar Front Image",
            validations = listOf("Must be a clear image", "Max file size 5MB", "Supported formats: PNG, JPG, JPEG"),
            commonIssues = listOf("Image is blurry", "Glare on the card", "Edges of the card are cropped"),
        ),
        "aadhaar_back_field" to KycisIntegration.ComponentKb(
            displayName = "Aadhaar Back Image",
            validations = listOf("Must be a clear image", "Max file size 5MB", "Supported formats: PNG, JPG, JPEG"),
            commonIssues = listOf("Image is blurry", "Glare on the card", "Edges of the card are cropped"),
        ),
        "selfie_capture" to KycisIntegration.ComponentKb(
            displayName = "Selfie Capture",
            validations = listOf("Face must be clearly visible", "Sufficient lighting required", "No sunglasses or hats"),
            commonIssues = listOf("Room is too dark", "Face is partially outside the frame", "Multiple faces detected"),
        ),
        "signature_field" to KycisIntegration.ComponentKb(
            displayName = "Signature Field",
            validations = listOf("Must provide a signature"),
            commonIssues = listOf("Signature is too small", "Signature goes outside the box"),
        ),
    )
}
