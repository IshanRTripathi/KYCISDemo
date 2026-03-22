package com.kycis.sdk.voice

/**
 * Voice activity state for UI indication.
 */
enum class VoiceActivity {
    IDLE,
    USER_SPEAKING,
    AGENT_SPEAKING,
}

/**
 * A single transcript entry for display.
 */
data class TranscriptEntry(
    val role: TranscriptRole,
    val text: String,
    val time: String,
)

enum class TranscriptRole {
    USER,
    AGENT,
}
