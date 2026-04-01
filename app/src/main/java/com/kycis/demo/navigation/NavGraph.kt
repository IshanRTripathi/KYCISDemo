package com.kycis.demo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kycis.demo.presentation.screens.*
import com.kycis.sdk.AI

object Routes {
    const val HOME = "home"
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
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier.fillMaxSize()
    ) {
        composable(Routes.HOME) {
            LaunchedEffect(Unit) { AI.setKycStep("new_home") }
            HomeScreen(
                onStartFlow = { navController.navigate(Routes.PHONE_ENTRY) }
            )
        }

        composable(Routes.PHONE_ENTRY) {
            LaunchedEffect(Unit) { AI.setKycStep("phone_entry") }
            PhoneEntryScreen(
                onBack = { navController.popBackStack() },
                onGetOtp = { phone ->
                    val encodedPhone = java.net.URLEncoder.encode(phone, "UTF-8")
                    navController.navigate(Routes.PHONE_OTP.replace("{phone}", encodedPhone))
                }
            )
        }

        composable(Routes.PHONE_OTP) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val decodedPhone = java.net.URLDecoder.decode(phone, "UTF-8")
            LaunchedEffect(Unit) { AI.setKycStep("phone_otp") }
            PhoneOtpScreen(
                phoneNumber = decodedPhone,
                onBack = { navController.popBackStack() },
                onVerify = {
                    navController.navigate(Routes.EMAIL_ENTRY)
                },
                onResendProvider = {}
            )
        }

        composable(Routes.EMAIL_ENTRY) {
            LaunchedEffect(Unit) { AI.setKycStep("email_entry") }
            EmailEntryScreen(
                onBack = { navController.popBackStack() },
                onGetOtp = { email ->
                    val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
                    navController.navigate(Routes.EMAIL_OTP.replace("{email}", encodedEmail))
                }
            )
        }

        composable(Routes.EMAIL_OTP) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val decodedEmail = java.net.URLDecoder.decode(email, "UTF-8")
            LaunchedEffect(Unit) { AI.setKycStep("email_otp") }
            EmailOtpScreen(
                email = decodedEmail,
                onBack = { navController.popBackStack() },
                onVerify = {
                    navController.navigate(Routes.PAN_DETAILS)
                },
                onResendProvider = {},
                onSkip = {
                    navController.navigate(Routes.PAN_DETAILS)
                }
            )
        }

        composable(Routes.PAN_DETAILS) {
            LaunchedEffect(Unit) { AI.setKycStep("pan_details") }
            PanDetailsScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.PERSONAL_DETAILS) }
            )
        }

        composable(Routes.PERSONAL_DETAILS) {
            LaunchedEffect(Unit) { AI.setKycStep("new_personal_details") }
            PersonalDetailsScreen(
                onBack = { navController.popBackStack() },
                onProceed = { navController.navigate(Routes.VERIFY_DOCUMENTS) }
            )
        }

        composable(Routes.VERIFY_DOCUMENTS) {
            LaunchedEffect(Unit) { AI.setKycStep("verify_documents") }
            VerifyDocumentsScreen(
                onBack = { navController.popBackStack() },
                onProceedWithAadhaar = { navController.navigate(Routes.DIGILOCKER_AADHAAR) },
                onOfflineProcess = { navController.navigate(Routes.UPLOAD_AADHAAR_FRONT) }
            )
        }

        composable(Routes.DIGILOCKER_AADHAAR) {
            LaunchedEffect(Unit) { AI.setKycStep("new_digilocker_aadhaar") }
            DigilockerAadhaarScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.SELFIE_CAPTURE) },
                onTryAnotherWay = { navController.navigate(Routes.UPLOAD_AADHAAR_FRONT) }
            )
        }

        composable(Routes.UPLOAD_AADHAAR_FRONT) {
            LaunchedEffect(Unit) { AI.setKycStep("upload_aadhaar_front") }
            UploadAadhaarScreen(
                isFront = true,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.UPLOAD_AADHAAR_BACK) }
            )
        }

        composable(Routes.UPLOAD_AADHAAR_BACK) {
            LaunchedEffect(Unit) { AI.setKycStep("upload_aadhaar_back") }
            UploadAadhaarScreen(
                isFront = false,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.SELFIE_CAPTURE) }
            )
        }

        composable(Routes.SELFIE_CAPTURE) {
            LaunchedEffect(Unit) { AI.setKycStep("new_selfie_capture") }
            SelfieCaptureScreen(
                onCaptured = { navController.navigate(Routes.SIGNATURE) }
            )
        }

        composable(Routes.SIGNATURE) {
            LaunchedEffect(Unit) { AI.setKycStep("new_signature") }
            SignatureScreen(
                onBack = { navController.popBackStack() },
                onSubmit = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
            )
        }
    }
}
