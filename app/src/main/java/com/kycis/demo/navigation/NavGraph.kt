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
import com.kycis.demo.presentation.screens.*
import com.kycis.demo.presentation.viewmodel.KycViewModel

// Route constants
object Routes {
    const val HOME = "home"
    const val LOGIN = "login"
    const val PERSONAL_DETAILS = "personal_details"
    const val PAN_ENTRY = "pan_entry"
    const val PAN_UPLOAD = "pan_upload"
    const val AADHAAR_ENTRY = "aadhaar_entry"
    const val OTP_VERIFICATION = "otp_verification"
    const val SELFIE_CAPTURE = "selfie_capture"
    const val SUCCESS = "success"
    const val MUTUAL_FUND_KYC = "mutual_fund_kyc"
}

@Composable
fun KycNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val viewModel: KycViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier.fillMaxSize()
    ) {
        // Home Screen - Activity List
        composable(Routes.HOME) {
            HomeScreen(
                onActivityClick = { activity ->
                    when (activity.id) {
                        "kyc" -> navController.navigate(Routes.PERSONAL_DETAILS)
                        "pan" -> navController.navigate(Routes.PAN_ENTRY)
                        "aadhaar" -> navController.navigate(Routes.AADHAAR_ENTRY)
                        "selfie" -> navController.navigate(Routes.SELFIE_CAPTURE)
                        "document" -> navController.navigate(Routes.PAN_UPLOAD)
                        "video" -> navController.navigate(Routes.SELFIE_CAPTURE)
                        "mutual_fund" -> navController.navigate(Routes.MUTUAL_FUND_KYC)
                    }
                }
            )
        }

        // Login Screen
        composable(Routes.LOGIN) {
            LoginScreen(
                onStartJourney = { 
                    navController.navigate(Routes.PERSONAL_DETAILS)
                }
            )
        }

        // Personal Details Screen
        composable(Routes.PERSONAL_DETAILS) {
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
                        navController.navigate(Routes.PAN_ENTRY)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // PAN Entry Screen
        composable(Routes.PAN_ENTRY) {
            PanEntryScreen(
                state = state.panState,
                onPanChanged = viewModel::onPanChanged,
                onContinue = { 
                    val success = viewModel.submitPAN()
                    if (success) {
                        navController.navigate(Routes.PAN_UPLOAD)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // PAN Upload Screen
        composable(Routes.PAN_UPLOAD) {
            PanUploadScreen(
                state = state.panUploadState,
                onImageCaptured = viewModel::onPanImageCaptured,
                onRetryClick = viewModel::retryPanUpload,
                onContinueClick = { 
                    viewModel.uploadPanDocument {
                        navController.navigate(Routes.AADHAAR_ENTRY)
                    }
                },
                onBack = { navController.popBackStack() },
                cameraManager = viewModel.cameraManagerImpl
            )
        }

        // Aadhaar Entry Screen
        composable(Routes.AADHAAR_ENTRY) {
            AadhaarEntryScreen(
                state = state.aadhaarState,
                isLoading = state.isLoading,
                onAadhaarChanged = viewModel::onAadhaarChanged,
                onContinue = { 
                    viewModel.submitAadhaar {
                        navController.navigate(Routes.OTP_VERIFICATION)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // OTP Verification Screen
        composable(Routes.OTP_VERIFICATION) {
            OtpVerificationScreen(
                state = state.otpState,
                onDigitChange = viewModel::onOtpDigitChanged,
                onVerify = { 
                    viewModel.verifyOTP {
                        navController.navigate(Routes.SELFIE_CAPTURE)
                    }
                },
                onResend = viewModel::resendOTP,
                onBack = { navController.popBackStack() }
            )
        }

        // Selfie Capture Screen
        composable(Routes.SELFIE_CAPTURE) {
            SelfieCaptureScreen(
                state = state.selfieState,
                onImageCaptured = viewModel::onSelfieCaptured,
                onRetryClick = viewModel::retrySelfie,
                onContinueClick = { 
                    viewModel.uploadSelfie {
                        navController.navigate(Routes.SUCCESS)
                    }
                },
                onBack = { navController.popBackStack() },
                cameraManager = viewModel.cameraManagerImpl
            )
        }

        // Success Screen
        composable(Routes.SUCCESS) {
            SuccessScreen(
                onDone = { 
                    viewModel.restartJourney()
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRestart = { 
                    viewModel.restartJourney()
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Mutual Fund KYC Screen
        composable(Routes.MUTUAL_FUND_KYC) {
            MutualFundKycScreen(
                onBack = { navController.popBackStack() },
                onSubmit = { 
                    navController.navigate(Routes.SUCCESS)
                }
            )
        }
    }
}