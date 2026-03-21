package com.kycis.demo.domain.usecase

import com.kycis.demo.domain.models.PersonalDetails
import com.kycis.demo.domain.models.ValidationResult
import com.kycis.demo.domain.validation.ValidationEngine
import javax.inject.Inject

class ValidatePersonalDetailsUseCase @Inject constructor(
    private val validationEngine: ValidationEngine
) {
    operator fun invoke(details: PersonalDetails): ValidationResult {
        val fullNameResult = validationEngine.validateRequired(details.fullName)
        if (fullNameResult is ValidationResult.Invalid) {
            return ValidationResult.Invalid("Full name: ${fullNameResult.errorMessage}")
        }

        val dobResult = validationEngine.validateDateOfBirth(details.dateOfBirth)
        if (dobResult is ValidationResult.Invalid) {
            return ValidationResult.Invalid("Date of birth: ${dobResult.errorMessage}")
        }

        val phoneResult = validationEngine.validatePhone(details.phoneNumber)
        if (phoneResult is ValidationResult.Invalid) {
            return ValidationResult.Invalid("Phone: ${phoneResult.errorMessage}")
        }

        val emailResult = validationEngine.validateEmail(details.email)
        if (emailResult is ValidationResult.Invalid) {
            return ValidationResult.Invalid("Email: ${emailResult.errorMessage}")
        }

        return ValidationResult.Valid
    }
}