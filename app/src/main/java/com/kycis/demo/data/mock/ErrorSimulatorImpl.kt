package com.kycis.demo.data.mock

import com.kycis.demo.data.config.ErrorSimulations

import javax.inject.Inject

class ErrorSimulatorImpl @Inject constructor(
    private val errorSimulations: ErrorSimulations
) : ErrorSimulator {

    override fun shouldSimulateError(context: ErrorContext): Boolean {
        val enabledScenarios = errorSimulations.scenarios.filter { it.enabled }

        return enabledScenarios.any { scenario ->
            matchesScreenAndAction(scenario, context) &&
                    matchesTriggerCondition(scenario, context)
        }
    }

    override fun getErrorMessage(context: ErrorContext): String {
        val enabledScenarios = errorSimulations.scenarios.filter { it.enabled }

        val matchingScenario = enabledScenarios.find { scenario ->
            matchesScreenAndAction(scenario, context) &&
                    matchesTriggerCondition(scenario, context)
        }

        return matchingScenario?.errorMessage ?: getDefaultErrorMessage(context)
    }

    private fun matchesScreenAndAction(
        scenario: com.kycis.demo.data.config.ErrorScenario,
        context: ErrorContext
    ): Boolean {
        val screenMatches = scenario.screen.isEmpty() || 
                scenario.screen.equals(context.screen, ignoreCase = true)
        val actionMatches = scenario.action.isEmpty() || 
                scenario.action.equals(context.action, ignoreCase = true)
        return screenMatches && actionMatches
    }

    private fun matchesTriggerCondition(
        scenario: com.kycis.demo.data.config.ErrorScenario,
        context: ErrorContext
    ): Boolean {
        return when (scenario.triggerCondition.lowercase()) {
            "always" -> true
            "first_attempt" -> context.attemptCount == 1
            "retry" -> context.attemptCount > 1
            "attempt_${context.attemptCount}" -> true
            else -> {
                // Check if triggerCondition matches a specific attempt count
                val attemptMatch = Regex("attempt_(\\d+)").find(scenario.triggerCondition)
                attemptMatch?.let {
                    it.groupValues[1].toIntOrNull() == context.attemptCount
                } ?: false
            }
        }
    }

    private fun getDefaultErrorMessage(context: ErrorContext): String {
        return when {
            context.action.contains("upload", ignoreCase = true) -> 
                "Upload failed. Please try again."
            context.action.contains("validate", ignoreCase = true) -> 
                "Validation failed. Please check your input."
            context.action.contains("verify", ignoreCase = true) -> 
                "Verification failed. Please try again."
            context.action.contains("send", ignoreCase = true) -> 
                "Failed to send OTP. Please try again."
            else -> 
                "An error occurred. Please try again."
        }
    }
}