package com.kycis.sdk.voice

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kycis.sdk.VoiceSessionResult
import com.kycis.sdk.core.PermissionRequestFragment
import com.kycis.sdk.core.VoicePermissionCallbackHolder
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Connects to a LiveKit voice room using credentials from [VoiceSessionResult].
 * Publishes microphone audio. Subscribed agent audio is played automatically.
 *
 * Usage (with permission request – recommended when you have an Activity):
 * ```
 * val connector = VoiceRoomConnector(context)
 * AI.setVoiceSessionListener { result ->
 *     connector.connect(result, activity = myFragmentActivity) { room ->
 *         // Connected. Show call UI.
 *     }
 * }
 * ```
 *
 * Usage (without activity – permission must be granted before calling):
 * ```
 * connector.connect(result) { room -> ... }
 * // Throws SecurityException if RECORD_AUDIO not granted.
 * ```
 */
class VoiceRoomConnector(
    private val appContext: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private var currentRoom: Room? = null

    /**
     * Connect to the LiveKit room and publish mic. Calls [onConnected] when ready.
     *
     * @param result Session credentials from backend.
     * @param activity Optional FragmentActivity. When provided and RECORD_AUDIO is not granted,
     *        the permission will be requested before connecting. Use this when your flow
     *        may bypass the SDK's startAssistantSession (e.g. custom "Start call" button).
     * @param onConnected Called when connected. Use for mute: room.localParticipant.setMicrophoneEnabled(false)
     * @throws SecurityException if RECORD_AUDIO is not granted and [activity] is null.
     */
    fun connect(
        result: VoiceSessionResult,
        activity: FragmentActivity? = null,
        onConnected: (Room) -> Unit = {},
    ) {
        if (!result.isValid) return
        val hasPermission = ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            doConnect(result, onConnected)
            return
        }

        if (activity != null) {
            if (activity.supportFragmentManager.findFragmentByTag(PermissionRequestFragment.TAG) != null) {
                return // Already requesting
            }
            VoicePermissionCallbackHolder.callback = { granted ->
                if (granted) doConnect(result, onConnected)
            }
            activity.runOnUiThread {
                activity.supportFragmentManager.beginTransaction()
                    .add(PermissionRequestFragment(), PermissionRequestFragment.TAG)
                    .commit()
            }
        } else {
            throw SecurityException(
                "RECORD_AUDIO permission required. Either use the SDK flow (trigger → confirm) so permission is requested, " +
                    "or pass activity to connect(): connect(result, activity = myActivity) { ... }"
            )
        }
    }

    private fun doConnect(result: VoiceSessionResult, onConnected: (Room) -> Unit) {
        scope.launch {
            try {
                val room = LiveKit.create(appContext)
                currentRoom = room
                room.connect(result.livekitUrl, result.token)
                room.localParticipant.setMicrophoneEnabled(true)
                onConnected(room)
            } catch (e: Exception) {
                currentRoom = null
                throw e
            }
        }
    }

    /**
     * Disconnect from the current room.
     */
    fun disconnect() {
        scope.launch {
            try {
                currentRoom?.disconnect()
            } catch (_: Exception) { }
            currentRoom = null
        }
    }

    /**
     * Release resources. Call when the connector is no longer needed.
     */
    fun release() {
        disconnect()
        scope.cancel()
    }
}
