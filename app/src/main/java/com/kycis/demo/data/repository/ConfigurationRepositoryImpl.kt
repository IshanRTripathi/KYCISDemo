package com.kycis.demo.data.repository

import android.content.Context
import com.kycis.demo.data.config.Configuration
import com.kycis.demo.data.config.ConfigurationParser
import com.kycis.demo.data.config.ErrorSimulations
import com.kycis.demo.data.config.FeatureFlags
import com.kycis.demo.data.config.MockScenarios
import com.kycis.demo.data.config.ValidationRules
import com.kycis.demo.domain.repository.ConfigurationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configurationParser: ConfigurationParser
) : ConfigurationRepository {

    private var cachedConfiguration: Configuration = Configuration()

    override suspend fun loadConfiguration(): Result<Configuration> = withContext(Dispatchers.IO) {
        try {
            val configFile = context.assets.open(CONFIG_FILE_NAME)
            val content = configFile.bufferedReader().use { it.readText() }
            val result = configurationParser.parse(content)
            
            result.onSuccess { config ->
                cachedConfiguration = config
            }.onFailure {
                // Use default configuration on failure
                cachedConfiguration = getDefaultConfiguration()
            }
            
            Result.success(cachedConfiguration)
        } catch (e: IOException) {
            // Use default configuration if file not found
            cachedConfiguration = getDefaultConfiguration()
            Result.success(cachedConfiguration)
        } catch (e: Exception) {
            // Use default configuration on any error
            cachedConfiguration = getDefaultConfiguration()
            Result.success(cachedConfiguration)
        }
    }

    override fun getValidationRules(): ValidationRules {
        return cachedConfiguration.validationRules
    }

    override fun getMockScenarios(): MockScenarios {
        return cachedConfiguration.mockScenarios
    }

    override fun getErrorSimulations(): ErrorSimulations {
        return cachedConfiguration.errorSimulations
    }

    override fun isFeatureEnabled(feature: String): Boolean {
        return when (feature) {
            FEATURE_PERSISTENCE -> cachedConfiguration.featureFlags.persistenceEnabled
            FEATURE_ERROR_SIMULATION -> cachedConfiguration.featureFlags.errorSimulationEnabled
            else -> false
        }
    }

    private fun getDefaultConfiguration(): Configuration {
        return Configuration(
            validationRules = ValidationRules(),
            mockScenarios = MockScenarios(),
            errorSimulations = ErrorSimulations(),
            featureFlags = FeatureFlags()
        )
    }

    companion object {
        private const val CONFIG_FILE_NAME = "kyc_config.json"
        private const val FEATURE_PERSISTENCE = "persistence"
        private const val FEATURE_ERROR_SIMULATION = "error_simulation"
    }
}