package com.kycis.demo.data.config

import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class Configuration(
    val validationRules: ValidationRules = ValidationRules(),
    val mockScenarios: MockScenarios = MockScenarios(),
    val errorSimulations: ErrorSimulations = ErrorSimulations(),
    val featureFlags: FeatureFlags = FeatureFlags()
)

@Serializable
data class ValidationRules(
    val email: EmailValidationRule = EmailValidationRule(),
    val phone: PhoneValidationRule = PhoneValidationRule(),
    val pan: PanValidationRule = PanValidationRule(),
    val aadhaar: AadhaarValidationRule = AadhaarValidationRule(),
    val dateOfBirth: DateValidationRule = DateValidationRule()
)

@Serializable
data class EmailValidationRule(
    val required: Boolean = true,
    val pattern: String = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
)

@Serializable
data class PhoneValidationRule(
    val required: Boolean = true,
    val minLength: Int = 10,
    val maxLength: Int = 10,
    val pattern: String = "^\\d{10}$"
)

@Serializable
data class PanValidationRule(
    val required: Boolean = true,
    val pattern: String = "^[A-Z]{5}\\d{4}[A-Z]$"
)

@Serializable
data class AadhaarValidationRule(
    val required: Boolean = true,
    val length: Int = 12,
    val pattern: String = "^\\d{12}$"
)

@Serializable
data class DateValidationRule(
    val required: Boolean = true,
    val minAge: Int = 18,
    val maxAge: Int = 100
)

@Serializable
data class MockScenarios(
    val responseDelays: Map<String, DelayRange> = mapOf(
        "default" to DelayRange(1000, 3000)
    ),
    val successConditions: Map<String, Boolean> = emptyMap(),
    val validOtpCodes: List<String> = listOf("123456", "000000")
)

@Serializable
data class DelayRange(
    val min: Int,
    val max: Int
)

@Serializable
data class ErrorSimulations(
    val scenarios: List<ErrorScenario> = emptyList()
)

@Serializable
data class ErrorScenario(
    val enabled: Boolean = false,
    val screen: String = "",
    val action: String = "",
    val errorType: String = "",
    val triggerCondition: String = "",
    val errorMessage: String = ""
)

@Serializable
data class FeatureFlags(
    val persistenceEnabled: Boolean = true,
    val errorSimulationEnabled: Boolean = true
)