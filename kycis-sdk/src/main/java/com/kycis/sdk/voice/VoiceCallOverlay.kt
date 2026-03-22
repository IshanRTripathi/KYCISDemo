package com.kycis.sdk.voice

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.livekit.android.annotations.Beta
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room

/**
 * Voice call overlay with activity indicator and transcripts.
 *
 * Pass the [Room] from [VoiceRoomConnector.connect]'s onConnected callback.
 * When disconnected, pass null to hide.
 *
 * @param room The LiveKit room when connected, null when disconnected.
 * @param isMuted Current mute state.
 * @param onMuteToggle Called when user taps mute (pass desired muted state).
 * @param onEndCall Called when user taps end call.
 */
@OptIn(Beta::class)
@Composable
fun VoiceCallOverlay(
    room: Room?,
    isMuted: Boolean,
    onMuteToggle: (Boolean) -> Unit,
    onEndCall: () -> Unit,
) {
    var voiceActivity by remember { mutableStateOf(VoiceActivity.IDLE) }
    val transcripts = remember { mutableStateListOf<TranscriptEntry>() }

    LaunchedEffect(room) {
        if (room == null) {
            voiceActivity = VoiceActivity.IDLE
            transcripts.clear()
            return@LaunchedEffect
        }
        room.events.collect { event: RoomEvent ->
            when (event) {
                is RoomEvent.ActiveSpeakersChanged -> {
                    val localIdentity = room.localParticipant.identity
                    val hasLocal = event.speakers.any { it.identity == localIdentity }
                    val hasRemote = event.speakers.any { it.identity != localIdentity }
                    voiceActivity = when {
                        hasLocal && hasRemote -> VoiceActivity.AGENT_SPEAKING
                        hasLocal -> VoiceActivity.USER_SPEAKING
                        hasRemote -> VoiceActivity.AGENT_SPEAKING
                        else -> VoiceActivity.IDLE
                    }
                }
                is RoomEvent.TranscriptionReceived -> {
                    val localIdentity = room.localParticipant.identity
                    val isUser = event.participant?.identity == localIdentity
                    val role = if (isUser) TranscriptRole.USER else TranscriptRole.AGENT
                    event.transcriptionSegments
                        .filter { it.text.isNotBlank() }
                        .forEach { seg ->
                            transcripts.add(
                                TranscriptEntry(
                                    role = role,
                                    text = seg.text.trim(),
                                    time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                        .format(java.util.Date(seg.lastReceivedTime)),
                                ),
                            )
                        }
                    if (transcripts.size > 50) transcripts.removeAt(0)
                }
                is RoomEvent.Disconnected -> {
                    voiceActivity = VoiceActivity.IDLE
                    transcripts.clear()
                }
                else -> {}
            }
        }
    }

    if (room == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        if (voiceActivity != VoiceActivity.IDLE) {
            VoiceActivityIndicator(
                activity = voiceActivity,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (transcripts.isNotEmpty()) {
            Text(
                "Transcripts",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                transcripts.forEach { t ->
                    TranscriptLine(entry = t)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onMuteToggle(!isMuted) }) {
                Text(if (isMuted) "Unmute" else "Mute")
            }
            TextButton(onClick = onEndCall) {
                Text("End Call", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun VoiceActivityIndicator(
    activity: VoiceActivity,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(scale)
                .background(
                    color = when (activity) {
                        VoiceActivity.USER_SPEAKING -> Color(0xFF22C55E)
                        VoiceActivity.AGENT_SPEAKING -> Color(0xFF3B82F6)
                        else -> Color.Gray
                    },
                    shape = CircleShape,
                ),
        )
        Text(
            text = when (activity) {
                VoiceActivity.USER_SPEAKING -> "You are speaking…"
                VoiceActivity.AGENT_SPEAKING -> "Agent is speaking…"
                else -> ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TranscriptLine(entry: TranscriptEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = when (entry.role) {
                    TranscriptRole.USER -> Color(0xFF22C55E).copy(alpha = 0.12f)
                    TranscriptRole.AGENT -> Color(0xFF3B82F6).copy(alpha = 0.12f)
                },
                shape = MaterialTheme.shapes.small,
            )
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = entry.role.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
