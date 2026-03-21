package com.kycis.demo.domain.usecase

import com.kycis.demo.domain.models.PersonalDetails
import com.kycis.demo.domain.models.SubmissionResponse
import com.kycis.demo.domain.repository.KycRepository
import javax.inject.Inject

class SubmitPersonalDetailsUseCase @Inject constructor(
    private val kycRepository: KycRepository
) {
    suspend operator fun invoke(details: PersonalDetails): Result<SubmissionResponse> {
        return kycRepository.submitPersonalDetails(details)
    }
}