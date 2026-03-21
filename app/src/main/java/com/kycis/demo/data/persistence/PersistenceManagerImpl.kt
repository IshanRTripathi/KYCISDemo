package com.kycis.demo.data.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kycis.demo.domain.repository.ConfigurationRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kyc_progress")

@Singleton
class PersistenceManagerImpl @Inject constructor(
    private val context: Context,
    private val configurationRepository: ConfigurationRepository
) : PersistenceManager {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private val PROGRESS_KEY = stringPreferencesKey("kyc_progress_data")
    }

    override suspend fun saveProgress(progress: KycProgress): Result<Unit> {
        return try {
            if (!isEnabled()) {
                return Result.success(Unit)
            }
            val progressJson = json.encodeToString(progress)
            context.dataStore.edit { preferences ->
                preferences[PROGRESS_KEY] = progressJson
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadProgress(): Result<KycProgress?> {
        return try {
            if (!isEnabled()) {
                return Result.success(null)
            }
            val preferences = context.dataStore.data.firstOrNull()
            val progressJson = preferences?.get(PROGRESS_KEY)
            if (progressJson.isNullOrEmpty()) {
                return Result.success(null)
            }
            val progress = json.decodeFromString<KycProgress>(progressJson)
            Result.success(progress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearProgress(): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences.remove(PROGRESS_KEY)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isEnabled(): Boolean {
        return configurationRepository.isFeatureEnabled("persistence")
    }
}