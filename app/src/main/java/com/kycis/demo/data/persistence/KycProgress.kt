package com.kycis.demo.data.persistence

import com.kycis.demo.domain.models.PersonalDetails
import kotlinx.serialization.Serializable

@Serializable
data class KycProgress(
    val currentScreen: String,
    val completedScreens: List<String>,
    val personalDetails: PersonalDetails?,
    val panNumber: String?,
    val aadhaarNumber: String?,
    val timestamp: Long
)