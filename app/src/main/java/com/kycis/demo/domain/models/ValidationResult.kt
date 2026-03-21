package com.kycis.demo.domain.models

/**
 * Result of a validation operation.
 */
sealed class ValidationResult {
    /**
     * Validation passed.
     */
    data object Valid : ValidationResult()

    /**
     * Validation failed with an error message.
     */
    data class Invalid(val errorMessage: String) : ValidationResult()
}