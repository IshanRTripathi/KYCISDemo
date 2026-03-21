package com.kycis.demo.data.config

interface ConfigurationParser {
    fun parse(content: String): Result<Configuration>
    fun format(config: Configuration): String
}