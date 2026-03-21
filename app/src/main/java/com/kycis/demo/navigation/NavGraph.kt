package com.kycis.demo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kycis.demo.domain.models.KycScreen
import com.kycis.demo.presentation.screens.*
import com.kycis.demo.presentation.viewmodel.KycViewModel

@Composable
fun KycNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val viewModel: KycViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = KycScreen.LOGIN.name,
        modifier = modifier.fillMaxSize()
    ) {
        // Login Screen
        composable(KycScreen.LOGIN.name) {
            LoginScreen(
                onStartJourney = { 
                    navController.navigate(KycScreen.PERSONAL_DETAILS.name)
                }
            )
        }

        // Personal Details Screen
        composable(KycScreen.PERSONAL_DETAILS.name) {
            PersonalDetailsScreen(
                state = state.personalDetails,
                isLoading = state.isLoading,
                error = state.error,
                onFullNameChanged = viewModel::onFullNameChanged,
                onDateOfBirthChanged = viewModel::onDateOfBirthChanged,
                onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                onEmailChanged = viewModel::onEmailChanged,
                onContinue = { 
                    viewModel.submitPersonalDetails {
                        navController.navigate(KycScreen.PAN_ENTRY.name)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // PAN Entry Screen
        composable(KycScreen.PAN_ENTRY.name) {
            PanEntryScreen(
                state = state.panState,
                onPanChanged = viewModel::onPanChanged,
                onContinue = { 
                    val success = viewModel.submitPAN()
                    if (success) {
                        navController.navigate(KycScreen.PAN_UPLOAD.name)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // PAN Upload Screen
        composable(KycScreen.PAN_UPLOAD.name) {
            PanUploadScreen(
                state = state.panUploadState,
                onCaptureClick = viewModel::capturePanFromCamera,
                onGalleryClick = viewModel::selectPanFromGallery,
                onRetryClick = viewModel::retryPanUpload,
                onContinueClick = { 
                    viewModel.uploadPanDocument {
                        navController.navigate(KycScreen.AADHAAR_ENTRY.name)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Aadhaar Entry Screen
        composable(KycScreen.AADHAAR_ENTRY.name) {
            AadhaarEntryScreen(
                state = state.aadhaarState,
                isLoading = state.isLoading,
                onAadhaarChanged = viewModel::onAadhaarChanged,
                onContinue = { 
                    viewModel.submitAadhaar {
                        navController.navigate(KycScreen.OTP_VERIFICATION.name)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // OTP Verification Screen
        composable(KycScreen.OTP_VERIFICATION.name) {
            OtpVerificationScreen(
                state = state.otpState,
                onDigitChange = viewModel::onOtpDigitChanged,
                onVerify = { 
                    viewModel.verifyOTP {
                        navController.navigate(KycScreen.SELFIE_CAPTURE.name)
                    }
                },
                onResend = viewModel::resendOTP,
                onBack = { navController.popBackStack() }
            )
        }

        // Selfie Capture Screen
        composable(KycScreen.SELFIE_CAPTURE.name) {
            SelfieCaptureScreen(
                state = state.selfieState,
                onCaptureClick = viewModel::captureSelfieFromCamera,
                onGalleryClick = viewModel::selectSelfieFromGallery,
                onRetryClick = viewModel::retrySelfie,
                onContinueClick = { 
                    viewModel.uploadSelfie {
                        navController.navigate(KycScreen.SUCCESS.name)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Success Screen
        composable(KycScreen.SUCCESS.name) {
            SuccessScreen(
                onDone = { 
                    // Restart the journey
                    viewModel.restartJourney()
                    navController.navigate(KycScreen.LOGIN.name) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRestart = { 
                    viewModel.restartJourney()
                    navController.navigate(KycScreen.LOGIN.name) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}