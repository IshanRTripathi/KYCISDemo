package com.kycis.demo.domain.usecase

import com.kycis.demo.domain.models.ValidationResult
import com.kycis.demo.domain.validation.ValidationEngine
import javax.inject.Inject

class ValidateAadhaarUseCase @Inject constructor(
    private val validationEngine: ValidationEngine
) {
    operator fun invoke(aadhaar: String): ValidationResult {
        val requiredResult = validationEngine.validateRequired(aadhaar)
        if (requiredResult is ValidationResult.Invalid) {
            return requiredResult
        }

        return validationEngine.validateAadhaar(aadhaar)
    }
}