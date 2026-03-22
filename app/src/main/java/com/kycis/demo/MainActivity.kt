package com.kycis.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.kycis.sdk.voice.VoiceCallPillOverlay
import com.kycis.sdk.voice.VoiceRoomConnector
import dagger.hilt.android.AndroidEntryPoint
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
                    voiceConnector.connect(result, activity = currentActivity) { room ->
                        voiceRoom = room
                        isMuted = false
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

    Box(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        KycNavGraph(navController = navController)

        if (voiceRoom == null) {
            FloatingActionButton(
                onClick = { AI.startAssistant() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.ContactSupport,
                    contentDescription = "Get AI help",
                )
            }
        }

        voiceRoom?.let { room ->
            VoiceCallPillOverlay(
                room = room,
                isMuted = isMuted,
                onMuteToggle = { muted ->
                    isMuted = muted
                    scope.launch {
                        room.localParticipant.setMicrophoneEnabled(!muted)
                    }
                },
                onEndCall = {
                    voiceConnector.disconnect()
                    voiceRoom = null
                },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}