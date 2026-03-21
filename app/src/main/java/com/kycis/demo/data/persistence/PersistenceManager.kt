package com.kycis.demo.data.persistence

interface PersistenceManager {
    suspend fun saveProgress(progress: KycProgress): Result<Unit>
    suspend fun loadProgress(): Result<KycProgress?>
    suspend fun clearProgress(): Result<Unit>
    fun isEnabled(): Boolean
}