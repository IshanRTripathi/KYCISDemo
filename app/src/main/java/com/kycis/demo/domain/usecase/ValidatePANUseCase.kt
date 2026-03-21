package com.kycis.demo.domain.usecase

import com.kycis.demo.domain.models.ValidationResult
import com.kycis.demo.domain.validation.ValidationEngine
import javax.inject.Inject

class ValidatePANUseCase @Inject constructor(
    private val validationEngine: ValidationEngine
) {
    operator fun invoke(pan: String): ValidationResult {
        val requiredResult = validationEngine.validateRequired(pan)
        if (requiredResult is ValidationResult.Invalid) {
            return requiredResult
        }

        return validationEngine.validatePAN(pan)
    }
}