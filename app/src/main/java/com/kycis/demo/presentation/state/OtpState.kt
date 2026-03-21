package com.kycis.demo.presentation.state

data class OtpState(
    val digits: List<String> = List(6) { "" },
    val timeRemaining: Int = 60,
    val canResend: Boolean = false,
    val otpError: String? = null,
    val isVerifying: Boolean = false,
    val otpSent: Boolean = false
)