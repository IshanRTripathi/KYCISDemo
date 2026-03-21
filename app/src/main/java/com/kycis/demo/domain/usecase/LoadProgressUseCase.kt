package com.kycis.demo.domain.usecase

import com.kycis.demo.data.persistence.KycProgress
import com.kycis.demo.data.persistence.PersistenceManager
import javax.inject.Inject

class LoadProgressUseCase @Inject constructor(
    private val persistenceManager: PersistenceManager
) {
    suspend operator fun invoke(): Result<KycProgress?> {
        if (!persistenceManager.isEnabled()) {
            return Result.success(null) // Skip if disabled
        }
        return persistenceManager.loadProgress()
    }
}