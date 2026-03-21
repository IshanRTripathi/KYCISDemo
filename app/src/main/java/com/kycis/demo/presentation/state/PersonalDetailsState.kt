package com.kycis.demo.presentation.state

data class PersonalDetailsState(
    val fullName: String = "",
    val fullNameError: String? = null,
    val dateOfBirth: String = "",
    val dateOfBirthError: String? = null,
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val isValid: Boolean = false
)