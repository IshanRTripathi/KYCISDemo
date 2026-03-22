package com.kycis.sdk.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.livekit.android.annotations.Beta
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlin.math.roundToInt

/**
 * Floating pill overlay for voice calls with expandable transcripts.
 *
 * - Draggable: User can drag the pill anywhere on screen.
 * - Compact mode: Shows status, mute, expand, and end buttons.
 * - Expanded mode: Toggle via expand button to show transcript list.
 *
 * Pass the [Room] from [VoiceRoomConnector.connect]'s onConnected callback.
 * Use [modifier] to position the pill, e.g. `Modifier.align(Alignment.BottomEnd)` inside a Box.
 */
@OptIn(Beta::class)
@Composable
fun VoiceCallPillOverlay(
    room: Room?,
    isMuted: Boolean,
    onMuteToggle: (Boolean) -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var voiceActivity by remember { mutableStateOf(VoiceActivity.IDLE) }
    val transcripts = remember { mutableStateListOf<TranscriptEntry>() }
    var isExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(room) {
        if (room == null) {
            voiceActivity = VoiceActivity.IDLE
            transcripts.clear()
            isExpanded = false
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

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Compact pill
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(28.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VoiceActivityIndicator(activity = voiceActivity, compact = true)
                Text(
                    text = "AI Agent",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = { onMuteToggle(!isMuted) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Hide transcripts" else "Show transcripts",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End call",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(16.dp),
                        )
                        .padding(12.dp)
                        .heightIn(max = 240.dp),
                ) {
                    Text(
                        "Transcripts",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (transcripts.isEmpty()) {
                        Text(
                            "No transcripts yet…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            transcripts.forEach { t ->
                                TranscriptLine(entry = t)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceActivityIndicator(
    activity: VoiceActivity,
    compact: Boolean = false,
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

    val size = if (compact) 8.dp else 10.dp
    Box(
        modifier = modifier
            .size(size)
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
