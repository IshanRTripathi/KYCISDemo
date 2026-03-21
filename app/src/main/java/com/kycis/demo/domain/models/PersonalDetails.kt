package com.kycis.demo.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class PersonalDetails(
    val fullName: String,
    val dateOfBirth: String,
    val phoneNumber: String,
    val email: String
)