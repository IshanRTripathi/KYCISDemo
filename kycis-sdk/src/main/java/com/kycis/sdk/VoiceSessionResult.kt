package com.kycis.sdk

/**
 * Result of starting a voice assistant session from the backend.
 * Use [token], [livekitUrl], and [livekitRoom] to connect to the LiveKit room
 * and participate in the voice call.
 */
data class VoiceSessionResult(
    val token: String,
    val livekitUrl: String,
    val livekitRoom: String,
) {
    val isValid: Boolean
        get() = token.isNotBlank() && livekitUrl.isNotBlank() && livekitRoom.isNotBlank()
}
