package com.kycis.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.kycis.demo.kycis.KycisHandlers
import com.kycis.demo.kycis.KycisIntegration
import com.kycis.demo.kycis.KycisStepMap
import com.kycis.demo.navigation.KycNavGraph
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.sdk.core.DynamicPopup
import com.kycis.sdk.ui.EmbedProvider
import com.kycis.sdk.ui.KycisOptions
import com.kycis.sdk.ui.rememberEmbedProviderState
import com.kycis.sdk.ui.useKycis
import com.kycis.sdk.voice.FabPosition
import com.kycis.sdk.voice.TranscriptBackground
import com.kycis.sdk.voice.VoiceFabConfig
import com.kycis.sdk.voice.VoiceFabHost
import com.kycis.sdk.voice.VoiceFabType
import com.kycis.sdk.voice.VoiceRoomConnector
import dagger.hilt.android.AndroidEntryPoint
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bind after Application init — lifecycle may fire before Activity is ready
        KycisIntegration.bindActivity(this)
        setContent {
            KycDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceAssistantContent(
                        onBackendUrlSaved = { recreate() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        KycisIntegration.refreshVoiceAudioFocus()
    }

    @Deprecated("Deprecated in Java 13, kept for Hilt compatibility")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (KycisIntegration.onRequestPermissionsResult(requestCode, permissions, grantResults)) return
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

@Composable
private fun VoiceAssistantContent(
    onBackendUrlSaved: () -> Unit,
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val currentActivity = context as? FragmentActivity
    var backendBaseUrl by remember { mutableStateOf(BackendUrlStore.get(context)) }

    var voiceRoom by remember { mutableStateOf<Room?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var stopNotified by remember { mutableStateOf(false) }
    var pendingPopup by remember { mutableStateOf<DynamicPopup?>(null) }
    val scope = rememberCoroutineScope()

    val voiceConnector = remember {
        VoiceRoomConnector(context)
    }

    val providerState = rememberEmbedProviderState()
    val navController = rememberNavController()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required for voice assistant", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    navController.addOnDestinationChangedListener { _, destination, _ ->
        val step = KycisStepMap.normalize(destination.route)
        providerState.updateRoute(step)
        VoiceUiSnapshotHolder.setCurrentScreen(step)
        if (step.isNotBlank() && step != "home") {
            KycisIntegration.trackStepStarted(step)
        }
    }

    // Demo-shell SDK UI: useKycis skips AI.init when already done in Application
    val kycis = useKycis(
        application = application,
        options = KycisOptions(
            apiKey = KycisIntegration.demoApiKey(),
            userId = KycisIntegration.demoUserId(),
            policy = KycisIntegration.demoPolicy(application),
            attachToLifecycle = true,
            autoTrackScreen = true,
            autoCheckPopup = true,
            onPopup = { popup ->
                if (popup.show && pendingPopup == null) {
                    KycisHandlers.logPopupShown(popup)
                    pendingPopup = popup
                }
            },
        ),
        onStatusChange = { status ->
            KycisHandlers.onStatusChange(status.code.toString())
        }
    )

    DisposableEffect(Unit) {
        KycisIntegration.setVoiceSessionListener { result ->
            if (KycisHandlers.isValidVoiceSession(result)) {
                try {
                    voiceConnector.connect(
                        result = result,
                        activity = currentActivity,
                        onConnectionFailed = { msg ->
                            KycisHandlers.logVoiceConnectFailed(context, msg)
                        },
                        onConnected = { room ->
                            voiceConnector.setHostWantsMicOn(true)
                            voiceRoom = room
                            isMuted = false
                            stopNotified = false
                        },
                    )
                } catch (_: SecurityException) {
                    // RECORD_AUDIO not granted
                }
            }
        }
        onDispose {
            KycisHandlers.onSdkTeardown()
            KycisIntegration.setVoiceSessionListener(null)
            voiceConnector.release()
        }
    }

    LaunchedEffect(voiceRoom) {
        val room = voiceRoom ?: return@LaunchedEffect
        room.events.collect { event: RoomEvent ->
            if (event is RoomEvent.Disconnected) {
                voiceRoom = null
                if (!stopNotified) {
                    stopNotified = true
                    KycisIntegration.stopAssistant()
                }
            }
        }
    }

    LaunchedEffect(kycis.isReady) {
        if (kycis.isReady) {
            KycisIntegration.setVoiceUiSnapshotProvider { VoiceUiSnapshotHolder.buildSnapshot() }
            providerState.setOnPopupCheck {
                KycisIntegration.checkForDynamicPopup()
            }
            KycisHandlers.onSdkReady()
        }
    }

    pendingPopup?.let { popup ->
        AlertDialog(
            onDismissRequest = {
                KycisHandlers.onPopupDismissed(popup)
                pendingPopup = null
            },
            title = { Text("Need a hand?") },
            text = {
                Text(
                    buildString {
                        append(popup.message)
                        popup.popupReasonCode?.let { append("\n\n[$it]") }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        KycisHandlers.onPopupAccepted(popup)
                        pendingPopup = null
                    }
                ) { Text("Talk to assistant") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        KycisHandlers.onPopupDismissed(popup)
                        pendingPopup = null
                    }
                ) { Text("Not now") }
            },
        )
    }

    when {
        kycis.isLoading -> {
            Log.d("KYCIS", "MainActivity: showing loading state")
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        kycis.isError -> {
            Log.e("KYCIS", "MainActivity: showing error state: ${kycis.error}")
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Error: ${kycis.error}",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        kycis.isReady -> {
            Log.d("KYCIS", "MainActivity: showing ready state")
            Box(modifier = Modifier.fillMaxSize()) {
                EmbedProvider(
                    state = providerState,
                    flowName = "onboarding",
                    includeRoutes = listOf(
                        "phone_entry", "phone_otp", "email_entry", "email_otp",
                        "pan_details", "personal_details", "verify_documents",
                        "digilocker_aadhaar", "upload_aadhaar_front", "upload_aadhaar_back",
                        "selfie_capture", "signature", "sdk_diagnostics",
                    ),
                    excludeRoutes = listOf("home"),
                    autoTrackScreen = true,
                    autoCheckPopup = true,
                ) {
                    KycNavGraph(
                        navController = navController,
                        backendBaseUrl = backendBaseUrl,
                        onBackendUrlSaved = { savedUrl ->
                            backendBaseUrl = savedUrl
                            onBackendUrlSaved()
                        },
                    )
                }

                VoiceFabHost(
                    room = voiceRoom,
                    isMuted = isMuted,
                    initialPosition = FabPosition.BOTTOM_START,
                    onMuteToggle = { muted ->
                        voiceConnector.setHostWantsMicOn(!muted)
                        isMuted = muted
                        voiceRoom?.let { room ->
                            scope.launch {
                                room.localParticipant.setMicrophoneEnabled(!muted)
                            }
                        }
                    },
                    onEndCall = {
                        if (!stopNotified) {
                            stopNotified = true
                            KycisIntegration.stopAssistant()
                        }
                        voiceConnector.disconnect()
                        voiceRoom = null
                    },
                    onStartClick = {
                        Log.d("KYCIS", "FAB: onStartClick called, hasAudioPermission=$hasAudioPermission")
                        if (hasAudioPermission) {
                            KycisIntegration.startAssistant()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    fabType = VoiceFabType.PREMIUM,
                    config = VoiceFabConfig(
                        assistantName = "Ishan",
                        transcriptBackground = TranscriptBackground.DotGrid,
                        fabLottieResId = R.raw.fab_animated,
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                )
            }
        }
    }
}
