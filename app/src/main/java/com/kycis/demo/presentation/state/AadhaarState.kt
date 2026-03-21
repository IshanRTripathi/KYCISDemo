package com.kycis.demo.presentation.state

data class AadhaarState(
    val aadhaarNumber: String = "",
    val aadhaarError: String? = null,
    val isValid: Boolean = false
)