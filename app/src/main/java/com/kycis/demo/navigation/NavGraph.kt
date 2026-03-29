package com.kycis.demo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kycis.demo.presentation.screens.*
import com.kycis.demo.presentation.screens.legacy.LegacyAadhaarEntryScreen
import com.kycis.demo.presentation.screens.legacy.LegacyHomeScreen
import com.kycis.demo.presentation.screens.legacy.LegacyLoginScreen
import com.kycis.demo.presentation.screens.legacy.LegacyMutualFundKycScreen
import com.kycis.demo.presentation.screens.legacy.LegacyOnboardingScreen
import com.kycis.demo.presentation.screens.legacy.LegacyOtpVerificationScreen
import com.kycis.demo.presentation.screens.legacy.LegacyPanEntryScreen
import com.kycis.demo.presentation.screens.legacy.LegacyPanUploadScreen
import com.kycis.demo.presentation.screens.legacy.LegacyPersonalDetailsScreen
import com.kycis.demo.presentation.screens.legacy.LegacySdkDebugScreen
import com.kycis.demo.presentation.screens.legacy.LegacySelfieCaptureScreen
import com.kycis.demo.presentation.screens.legacy.LegacySuccessScreen
import com.kycis.demo.presentation.viewmodel.KycViewModel
import com.kycis.sdk.AI

// Route constants
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SDK_DEBUG = "sdk_debug"
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

object NewRoutes {
    const val HOME = "new_home"
    const val PHONE_ENTRY = "new_phone_entry"
    const val PHONE_OTP = "new_phone_otp/{phone}"
    const val EMAIL_ENTRY = "new_email_entry"
    const val EMAIL_OTP = "new_email_otp/{email}"
    const val PAN_DETAILS = "new_pan_details"
    const val PERSONAL_DETAILS = "new_personal_details"
    const val VERIFY_DOCUMENTS = "new_verify_documents"
    const val UPLOAD_AADHAAR_FRONT = "new_upload_aadhaar_front"
    const val UPLOAD_AADHAAR_BACK = "new_upload_aadhaar_back"
    const val SELFIE_CAPTURE = "new_selfie_capture"
    const val SIGNATURE = "new_signature"
}

@Composable
fun KycNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NewRoutes.HOME,
        modifier = modifier.fillMaxSize()
    ) {
        composable(NewRoutes.HOME) {
            LaunchedEffect(Unit) { AI.setKycStep("new_home") }
            HomeScreen(
                onStartFlow = { navController.navigate(NewRoutes.PHONE_ENTRY) },
                onLegacyScreensClick = { navController.navigate(Routes.ONBOARDING) }
            )
        }
        
        composable(NewRoutes.PHONE_ENTRY) {
            LaunchedEffect(Unit) { AI.setKycStep("phone_entry") }
            PhoneEntryScreen(
                onBack = { navController.popBackStack() },
                onGetOtp = { phone -> 
                     val encodedPhone = java.net.URLEncoder.encode(phone, "UTF-8")
                     navController.navigate(NewRoutes.PHONE_OTP.replace("{phone}", encodedPhone))
                }
            )
        }
        
        composable(NewRoutes.PHONE_OTP) { backStackEntry -> 
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val decodedPhone = java.net.URLDecoder.decode(phone, "UTF-8")
            LaunchedEffect(Unit) { AI.setKycStep("phone_otp") }
            PhoneOtpScreen(
                phoneNumber = decodedPhone,
                onBack = { navController.popBackStack() },
                onVerify = { 
                    navController.navigate(NewRoutes.EMAIL_ENTRY) 
                },
                onResendProvider = {}
            )
        }
        
        composable(NewRoutes.EMAIL_ENTRY) {
            LaunchedEffect(Unit) { AI.setKycStep("email_entry") }
            EmailEntryScreen(
                onBack = { navController.popBackStack() },
                onGetOtp = { email ->
                    val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
                    navController.navigate(NewRoutes.EMAIL_OTP.replace("{email}", encodedEmail))
                }
            )
        }
        
        composable(NewRoutes.EMAIL_OTP) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val decodedEmail = java.net.URLDecoder.decode(email, "UTF-8")
            LaunchedEffect(Unit) { AI.setKycStep("email_otp") }
            EmailOtpScreen(
                email = decodedEmail,
                onBack = { navController.popBackStack() },
                onVerify = {
                    navController.navigate("new_pan_details")
                },
                onResendProvider = {}
            )
        }
        composable(NewRoutes.PAN_DETAILS) {
            LaunchedEffect(Unit) { AI.setKycStep("pan_details") }
            PanDetailsScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(NewRoutes.PERSONAL_DETAILS) }
            )
        }

        composable(NewRoutes.PERSONAL_DETAILS) {
            LaunchedEffect(Unit) { AI.setKycStep("personal_details") }
            PersonalDetailsScreen(
                onBack = { navController.popBackStack() },
                onProceed = { navController.navigate(NewRoutes.VERIFY_DOCUMENTS) }
            )
        }
        
        composable(NewRoutes.VERIFY_DOCUMENTS) {
            LaunchedEffect(Unit) { AI.setKycStep("verify_documents") }
            VerifyDocumentsScreen(
                onBack = { navController.popBackStack() },
                onProceedWithAadhaar = { navController.navigate(NewRoutes.UPLOAD_AADHAAR_FRONT) }, // Will lead to digilocker or direct upload
                onOfflineProcess = { navController.navigate(NewRoutes.UPLOAD_AADHAAR_FRONT) }
            )
        }
        
        composable(NewRoutes.UPLOAD_AADHAAR_FRONT) {
            LaunchedEffect(Unit) { AI.setKycStep("upload_aadhaar_front") }
            UploadAadhaarScreen(
                isFront = true,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(NewRoutes.UPLOAD_AADHAAR_BACK) }
            )
        }

        composable(NewRoutes.UPLOAD_AADHAAR_BACK) {
            LaunchedEffect(Unit) { AI.setKycStep("upload_aadhaar_back") }
            UploadAadhaarScreen(
                isFront = false,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(NewRoutes.SELFIE_CAPTURE) }
            )
        }
        
        composable(NewRoutes.SELFIE_CAPTURE) {
            LaunchedEffect(Unit) { AI.setKycStep("new_selfie_capture") }
            SelfieCaptureScreen(
                onCaptured = { navController.navigate(NewRoutes.SIGNATURE) }
            )
        }
        
        composable(NewRoutes.SIGNATURE) {
            LaunchedEffect(Unit) { AI.setKycStep("new_signature") }
            SignatureScreen(
                onBack = { navController.popBackStack() },
                onSubmit = { navController.navigate(NewRoutes.HOME) { popUpTo(NewRoutes.HOME) { inclusive = true } } }
            )
        }
        
        composable(Routes.ONBOARDING) {
            LaunchedEffect(Unit) { AI.setKycStep("onboarding") }
            LegacyOnboardingScreen(
                onStartKyc = { navController.navigate(Routes.PERSONAL_DETAILS) },
                onExploreAllDemos = { navController.navigate(Routes.HOME) },
            )
        }

        // Home Screen - Activity List (no shared ViewModel needed)
        composable(Routes.HOME) {
            LaunchedEffect(Unit) { AI.setKycStep("home") }
            LegacyHomeScreen(
                onActivityClick = { activity ->
                    when (activity.id) {
                        "sdk_debug" -> navController.navigate(Routes.SDK_DEBUG)
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

        composable(Routes.SDK_DEBUG) {
            LaunchedEffect(Unit) { AI.setKycStep("sdk_debug") }
            LegacySdkDebugScreen(onBack = { navController.popBackStack() })
        }

        // Login Screen
        composable(Routes.LOGIN) {
            LaunchedEffect(Unit) { AI.setKycStep("login") }
            LegacyLoginScreen(
                onStartJourney = { 
                    navController.navigate(Routes.PERSONAL_DETAILS)
                }
            )
        }

        // Personal Details Screen
        composable(Routes.PERSONAL_DETAILS) {
            val viewModel: KycViewModel = hiltViewModel(navController.getBackStackEntry(Routes.HOME))
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { AI.setKycStep("personal_details") }
            LegacyPersonalDetailsScreen(
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
            val viewModel: KycViewModel = hiltViewModel(navController.getBackStackEntry(Routes.HOME))
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { AI.setKycStep("pan_entry") }
            LegacyPanEntryScreen(
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
            val viewModel: KycViewModel = hiltViewModel(navController.getBackStackEntry(Routes.HOME))
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { AI.setKycStep("pan_upload") }
            LegacyPanUploadScreen(
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
            val viewModel: KycViewModel = hiltViewModel(navController.getBackStackEntry(Routes.HOME))
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { AI.setKycStep("aadhaar_entry") }
            LegacyAadhaarEntryScreen(
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
            val viewModel: KycViewModel = hiltViewModel(navController.getBackStackEntry(Routes.HOME))
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { AI.setKycStep("otp_verify") }
            LegacyOtpVerificationScreen(
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
            val viewModel: KycViewModel = hiltViewModel(navController.getBackStackEntry(Routes.HOME))
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { AI.setKycStep("selfie_capture") }
            LegacySelfieCaptureScreen(
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
            LaunchedEffect(Unit) { AI.setKycStep("success") }
            val viewModel: KycViewModel = hiltViewModel(navController.getBackStackEntry(Routes.HOME))
            LegacySuccessScreen(
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

        // Mutual Fund KYC Screen (no shared ViewModel)
        composable(Routes.MUTUAL_FUND_KYC) {
            LaunchedEffect(Unit) { AI.setKycStep("mutual_fund_kyc") }
            LegacyMutualFundKycScreen(
                onBack = { navController.popBackStack() },
                onSubmit = { 
                    navController.navigate(Routes.SUCCESS)
                }
            )
        }
    }
}
