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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.kycis.demo.navigation.KycNavGraph
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.sdk.AI
import com.kycis.sdk.core.RuntimePolicy
import com.kycis.sdk.ui.EmbedProvider
import com.kycis.sdk.ui.KycEvent
import com.kycis.sdk.ui.KycisOptions
import com.kycis.sdk.ui.rememberEmbedProviderState
import com.kycis.sdk.ui.useKycis
import com.kycis.sdk.voice.VoiceFabConfig
import com.kycis.sdk.voice.VoiceFabHost
import com.kycis.sdk.voice.VoiceFabType
import com.kycis.sdk.voice.VoiceRoomConnector
import com.kycis.sdk.voice.FabPosition
import com.kycis.sdk.voice.TranscriptBackground
import dagger.hilt.android.AndroidEntryPoint
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Manually bind activity to SDK since lifecycle callbacks may fire before SDK init
        AI.bindActivity(this)
        setContent {
            KycDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceAssistantContent()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java 13, kept for Hilt compatibility")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (AI.onRequestPermissionsResult(requestCode, permissions, grantResults)) return
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

@Composable
private fun VoiceAssistantContent() {
    val context = LocalContext.current
    val currentActivity = context as? FragmentActivity

    var voiceRoom by remember { mutableStateOf<Room?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var stopNotified by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val voiceConnector = remember {
        VoiceRoomConnector(context)
    }

    val providerState = rememberEmbedProviderState()
    val navController = rememberNavController()

    // Permission launcher for RECORD_AUDIO
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

    // Request permission on first launch if not granted
    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    navController.addOnDestinationChangedListener { _, destination, _ ->
        providerState.updateRoute(destination.route ?: "")
    }

    val kycis = useKycis(
        application = context.applicationContext as android.app.Application,
        options = KycisOptions(
            apiKey = "demo-api-key",
            userId = "demo-user",
            policy = RuntimePolicy(
                backendBaseUrl = "http://10.0.2.2:8000/v1",
                componentInputHintsMasked = false,  // Send unmasked values for demo/debug
            ),
            attachToLifecycle = true,
            autoTrackScreen = true,
            autoCheckPopup = true,
            onPopup = { popup ->
                if (popup.show) {
                    Toast.makeText(context, popup.message, Toast.LENGTH_LONG).show()
                }
            }
        ),
        onStatusChange = { status ->
            android.util.Log.d("KYCIS", "Status changed: ${status.code}")
        }
    )

    DisposableEffect(Unit) {
        AI.setVoiceSessionListener { result ->
            if (result.isValid) {
                try {
                    voiceConnector.connect(
                        result = result,
                        activity = currentActivity,
                        onConnectionFailed = { msg ->
                            Log.e("KYCIS", "Voice LiveKit connect failed: $msg")
                            Toast.makeText(
                                context,
                                "Voice connection failed. $msg",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                        onConnected = { room ->
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
            AI.setVoiceSessionListener(null)
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
                    AI.stopAssistant()
                }
            }
        }
    }

    LaunchedEffect(kycis.isReady) {
        if (kycis.isReady) {
            providerState.setOnPopupCheck {
                AI.checkForDynamicPopup()
            }
            KycEvent.identity(userId = "demo-user")
            AI.setFlow("onboarding")
        }
    }

    when {
        kycis.isLoading -> {
            android.util.Log.d("KYCIS", "MainActivity: showing loading state")
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        kycis.isError -> {
            android.util.Log.e("KYCIS", "MainActivity: showing error state: ${kycis.error}")
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.material3.Text(
                    text = "Error: ${kycis.error}",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        kycis.isReady -> {
            android.util.Log.d("KYCIS", "MainActivity: showing ready state")
            Box(modifier = Modifier.fillMaxSize()) {
                EmbedProvider(
                    state = providerState,
                    flowName = "onboarding",
                    includeRoutes = listOf("phone_entry", "phone_otp", "email_entry", "email_otp", "pan_details", "personal_details", "verify_documents", "digilocker_aadhaar", "upload_aadhaar_front", "upload_aadhaar_back", "selfie_capture", "signature", "sdk_diagnostics"),
                    excludeRoutes = listOf("home"),
                    autoTrackScreen = true,
                    autoCheckPopup = true
                ) {
                    KycNavGraph(navController = navController)
                }

                VoiceFabHost(
                    room = voiceRoom,
                    isMuted = isMuted,
                    initialPosition = FabPosition.BOTTOM_START,
                    onMuteToggle = { muted ->
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
                            AI.stopAssistant()
                        }
                        voiceConnector.disconnect()
                        voiceRoom = null
                    },
                    onStartClick = {
                        android.util.Log.d("KYCIS", "FAB: onStartClick called, hasAudioPermission=$hasAudioPermission")
                        if (hasAudioPermission) {
                            AI.startAssistant()
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
