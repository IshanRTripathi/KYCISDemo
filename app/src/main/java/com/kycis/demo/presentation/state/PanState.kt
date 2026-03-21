package com.kycis.demo.presentation.state

data class PanState(
    val panNumber: String = "",
    val panError: String? = null,
    val isValid: Boolean = false
)