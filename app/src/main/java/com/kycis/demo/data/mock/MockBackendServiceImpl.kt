package com.kycis.demo.data.mock

import com.kycis.demo.data.config.DelayRange
import com.kycis.demo.data.config.MockScenarios
import com.kycis.demo.domain.models.ImageData
import com.kycis.demo.domain.models.OTPResponse
import com.kycis.demo.domain.models.PersonalDetails
import com.kycis.demo.domain.models.SubmissionResponse
import com.kycis.demo.domain.models.UploadResponse
import com.kycis.demo.domain.models.ValidationResponse
import com.kycis.demo.domain.models.VerificationResponse
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

import javax.inject.Inject

class MockBackendServiceImpl @Inject constructor(
    private val mockScenarios: MockScenarios,
    private val errorSimulator: ErrorSimulator
) : MockBackendService {

    override suspend fun submitPersonalDetails(details: PersonalDetails): SubmissionResponse {
        val delayMs = getDelay("submitPersonalDetails")
        delay(delayMs.toLong())

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
            return SubmissionResponse(
                success = false,
                message = errorSimulator.getErrorMessage(context),
                sessionId = null
            )
        }

        return SubmissionResponse(
            success = true,
            message = "Personal details submitted successfully",
            sessionId = UUID.randomUUID().toString()
        )
    }

    override suspend fun validatePAN(pan: String): ValidationResponse {
        val delayMs = getDelay("validatePAN")
        delay(delayMs.toLong())

        val context = ErrorContext(
            screen = "pan_verification",
            action = "validate",
            attemptCount = 1,
            inputData = mapOf("pan" to pan)
        )

        if (errorSimulator.shouldSimulateError(context)) {
            return ValidationResponse(
                isValid = false,
                message = errorSimulator.getErrorMessage(context)
            )
        }

        val isValid = pan.matches(Regex("^[A-Z]{5}\\d{4}[A-Z]$"))
        return ValidationResponse(
            isValid = isValid,
            message = if (isValid) "PAN validated successfully" else "Invalid PAN format"
        )
    }

    override suspend fun uploadDocument(document: ImageData): UploadResponse {
        val delayMs = getDelay("uploadDocument")
        delay(delayMs.toLong())

        val context = ErrorContext(
            screen = "document_upload",
            action = "upload",
            attemptCount = 1,
            inputData = mapOf(
                "uri" to document.uri,
                "sizeBytes" to document.sizeBytes,
                "format" to document.format.name
            )
        )

        if (errorSimulator.shouldSimulateError(context)) {
            return UploadResponse(
                success = false,
                documentId = null,
                message = errorSimulator.getErrorMessage(context)
            )
        }

        return UploadResponse(
            success = true,
            documentId = UUID.randomUUID().toString(),
            message = "Document uploaded successfully"
        )
    }

    override suspend fun validateAadhaar(aadhaar: String): ValidationResponse {
        val delayMs = getDelay("validateAadhaar")
        delay(delayMs.toLong())

        val context = ErrorContext(
            screen = "aadhaar_verification",
            action = "validate",
            attemptCount = 1,
            inputData = mapOf("aadhaar" to aadhaar)
        )

        if (errorSimulator.shouldSimulateError(context)) {
            return ValidationResponse(
                isValid = false,
                message = errorSimulator.getErrorMessage(context)
            )
        }

        val isValid = aadhaar.matches(Regex("^\\d{12}$"))
        return ValidationResponse(
            isValid = isValid,
            message = if (isValid) "Aadhaar validated successfully" else "Invalid Aadhaar format"
        )
    }

    override suspend fun sendOTP(aadhaar: String): OTPResponse {
        val delayMs = getDelay("sendOTP")
        delay(delayMs.toLong())

        val context = ErrorContext(
            screen = "otp_verification",
            action = "send",
            attemptCount = 1,
            inputData = mapOf("aadhaar" to aadhaar)
        )

        if (errorSimulator.shouldSimulateError(context)) {
            return OTPResponse(
                success = false,
                message = errorSimulator.getErrorMessage(context)
            )
        }

        val otpCode = generateRandomOtp()
        return OTPResponse(
            success = true,
            message = "OTP sent successfully. Code: $otpCode"
        )
    }

    override suspend fun verifyOTP(otp: String): VerificationResponse {
        val delayMs = getDelay("verifyOTP")
        delay(delayMs.toLong())

        val context = ErrorContext(
            screen = "otp_verification",
            action = "verify",
            attemptCount = 1,
            inputData = mapOf("otp" to otp)
        )

        if (errorSimulator.shouldSimulateError(context)) {
            return VerificationResponse(
                isValid = false,
                message = errorSimulator.getErrorMessage(context)
            )
        }

        val isValid = mockScenarios.validOtpCodes.contains(otp)
        return VerificationResponse(
            isValid = isValid,
            message = if (isValid) "OTP verified successfully" else "Invalid OTP"
        )
    }

    override suspend fun uploadSelfie(selfie: ImageData): UploadResponse {
        val delayMs = getDelay("uploadSelfie")
        delay(delayMs.toLong())

        val context = ErrorContext(
            screen = "selfie_upload",
            action = "upload",
            attemptCount = 1,
            inputData = mapOf(
                "uri" to selfie.uri,
                "sizeBytes" to selfie.sizeBytes,
                "format" to selfie.format.name
            )
        )

        if (errorSimulator.shouldSimulateError(context)) {
            return UploadResponse(
                success = false,
                documentId = null,
                message = errorSimulator.getErrorMessage(context)
            )
        }

        return UploadResponse(
            success = true,
            documentId = UUID.randomUUID().toString(),
            message = "Selfie uploaded successfully"
        )
    }

    private fun getDelay(action: String): Int {
        val delayRange = mockScenarios.responseDelays[action]
            ?: mockScenarios.responseDelays["default"]
            ?: DelayRange(1000, 3000)
        return Random.nextInt(delayRange.min, delayRange.max + 1)
    }

    private fun generateRandomOtp(): String {
        return (100000..999999).random().toString()
    }
}