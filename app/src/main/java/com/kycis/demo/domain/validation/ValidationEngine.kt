package com.kycis.demo.domain.validation

import com.kycis.demo.domain.models.ValidationResult

interface ValidationEngine {
    fun validateEmail(email: String): ValidationResult
    fun validatePhone(phone: String): ValidationResult
    fun validatePAN(pan: String): ValidationResult
    fun validateAadhaar(aadhaar: String): ValidationResult
    fun validateDateOfBirth(dob: String): ValidationResult
    fun validateRequired(value: String): ValidationResult
}