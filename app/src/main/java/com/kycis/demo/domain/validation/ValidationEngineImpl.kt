package com.kycis.demo.domain.validation

import com.kycis.demo.domain.models.ValidationResult
import com.kycis.demo.domain.repository.ConfigurationRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

import javax.inject.Inject

class ValidationEngineImpl @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) : ValidationEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun validateEmail(email: String): ValidationResult {
        val rules = configurationRepository.getValidationRules().email

        if (email.isBlank()) {
            return if (rules.required) {
                ValidationResult.Invalid("Email is required")
            } else {
                ValidationResult.Valid
            }
        }

        val pattern = rules.pattern.toRegex()
        return if (pattern.matches(email)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Invalid email format")
        }
    }

    override fun validatePhone(phone: String): ValidationResult {
        val rules = configurationRepository.getValidationRules().phone

        if (phone.isBlank()) {
            return if (rules.required) {
                ValidationResult.Invalid("Phone number is required")
            } else {
                ValidationResult.Valid
            }
        }

        val pattern = rules.pattern.toRegex()
        return if (pattern.matches(phone)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Phone number must be 10 digits")
        }
    }

    override fun validatePAN(pan: String): ValidationResult {
        val rules = configurationRepository.getValidationRules().pan

        if (pan.isBlank()) {
            return if (rules.required) {
                ValidationResult.Invalid("PAN is required")
            } else {
                ValidationResult.Valid
            }
        }

        val pattern = rules.pattern.toRegex()
        return if (pattern.matches(pan)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Invalid PAN format (AAAAA9999A)")
        }
    }

    override fun validateAadhaar(aadhaar: String): ValidationResult {
        val rules = configurationRepository.getValidationRules().aadhaar

        if (aadhaar.isBlank()) {
            return if (rules.required) {
                ValidationResult.Invalid("Aadhaar is required")
            } else {
                ValidationResult.Valid
            }
        }

        val pattern = rules.pattern.toRegex()
        return if (pattern.matches(aadhaar)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Aadhaar must be 12 digits")
        }
    }

    override fun validateDateOfBirth(dob: String): ValidationResult {
        val rules = configurationRepository.getValidationRules().dateOfBirth

        if (dob.isBlank()) {
            return if (rules.required) {
                ValidationResult.Invalid("Date of birth is required")
            } else {
                ValidationResult.Valid
            }
        }

        val parsedDate = try {
            LocalDate.parse(dob, dateFormatter)
        } catch (e: DateTimeParseException) {
            return ValidationResult.Invalid("Invalid date format (use yyyy-MM-dd)")
        }

        val today = LocalDate.now()

        if (parsedDate.isAfter(today)) {
            return ValidationResult.Invalid("Date of birth must be in the past")
        }

        val age = ChronoUnit.YEARS.between(parsedDate, today)

        if (age < rules.minAge) {
            return ValidationResult.Invalid("Must be at least ${rules.minAge} years old")
        }

        if (age > rules.maxAge) {
            return ValidationResult.Invalid("Age cannot exceed ${rules.maxAge} years")
        }

        return ValidationResult.Valid
    }

    override fun validateRequired(value: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult.Invalid("This field is required")
        } else {
            ValidationResult.Valid
        }
    }
}