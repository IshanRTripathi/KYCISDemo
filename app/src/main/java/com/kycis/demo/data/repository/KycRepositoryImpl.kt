package com.kycis.demo.data.repository

import com.kycis.demo.data.mock.ErrorContext
import com.kycis.demo.data.mock.ErrorSimulator
import com.kycis.demo.data.mock.MockBackendService
import com.kycis.demo.domain.models.*
import com.kycis.demo.domain.repository.KycRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycRepositoryImpl @Inject constructor(
    private val mockBackendService: MockBackendService,
    private val errorSimulator: ErrorSimulator
) : KycRepository {

    override suspend fun submitPersonalDetails(details: PersonalDetails): Result<SubmissionResponse> {
        return try {
            val context = ErrorContext(
                screen = "personal_details",
                action = "submit",
                attemptCount = 1,
                inputData = mapOf(
                    "fullName" to details.fullName,
                    "email" to details.email,
                    "phoneNumber" to details.phoneNumber
                )
            )

            if (errorSimulator.shouldSimulateError(context)) {
                return Result.failure(
                    Exception(errorSimulator.getErrorMessage(context))
                )
            }

            val response = mockBackendService.submitPersonalDetails(details)
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validatePAN(pan: String): Result<ValidationResponse> {
        return try {
            val context = ErrorContext(
                screen = "pan_entry",
                action = "validate",
                attemptCount = 1,
                inputData = mapOf("pan" to pan)
            )

            if (errorSimulator.shouldSimulateError(context)) {
                return Result.failure(
                    Exception(errorSimulator.getErrorMessage(context))
                )
            }

            val response = mockBackendService.validatePAN(pan)
            if (response.isValid) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadDocument(document: ImageData, type: DocumentType): Result<UploadResponse> {
        return try {
            val context = ErrorContext(
                screen = "pan_upload",
                action = "upload",
                attemptCount = 1,
                inputData = mapOf(
                    "documentType" to type.name,
                    "sizeBytes" to document.sizeBytes
                )
            )

            if (errorSimulator.shouldSimulateError(context)) {
                return Result.failure(
                    Exception(errorSimulator.getErrorMessage(context))
                )
            }

            val response = mockBackendService.uploadDocument(document)
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateAadhaar(aadhaar: String): Result<ValidationResponse> {
        return try {
            val context = ErrorContext(
                screen = "aadhaar_entry",
                action = "validate",
                attemptCount = 1,
                inputData = mapOf("aadhaar" to aadhaar)
            )

            if (errorSimulator.shouldSimulateError(context)) {
                return Result.failure(
                    Exception(errorSimulator.getErrorMessage(context))
                )
            }

            val response = mockBackendService.validateAadhaar(aadhaar)
            if (response.isValid) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendOTP(aadhaar: String): Result<OTPResponse> {
        return try {
            val context = ErrorContext(
                screen = "otp_verification",
                action = "send",
                attemptCount = 1,
                inputData = mapOf("aadhaar" to aadhaar)
            )

            if (errorSimulator.shouldSimulateError(context)) {
                return Result.failure(
                    Exception(errorSimulator.getErrorMessage(context))
                )
            }

            val response = mockBackendService.sendOTP(aadhaar)
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyOTP(otp: String): Result<VerificationResponse> {
        return try {
            val context = ErrorContext(
                screen = "otp_verification",
                action = "verify",
                attemptCount = 1,
                inputData = mapOf("otp" to otp)
            )

            if (errorSimulator.shouldSimulateError(context)) {
                return Result.failure(
                    Exception(errorSimulator.getErrorMessage(context))
                )
            }

            val response = mockBackendService.verifyOTP(otp)
            if (response.isValid) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadSelfie(selfie: ImageData): Result<UploadResponse> {
        return try {
            val context = ErrorContext(
                screen = "selfie_capture",
                action = "upload",
                attemptCount = 1,
                inputData = mapOf(
                    "sizeBytes" to selfie.sizeBytes
                )
            )

            if (errorSimulator.shouldSimulateError(context)) {
                return Result.failure(
                    Exception(errorSimulator.getErrorMessage(context))
                )
            }

            val response = mockBackendService.uploadSelfie(selfie)
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}