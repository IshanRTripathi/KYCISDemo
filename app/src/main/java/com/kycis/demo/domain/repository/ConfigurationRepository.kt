package com.kycis.demo.domain.repository

import com.kycis.demo.data.config.Configuration
import com.kycis.demo.data.config.ErrorSimulations
import com.kycis.demo.data.config.MockScenarios
import com.kycis.demo.data.config.ValidationRules

interface ConfigurationRepository {
    suspend fun loadConfiguration(): Result<Configuration>
    fun getValidationRules(): ValidationRules
    fun getMockScenarios(): MockScenarios
    fun getErrorSimulations(): ErrorSimulations
    fun isFeatureEnabled(feature: String): Boolean
}