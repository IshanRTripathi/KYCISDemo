package com.kycis.demo.domain.usecase

import com.kycis.demo.data.persistence.KycProgress
import com.kycis.demo.data.persistence.PersistenceManager
import javax.inject.Inject

class SaveProgressUseCase @Inject constructor(
    private val persistenceManager: PersistenceManager
) {
    suspend operator fun invoke(progress: KycProgress): Result<Unit> {
        if (!persistenceManager.isEnabled()) {
            return Result.success(Unit) // Skip if disabled
        }
        return persistenceManager.saveProgress(progress)
    }
}