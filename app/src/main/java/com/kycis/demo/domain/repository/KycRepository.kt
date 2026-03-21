package com.kycis.demo.domain.repository

import com.kycis.demo.domain.models.*

interface KycRepository {
    suspend fun submitPersonalDetails(details: PersonalDetails): Result<SubmissionResponse>
    suspend fun validatePAN(pan: String): Result<ValidationResponse>
    suspend fun uploadDocument(document: ImageData, type: DocumentType): Result<UploadResponse>
    suspend fun validateAadhaar(aadhaar: String): Result<ValidationResponse>
    suspend fun sendOTP(aadhaar: String): Result<OTPResponse>
    suspend fun verifyOTP(otp: String): Result<VerificationResponse>
    suspend fun uploadSelfie(selfie: ImageData): Result<UploadResponse>
}