package com.kycis.demo.domain.usecase

import com.kycis.demo.domain.models.VerificationResponse
import com.kycis.demo.domain.repository.KycRepository
import javax.inject.Inject

class VerifyOTPUseCase @Inject constructor(
    private val kycRepository: KycRepository
) {
    suspend operator fun invoke(otp: String): Result<VerificationResponse> {
        return kycRepository.verifyOTP(otp)
    }
}