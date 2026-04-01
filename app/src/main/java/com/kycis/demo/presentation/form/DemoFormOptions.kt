package com.kycis.demo.presentation.form

/**
 * Shared pick-list values for KYC / onboarding demo screens (dropdowns).
 */
object DemoFormOptions {
    val GENDER: List<String> = listOf(
        "Male",
        "Female",
        "Other",
        "Prefer not to say",
    )

    val MARITAL_STATUS: List<String> = listOf(
        "Single",
        "Married",
        "Divorced",
        "Widowed",
        "Separated",
    )

    /** Tax / KYC residency classification (India-centric demo). */
    val RESIDENCY_STATUS: List<String> = listOf(
        "Resident Indian",
        "NRI",
        "PIO",
        "OCI",
        "Foreign national",
    )

    val OCCUPATION: List<String> = listOf(
        "Salaried",
        "Self-Employed",
        "Business",
        "Professional",
        "Retired",
        "Student",
        "Homemaker",
        "Other",
    )

    /** States + union territories (common KYC address dropdown). */
    val INDIAN_STATES_AND_UTS: List<String> = listOf(
        "Andhra Pradesh",
        "Arunachal Pradesh",
        "Assam",
        "Bihar",
        "Chhattisgarh",
        "Goa",
        "Gujarat",
        "Haryana",
        "Himachal Pradesh",
        "Jharkhand",
        "Karnataka",
        "Kerala",
        "Madhya Pradesh",
        "Maharashtra",
        "Manipur",
        "Meghalaya",
        "Mizoram",
        "Nagaland",
        "Odisha",
        "Punjab",
        "Rajasthan",
        "Sikkim",
        "Tamil Nadu",
        "Telangana",
        "Tripura",
        "Uttar Pradesh",
        "Uttarakhand",
        "West Bengal",
        "Andaman and Nicobar Islands",
        "Chandigarh",
        "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi",
        "Jammu and Kashmir",
        "Ladakh",
        "Lakshadweep",
        "Puducherry",
    )
}
