package com.kycis.demo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kycis.demo.data.camera.CameraManager
import com.kycis.demo.data.persistence.KycProgress
import com.kycis.demo.domain.models.*
import com.kycis.demo.domain.repository.ConfigurationRepository
import com.kycis.demo.domain.usecase.*
import com.kycis.demo.presentation.state.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KycViewModel @Inject constructor(
    private val validatePersonalDetailsUseCase: ValidatePersonalDetailsUseCase,
    private val validatePANUseCase: ValidatePANUseCase,
    private val validateAadhaarUseCase: ValidateAadhaarUseCase,
    private val submitPersonalDetailsUseCase: SubmitPersonalDetailsUseCase,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
    private val sendOTPUseCase: SendOTPUseCase,
    private val verifyOTPUseCase: VerifyOTPUseCase,
    private val captureSelfieUseCase: CaptureSelfieUseCase,
    private val saveProgressUseCase: SaveProgressUseCase,
    private val loadProgressUseCase: LoadProgressUseCase,
    private val cameraManager: CameraManager,
    private val configurationRepository: ConfigurationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KycUiState())
    val uiState: StateFlow<KycUiState> = _uiState.asStateFlow()

    private var otpTimerJob: Job? = null

    init {
        loadConfiguration()
        loadSavedProgress()
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            configurationRepository.loadConfiguration()
        }
    }

    private fun loadSavedProgress() {
        viewModelScope.launch {
            loadProgressUseCase().onSuccess { progress ->
                progress?.let {
                    val screen = KycScreen.valueOf(it.currentScreen)
                    _uiState.update { state ->
                        state.copy(
                            currentScreen = screen,
                            personalDetails = state.personalDetails.copy(
                                fullName = it.personalDetails?.fullName ?: "",
                                dateOfBirth = it.personalDetails?.dateOfBirth ?: "",
                                phoneNumber = it.personalDetails?.phoneNumber ?: "",
                                email = it.personalDetails?.email ?: ""
                            ),
                            panState = state.panState.copy(
                                panNumber = it.panNumber ?: ""
                            ),
                            aadhaarState = state.aadhaarState.copy(
                                aadhaarNumber = it.aadhaarNumber ?: ""
                            )
                        )
                    }
                }
            }
        }
    }

    // Navigation
    fun navigateTo(screen: KycScreen) {
        _uiState.update { it.copy(currentScreen = screen, error = null) }
        saveCurrentProgress()
    }

    fun navigateNext() {
        val currentScreen = _uiState.value.currentScreen
        val nextScreen = when (currentScreen) {
            KycScreen.LOGIN -> KycScreen.PERSONAL_DETAILS
            KycScreen.PERSONAL_DETAILS -> KycScreen.PAN_ENTRY
            KycScreen.PAN_ENTRY -> KycScreen.PAN_UPLOAD
            KycScreen.PAN_UPLOAD -> KycScreen.AADHAAR_ENTRY
            KycScreen.AADHAAR_ENTRY -> KycScreen.OTP_VERIFICATION
            KycScreen.OTP_VERIFICATION -> KycScreen.SELFIE_CAPTURE
            KycScreen.SELFIE_CAPTURE -> KycScreen.SUCCESS
            KycScreen.SUCCESS -> return
        }
        navigateTo(nextScreen)
    }

    fun canNavigateBack(): Boolean {
        return _uiState.value.currentScreen != KycScreen.LOGIN &&
                _uiState.value.currentScreen != KycScreen.SUCCESS
    }

    fun navigateBack() {
        if (!canNavigateBack()) return

        val currentScreen = _uiState.value.currentScreen
        val prevScreen = when (currentScreen) {
            KycScreen.PERSONAL_DETAILS -> KycScreen.LOGIN
            KycScreen.PAN_ENTRY -> KycScreen.PERSONAL_DETAILS
            KycScreen.PAN_UPLOAD -> KycScreen.PAN_ENTRY
            KycScreen.AADHAAR_ENTRY -> KycScreen.PAN_UPLOAD
            KycScreen.OTP_VERIFICATION -> KycScreen.AADHAAR_ENTRY
            KycScreen.SELFIE_CAPTURE -> KycScreen.OTP_VERIFICATION
            else -> return
        }
        navigateTo(prevScreen)
    }

    // Personal Details
    fun onFullNameChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                personalDetails = state.personalDetails.copy(
                    fullName = value,
                    fullNameError = null
                )
            )
        }
        validatePersonalDetails()
    }

    fun onDateOfBirthChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                personalDetails = state.personalDetails.copy(
                    dateOfBirth = value,
                    dateOfBirthError = null
                )
            )
        }
        validatePersonalDetails()
    }

    fun onPhoneNumberChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                personalDetails = state.personalDetails.copy(
                    phoneNumber = value.filter { it.isDigit() }.take(10),
                    phoneNumberError = null
                )
            )
        }
        validatePersonalDetails()
    }

    fun onEmailChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                personalDetails = state.personalDetails.copy(
                    email = value,
                    emailError = null
                )
            )
        }
        validatePersonalDetails()
    }

    private fun validatePersonalDetails() {
        val details = _uiState.value.personalDetails
        val personalDetails = PersonalDetails(
            fullName = details.fullName,
            dateOfBirth = details.dateOfBirth,
            phoneNumber = details.phoneNumber,
            email = details.email
        )

        val result = validatePersonalDetailsUseCase(personalDetails)
        when (result) {
            is ValidationResult.Valid -> {
                _uiState.update { state ->
                    state.copy(
                        personalDetails = state.personalDetails.copy(isValid = true)
                    )
                }
            }
            is ValidationResult.Invalid -> {
                parseValidationErrors(result.errorMessage)
            }
        }
    }

    private fun parseValidationErrors(errorMessage: String) {
        val errors = errorMessage.split(", ").map { it.trim() }
        _uiState.update { state ->
            var newState = state.copy(
                personalDetails = state.personalDetails.copy(isValid = false)
            )
            errors.forEach { error ->
                when {
                    error.startsWith("Full name:") -> {
                        newState = newState.copy(
                            personalDetails = newState.personalDetails.copy(
                                fullNameError = error.removePrefix("Full name: ").trim()
                            )
                        )
                    }
                    error.startsWith("Date of birth:") -> {
                        newState = newState.copy(
                            personalDetails = newState.personalDetails.copy(
                                dateOfBirthError = error.removePrefix("Date of birth: ").trim()
                            )
                        )
                    }
                    error.startsWith("Phone:") -> {
                        newState = newState.copy(
                            personalDetails = newState.personalDetails.copy(
                                phoneNumberError = error.removePrefix("Phone: ").trim()
                            )
                        )
                    }
                    error.startsWith("Email:") -> {
                        newState = newState.copy(
                            personalDetails = newState.personalDetails.copy(
                                emailError = error.removePrefix("Email: ").trim()
                            )
                        )
                    }
                }
            }
            newState
        }
    }

    fun submitPersonalDetails(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val details = _uiState.value.personalDetails
            val personalDetails = PersonalDetails(
                fullName = details.fullName,
                dateOfBirth = details.dateOfBirth,
                phoneNumber = details.phoneNumber,
                email = details.email
            )

            submitPersonalDetailsUseCase(personalDetails)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // PAN Entry
    fun onPanChanged(value: String) {
        val upperValue = value.uppercase().filter { it.isLetterOrDigit() }.take(10)
        _uiState.update { state ->
            state.copy(
                panState = state.panState.copy(
                    panNumber = upperValue,
                    panError = null
                )
            )
        }
        validatePAN()
    }

    private fun validatePAN() {
        val pan = _uiState.value.panState.panNumber
        val result = validatePANUseCase(pan)
        when (result) {
            is ValidationResult.Valid -> {
                _uiState.update { state ->
                    state.copy(panState = state.panState.copy(isValid = true))
                }
            }
            is ValidationResult.Invalid -> {
                _uiState.update { state ->
                    state.copy(
                        panState = state.panState.copy(
                            isValid = false,
                            panError = result.errorMessage
                        )
                    )
                }
            }
        }
    }

    fun submitPAN(): Boolean {
        return if (_uiState.value.panState.isValid) {
            true
        } else {
            false
        }
    }

    // PAN Upload
    fun onPanImageCaptured(imageData: ImageData) {
        _uiState.update { state ->
            state.copy(
                panUploadState = state.panUploadState.copy(
                    imageData = imageData,
                    uploadError = null
                )
            )
        }
    }

    fun capturePanFromCamera() {
        viewModelScope.launch {
            cameraManager.captureImage()
                .onSuccess { imageData ->
                    onPanImageCaptured(imageData)
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            panUploadState = state.panUploadState.copy(
                                uploadError = error.message ?: "Failed to capture image"
                            )
                        )
                    }
                }
        }
    }

    fun selectPanFromGallery() {
        viewModelScope.launch {
            cameraManager.selectFromGallery()
                .onSuccess { imageData ->
                    onPanImageCaptured(imageData)
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            panUploadState = state.panUploadState.copy(
                                uploadError = error.message ?: "Failed to select image"
                            )
                        )
                    }
                }
        }
    }

    fun uploadPanDocument(onSuccess: () -> Unit = {}) {
        val imageData = _uiState.value.panUploadState.imageData ?: return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    panUploadState = state.panUploadState.copy(
                        isUploading = true,
                        uploadError = null
                    )
                )
            }

            uploadDocumentUseCase(imageData, DocumentType.PAN_CARD)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            panUploadState = state.panUploadState.copy(
                                isUploading = false,
                                uploadSuccess = true
                            )
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            panUploadState = state.panUploadState.copy(
                                isUploading = false,
                                uploadError = error.message
                            )
                        )
                    }
                }
        }
    }

    fun retryPanUpload() {
        _uiState.update { state ->
            state.copy(
                panUploadState = UploadState()
            )
        }
    }

    // Aadhaar Entry
    fun onAadhaarChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(12)
        val formatted = formatAadhaar(digits)

        _uiState.update { state ->
            state.copy(
                aadhaarState = state.aadhaarState.copy(
                    aadhaarNumber = formatted,
                    aadhaarError = null
                )
            )
        }
        validateAadhaar()
    }

    private fun formatAadhaar(digits: String): String {
        return digits.chunked(4).joinToString(" ")
    }

    private fun validateAadhaar() {
        val aadhaar = _uiState.value.aadhaarState.aadhaarNumber.filter { it.isDigit() }
        val result = validateAadhaarUseCase(aadhaar)
        when (result) {
            is ValidationResult.Valid -> {
                _uiState.update { state ->
                    state.copy(aadhaarState = state.aadhaarState.copy(isValid = true))
                }
            }
            is ValidationResult.Invalid -> {
                _uiState.update { state ->
                    state.copy(
                        aadhaarState = state.aadhaarState.copy(
                            isValid = false,
                            aadhaarError = result.errorMessage
                        )
                    )
                }
            }
        }
    }

    fun submitAadhaar(onSuccess: () -> Unit = {}) {
        if (_uiState.value.aadhaarState.isValid) {
            sendOTP(onSuccess)
        }
    }

    // OTP
    private fun sendOTP(onSuccess: () -> Unit = {}) {
        val aadhaar = _uiState.value.aadhaarState.aadhaarNumber.filter { it.isDigit() }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            sendOTPUseCase(aadhaar)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            otpState = state.otpState.copy(otpSent = true)
                        )
                    }
                    onSuccess()
                    startOtpTimer()
                    navigateNext()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun startOtpTimer() {
        otpTimerJob?.cancel()
        otpTimerJob = viewModelScope.launch {
            var timeRemaining = 60
            while (timeRemaining > 0) {
                delay(1000)
                timeRemaining--
                _uiState.update { state ->
                    state.copy(
                        otpState = state.otpState.copy(
                            timeRemaining = timeRemaining,
                            canResend = timeRemaining == 0
                        )
                    )
                }
            }
        }
    }

    fun onOtpDigitChanged(index: Int, digit: String) {
        val newDigits = _uiState.value.otpState.digits.toMutableList()
        newDigits[index] = digit
        _uiState.update { state ->
            state.copy(
                otpState = state.otpState.copy(
                    digits = newDigits,
                    otpError = null
                )
            )
        }
    }

    fun verifyOTP(onSuccess: () -> Unit = {}) {
        val otp = _uiState.value.otpState.digits.joinToString("")

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    otpState = state.otpState.copy(isVerifying = true, otpError = null)
                )
            }

            verifyOTPUseCase(otp)
                .onSuccess { response ->
                    _uiState.update { state ->
                        state.copy(
                            otpState = state.otpState.copy(isVerifying = false)
                        )
                    }
                    if (response.isValid) {
                        onSuccess()
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                otpState = state.otpState.copy(
                                    otpError = response.message ?: "Invalid OTP"
                                )
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            otpState = state.otpState.copy(
                                isVerifying = false,
                                otpError = error.message
                            )
                        )
                    }
                }
        }
    }

    fun resendOTP() {
        sendOTP()
    }

    // Selfie
    fun onSelfieCaptured(imageData: ImageData) {
        _uiState.update { state ->
            state.copy(
                selfieState = state.selfieState.copy(
                    imageData = imageData,
                    uploadError = null
                )
            )
        }
    }

    fun captureSelfieFromCamera() {
        viewModelScope.launch {
            cameraManager.captureImage()
                .onSuccess { imageData ->
                    onSelfieCaptured(imageData)
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            selfieState = state.selfieState.copy(
                                uploadError = error.message ?: "Failed to capture selfie"
                            )
                        )
                    }
                }
        }
    }

    fun selectSelfieFromGallery() {
        viewModelScope.launch {
            cameraManager.selectFromGallery()
                .onSuccess { imageData ->
                    onSelfieCaptured(imageData)
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            selfieState = state.selfieState.copy(
                                uploadError = error.message ?: "Failed to select selfie"
                            )
                        )
                    }
                }
        }
    }

    fun uploadSelfie(onSuccess: () -> Unit = {}) {
        val imageData = _uiState.value.selfieState.imageData ?: return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    selfieState = state.selfieState.copy(
                        isUploading = true,
                        uploadError = null
                    )
                )
            }

            captureSelfieUseCase(imageData)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            selfieState = state.selfieState.copy(
                                isUploading = false,
                                uploadSuccess = true
                            )
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            selfieState = state.selfieState.copy(
                                isUploading = false,
                                uploadError = error.message
                            )
                        )
                    }
                }
        }
    }

    fun retrySelfie() {
        _uiState.update { state ->
            state.copy(selfieState = UploadState())
        }
    }

    // Progress
    private fun saveCurrentProgress() {
        viewModelScope.launch {
            val state = _uiState.value
            val progress = KycProgress(
                currentScreen = state.currentScreen.name,
                completedScreens = getCompletedScreens(),
                personalDetails = if (state.personalDetails.fullName.isNotEmpty()) {
                    PersonalDetails(
                        fullName = state.personalDetails.fullName,
                        dateOfBirth = state.personalDetails.dateOfBirth,
                        phoneNumber = state.personalDetails.phoneNumber,
                        email = state.personalDetails.email
                    )
                } else null,
                panNumber = state.panState.panNumber.takeIf { it.isNotEmpty() },
                aadhaarNumber = state.aadhaarState.aadhaarNumber.filter { it.isDigit() }.takeIf { it.isNotEmpty() },
                timestamp = System.currentTimeMillis()
            )
            saveProgressUseCase(progress)
        }
    }

    private fun getCompletedScreens(): List<String> {
        val currentScreen = _uiState.value.currentScreen
        return when (currentScreen) {
            KycScreen.LOGIN -> emptyList()
            KycScreen.PERSONAL_DETAILS -> listOf(KycScreen.LOGIN.name)
            KycScreen.PAN_ENTRY -> listOf(KycScreen.LOGIN.name, KycScreen.PERSONAL_DETAILS.name)
            KycScreen.PAN_UPLOAD -> listOf(KycScreen.LOGIN.name, KycScreen.PERSONAL_DETAILS.name, KycScreen.PAN_ENTRY.name)
            KycScreen.AADHAAR_ENTRY -> listOf(KycScreen.LOGIN.name, KycScreen.PERSONAL_DETAILS.name, KycScreen.PAN_ENTRY.name, KycScreen.PAN_UPLOAD.name)
            KycScreen.OTP_VERIFICATION -> listOf(KycScreen.LOGIN.name, KycScreen.PERSONAL_DETAILS.name, KycScreen.PAN_ENTRY.name, KycScreen.PAN_UPLOAD.name, KycScreen.AADHAAR_ENTRY.name)
            KycScreen.SELFIE_CAPTURE -> listOf(KycScreen.LOGIN.name, KycScreen.PERSONAL_DETAILS.name, KycScreen.PAN_ENTRY.name, KycScreen.PAN_UPLOAD.name, KycScreen.AADHAAR_ENTRY.name, KycScreen.OTP_VERIFICATION.name)
            KycScreen.SUCCESS -> KycScreen.entries.map { it.name }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun restartJourney() {
        otpTimerJob?.cancel()
        _uiState.value = KycUiState()
    }
}