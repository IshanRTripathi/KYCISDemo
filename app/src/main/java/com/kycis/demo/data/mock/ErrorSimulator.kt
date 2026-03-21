package com.kycis.demo.data.mock

data class ErrorContext(
    val screen: String,
    val action: String,
    val attemptCount: Int,
    val inputData: Map<String, Any>
)

interface ErrorSimulator {
    fun shouldSimulateError(context: ErrorContext): Boolean
    fun getErrorMessage(context: ErrorContext): String
}