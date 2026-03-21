package com.kycis.demo.presentation.state

import com.kycis.demo.domain.models.KycScreen

data class KycUiState(
    val currentScreen: KycScreen = KycScreen.LOGIN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val personalDetails: PersonalDetailsState = PersonalDetailsState(),
    val panState: PanState = PanState(),
    val panUploadState: UploadState = UploadState(),
    val aadhaarState: AadhaarState = AadhaarState(),
    val otpState: OtpState = OtpState(),
    val selfieState: UploadState = UploadState()
)

sealed class NavigationEvent {
    data object NavigateBack : NavigationEvent()
    data class NavigateTo(val screen: KycScreen) : NavigationEvent()
    data object NavigateToSuccess : NavigationEvent()
}