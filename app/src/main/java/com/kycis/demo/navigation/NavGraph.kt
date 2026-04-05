package com.kycis.demo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

object Routes {
    const val HOME = "home"
    const val SDK_DIAGNOSTICS = "sdk_diagnostics"
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
            HomeScreen(
                onStartFlow = {
                    android.util.Log.d("KYCIS", "NavGraph: onStartFlow called, navigating to phone_entry")
                    navController.navigate(Routes.PHONE_ENTRY)
                },
                onOpenSdkHarness = {
                    navController.navigate(Routes.SDK_DIAGNOSTICS)
                },
            )
        }

        composable(Routes.SDK_DIAGNOSTICS) {
            SdkDiagnosticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PHONE_ENTRY) {
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
            PanDetailsScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.PERSONAL_DETAILS) }
            )
        }

        composable(Routes.PERSONAL_DETAILS) {
            PersonalDetailsScreen(
                onBack = { navController.popBackStack() },
                onProceed = { navController.navigate(Routes.VERIFY_DOCUMENTS) }
            )
        }

        composable(Routes.VERIFY_DOCUMENTS) {
            VerifyDocumentsScreen(
                onBack = { navController.popBackStack() },
                onProceedWithAadhaar = { navController.navigate(Routes.DIGILOCKER_AADHAAR) },
                onOfflineProcess = { navController.navigate(Routes.UPLOAD_AADHAAR_FRONT) }
            )
        }

        composable(Routes.DIGILOCKER_AADHAAR) {
            DigilockerAadhaarScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.SELFIE_CAPTURE) },
                onTryAnotherWay = { navController.navigate(Routes.UPLOAD_AADHAAR_FRONT) }
            )
        }

        composable(Routes.UPLOAD_AADHAAR_FRONT) {
            UploadAadhaarScreen(
                isFront = true,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.UPLOAD_AADHAAR_BACK) }
            )
        }

        composable(Routes.UPLOAD_AADHAAR_BACK) {
            UploadAadhaarScreen(
                isFront = false,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.SELFIE_CAPTURE) }
            )
        }

        composable(Routes.SELFIE_CAPTURE) {
            SelfieCaptureScreen(
                onCaptured = { navController.navigate(Routes.SIGNATURE) }
            )
        }

        composable(Routes.SIGNATURE) {
            SignatureScreen(
                onBack = { navController.popBackStack() },
                onSubmit = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
            )
        }
    }
}
