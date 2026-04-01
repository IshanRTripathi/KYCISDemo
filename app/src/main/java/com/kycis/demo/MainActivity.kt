package com.kycis.demo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.kycis.demo.navigation.KycNavGraph
import com.kycis.demo.presentation.theme.KycDemoTheme
import com.kycis.sdk.AI
import com.kycis.sdk.voice.VoiceFabConfig
import com.kycis.sdk.voice.VoiceFabHost
import com.kycis.sdk.voice.VoiceFabType
import com.kycis.sdk.voice.VoiceRoomConnector
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
    var voiceRoom by remember { mutableStateOf<Room?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var stopNotified by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentActivity = context as? FragmentActivity

    val voiceConnector = remember {
        VoiceRoomConnector(context)
    }

    DisposableEffect(Unit) {
        AI.setVoiceSessionListener { result ->
            if (result.isValid) {
                try {
                    voiceConnector.connect(
                        result,
                        activity = currentActivity,
                        onConnectionFailed = { msg ->
                            Log.e("KYCIS", "Voice LiveKit connect failed: $msg")
                            Toast.makeText(
                                context,
                                "Voice connection failed. Use a reachable LiveKit URL (emulator: host IP, not localhost). $msg",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                    ) { room ->
                        voiceRoom = room
                        isMuted = false
                        stopNotified = false
                    }
                } catch (_: SecurityException) {
                    // RECORD_AUDIO not granted – user should grant in settings
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

    Box(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        KycNavGraph(navController = navController)

        VoiceFabHost(
            room = voiceRoom,
            isMuted = isMuted,
            onMuteToggle = { muted ->
                isMuted = muted
                voiceRoom?.let { room ->
                    scope.launch {
                        room.localParticipant.setMicrophoneEnabled(!muted)
                    }
                }
            },
            onEndCall = {
                // Notify backend before tearing down the room so we do not race with
                // RoomEvent.Disconnected (which would otherwise issue a second stop).
                if (!stopNotified) {
                    stopNotified = true
                    AI.stopAssistant()
                }
                voiceConnector.disconnect()
                voiceRoom = null
            },
            onStartClick = { AI.startAssistant() },
            fabType = VoiceFabType.PREMIUM,
            config = VoiceFabConfig(
                assistantName = "Ishan",
                transcriptBackground = TranscriptBackground.DotGrid,
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        )
    }
}