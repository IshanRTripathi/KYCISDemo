package com.kycis.demo.data.mock

import com.kycis.demo.domain.models.ImageData
import com.kycis.demo.domain.models.OTPResponse
import com.kycis.demo.domain.models.PersonalDetails
import com.kycis.demo.domain.models.SubmissionResponse
import com.kycis.demo.domain.models.UploadResponse
import com.kycis.demo.domain.models.ValidationResponse
import com.kycis.demo.domain.models.VerificationResponse

interface MockBackendService {
    suspend fun submitPersonalDetails(details: PersonalDetails): SubmissionResponse
    suspend fun validatePAN(pan: String): ValidationResponse
    suspend fun uploadDocument(document: ImageData): UploadResponse
    suspend fun validateAadhaar(aadhaar: String): ValidationResponse
    suspend fun sendOTP(aadhaar: String): OTPResponse
    suspend fun verifyOTP(otp: String): VerificationResponse
    suspend fun uploadSelfie(selfie: ImageData): UploadResponse
}