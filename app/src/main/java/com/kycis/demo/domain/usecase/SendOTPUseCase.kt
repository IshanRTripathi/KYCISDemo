package com.kycis.demo.domain.usecase

import com.kycis.demo.domain.models.OTPResponse
import com.kycis.demo.domain.repository.KycRepository
import javax.inject.Inject

class SendOTPUseCase @Inject constructor(
    private val kycRepository: KycRepository
) {
    suspend operator fun invoke(aadhaar: String): Result<OTPResponse> {
        return kycRepository.sendOTP(aadhaar)
    }
}