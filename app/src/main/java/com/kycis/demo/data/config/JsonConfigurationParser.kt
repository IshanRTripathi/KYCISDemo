package com.kycis.demo.data.config

import kotlinx.serialization.json.Json

class JsonConfigurationParser : ConfigurationParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun parse(content: String): Result<Configuration> {
        return try {
            val config = json.decodeFromString<Configuration>(content)
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun format(config: Configuration): String {
        return json.encodeToString(Configuration.serializer(), config)
    }
}