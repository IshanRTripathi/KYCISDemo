package com.kycis.demo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kycis.demo.kycis.KycisIntegration
import com.kycis.demo.presentation.screens.HomeScreen
import com.kycis.demo.presentation.screens.PhoneEntryScreen
import com.kycis.demo.presentation.screens.PhoneOtpScreen
import com.kycis.demo.presentation.screens.EmailEntryScreen
import com.kycis.demo.presentation.screens.EmailOtpScreen
import com.kycis.demo.presentation.screens.PanDetailsScreen
import com.kycis.demo.presentation.screens.PersonalDetailsScreen
import com.kycis.demo.presentation.screens.VerifyDocumentsScreen
import com.kycis.demo.presentation.screens.DigilockerAadhaarScreen
import com.kycis.demo.presentation.screens.UploadAadhaarScreen
import com.kycis.demo.presentation.screens.SelfieCaptureScreen
import com.kycis.demo.presentation.screens.SignatureScreen
import com.kycis.demo.presentation.screens.SdkDiagnosticsScreen
import com.kycis.demo.presentation.screens.BackendSettingsScreen

object Routes {
    const val HOME = "home"
    const val SDK_DIAGNOSTICS = "sdk_diagnostics"
    const val BACKEND_SETTINGS = "backend_settings"
    const val PHONE_ENTRY = "phone_entry"
    const val PHONE_OTP = "phone_otp/{phone}"
    const val EMAIL_ENTRY = "email_entry"
    const val EMAIL_OTP = "email_otp/{email}"
    const val PAN_DETAILS = "pan_details"
    const val PERSONAL_DETAILS = "personal_details"
    const val VERIFY_DOCUMENTS = "verify_documents"
    const val DIGILOCKER_AADHAAR = "digilocker_aadhaar"
    const val UPLOAD_AADHAAR_FRONT = "upload_aadhaar_front"
    const val UPLOAD_AADHAAR_BACK = "upload_aadhaar_back"
    const val SELFIE_CAPTURE = "selfie_capture"
    const val SIGNATURE = "signature"
}

@Composable
fun KycNavGraph(
    navController: NavHostController,
    backendBaseUrl: String,
    onBackendUrlSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier.fillMaxSize()
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartFlow = {
                    android.util.Log.d("KYCIS", "NavGraph: onStartFlow called, navigating to phone_entry")
                    KycisIntegration.trackAnalytics("kyc_journey_started", mapOf("flow" to "onboarding"))
                    navController.navigate(Routes.PHONE_ENTRY)
                },
                onOpenSdkHarness = {
                    navController.navigate(Routes.SDK_DIAGNOSTICS)
                },
                onOpenBackendSettings = {
                    navController.navigate(Routes.BACKEND_SETTINGS)
                },
            )
        }

        composable(Routes.SDK_DIAGNOSTICS) {
            SdkDiagnosticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.BACKEND_SETTINGS) {
            BackendSettingsScreen(
                currentBaseUrl = backendBaseUrl,
                onBack = { navController.popBackStack() },
                onBackendUrlSaved = { saved ->
                    onBackendUrlSaved(saved)
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.PHONE_ENTRY) {
            PhoneEntryScreen(
                onBack = { navController.popBackStack() },
                onGetOtp = { phone ->
                    KycisIntegration.trackStepCompleted("phone_entry", nextStep = "phone_otp")
                    val encodedPhone = java.net.URLEncoder.encode(phone, "UTF-8")
                    navController.navigate(Routes.PHONE_OTP.replace("{phone}", encodedPhone))
                }
            )
        }

        composable(Routes.PHONE_OTP) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val decodedPhone = java.net.URLDecoder.decode(phone, "UTF-8")
            PhoneOtpScreen(
                phoneNumber = decodedPhone,
                onBack = { navController.popBackStack() },
                onVerify = {
                    KycisIntegration.trackStepCompleted("phone_otp", nextStep = "email_entry")
                    navController.navigate(Routes.EMAIL_ENTRY)
                },
                onResendProvider = {
                    KycisIntegration.trackOtpResend("phone")
                },
            )
        }

        composable(Routes.EMAIL_ENTRY) {
            EmailEntryScreen(
                onBack = { navController.popBackStack() },
                onGetOtp = { email ->
                    KycisIntegration.trackStepCompleted("email_entry", nextStep = "email_otp")
                    val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
                    navController.navigate(Routes.EMAIL_OTP.replace("{email}", encodedEmail))
                }
            )
        }

        composable(Routes.EMAIL_OTP) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val decodedEmail = java.net.URLDecoder.decode(email, "UTF-8")
            EmailOtpScreen(
                email = decodedEmail,
                onBack = { navController.popBackStack() },
                onVerify = {
                    KycisIntegration.trackStepCompleted("email_otp", nextStep = "pan_details")
                    navController.navigate(Routes.PAN_DETAILS)
                },
                onResendProvider = {
                    KycisIntegration.trackOtpResend("email")
                },
                onSkip = {
                    KycisIntegration.trackAnalytics("email_otp_skipped", emptyMap())
                    navController.navigate(Routes.PAN_DETAILS)
                }
            )
        }

        composable(Routes.PAN_DETAILS) {
            PanDetailsScreen(
                onBack = { navController.popBackStack() },
                onNext = {
                    KycisIntegration.trackStepCompleted("pan_details", nextStep = "personal_details")
                    navController.navigate(Routes.PERSONAL_DETAILS)
                }
            )
        }

        composable(Routes.PERSONAL_DETAILS) {
            PersonalDetailsScreen(
                onBack = { navController.popBackStack() },
                onProceed = {
                    KycisIntegration.trackStepCompleted("personal_details", nextStep = "verify_documents")
                    navController.navigate(Routes.VERIFY_DOCUMENTS)
                }
            )
        }

        composable(Routes.VERIFY_DOCUMENTS) {
            VerifyDocumentsScreen(
                onBack = { navController.popBackStack() },
                onProceedWithAadhaar = {
                    KycisIntegration.trackStepCompleted("verify_documents", nextStep = "digilocker_aadhaar")
                    navController.navigate(Routes.DIGILOCKER_AADHAAR)
                },
                onOfflineProcess = {
                    KycisIntegration.trackStepCompleted("verify_documents", nextStep = "upload_aadhaar_front")
                    navController.navigate(Routes.UPLOAD_AADHAAR_FRONT)
                }
            )
        }

        composable(Routes.DIGILOCKER_AADHAAR) {
            DigilockerAadhaarScreen(
                onBack = { navController.popBackStack() },
                onNext = {
                    KycisIntegration.trackStepCompleted("digilocker_aadhaar", nextStep = "selfie_capture")
                    navController.navigate(Routes.SELFIE_CAPTURE)
                },
                onTryAnotherWay = {
                    KycisIntegration.trackAnalytics(
                        "aadhaar_method_switched",
                        mapOf("to" to "manual_upload"),
                    )
                    navController.navigate(Routes.UPLOAD_AADHAAR_FRONT)
                }
            )
        }

        composable(Routes.UPLOAD_AADHAAR_FRONT) {
            UploadAadhaarScreen(
                isFront = true,
                onBack = { navController.popBackStack() },
                onNext = {
                    KycisIntegration.trackStepCompleted("upload_aadhaar_front", nextStep = "upload_aadhaar_back")
                    navController.navigate(Routes.UPLOAD_AADHAAR_BACK)
                }
            )
        }

        composable(Routes.UPLOAD_AADHAAR_BACK) {
            UploadAadhaarScreen(
                isFront = false,
                onBack = { navController.popBackStack() },
                onNext = {
                    KycisIntegration.trackStepCompleted("upload_aadhaar_back", nextStep = "selfie_capture")
                    navController.navigate(Routes.SELFIE_CAPTURE)
                }
            )
        }

        composable(Routes.SELFIE_CAPTURE) {
            SelfieCaptureScreen(
                onCaptured = {
                    KycisIntegration.trackStepCompleted("selfie_capture", nextStep = "signature")
                    navController.navigate(Routes.SIGNATURE)
                }
            )
        }

        composable(Routes.SIGNATURE) {
            SignatureScreen(
                onBack = { navController.popBackStack() },
                onSubmit = {
                    KycisIntegration.trackAnalytics("kyc_journey_completed", mapOf("flow" to "onboarding"))
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }
    }
}
