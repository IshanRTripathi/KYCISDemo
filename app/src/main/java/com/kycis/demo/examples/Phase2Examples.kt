package com.kycis.demo.examples

import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.kycis.sdk.AI
import com.kycis.sdk.core.AgentEventType
import kotlinx.coroutines.launch

/**
 * Examples demonstrating Phase 2 enhancements (SDK v2.0):
 * - Agent Event Callbacks
 * - Dynamic Popup Support
 */
object Phase2Examples {

    /**
     * Example 1: Basic agent event listener
     * Listen for voice agent connection lifecycle
     */
    fun setupAgentEventListener() {
        AI.setAgentEventListener { event ->
            when (event.type) {
                AgentEventType.CONNECTED -> {
                    println("Voice agent connected at ${event.timestamp}")
                    // Update UI to show active state
                }
                AgentEventType.DISCONNECTED -> {
                    val duration = event.metadata.callDuration
                    println("Voice agent disconnected. Call duration: ${duration}ms")
                    // Update UI to show idle state
                    // Log analytics
                }
                AgentEventType.POPUP_VISIBLE -> {
                    val message = event.metadata.message
                    println("Show popup: $message")
                    // Display popup to user
                }
            }
        }
    }

    /**
     * Example 2: Track call duration analytics
     */
    fun trackCallDurationAnalytics() {
        AI.setAgentEventListener { event ->
            if (event.type == AgentEventType.DISCONNECTED) {
                val durationMs = event.metadata.callDuration ?: 0L
                val durationSeconds = durationMs / 1000
                
                AI.trackAnalytics("voice_call_completed", mapOf(
                    "duration_seconds" to durationSeconds,
                    "duration_ms" to durationMs,
                    "timestamp" to event.timestamp
                ))
            }
        }
    }

    /**
     * Example 3: UI state management with agent events
     */
    @Composable
    fun VoiceAssistantUI() {
        var isConnected by remember { mutableStateOf(false) }
        var callDuration by remember { mutableStateOf(0L) }
        
        DisposableEffect(Unit) {
            AI.setAgentEventListener { event ->
                when (event.type) {
                    AgentEventType.CONNECTED -> {
                        isConnected = true
                    }
                    AgentEventType.DISCONNECTED -> {
                        isConnected = false
                        callDuration = event.metadata.callDuration ?: 0L
                    }
                    AgentEventType.POPUP_VISIBLE -> {
                        // Handle popup
                    }
                }
            }
            
            onDispose {
                AI.setAgentEventListener(null)
            }
        }
        
        // UI based on connection state
        if (isConnected) {
            // Show active call UI
        } else if (callDuration > 0) {
            // Show call ended UI with duration
        } else {
            // Show idle UI
        }
    }

    /**
     * Example 4: Check for dynamic popup on screen change
     */
    fun checkPopupOnScreenChange(lifecycleScope: kotlinx.coroutines.CoroutineScope) {
        // Set up listener first
        AI.setAgentEventListener { event ->
            if (event.type == AgentEventType.POPUP_VISIBLE) {
                val message = event.metadata.message ?: return@setAgentEventListener
                val trigger = event.metadata.trigger
                
                // Show popup dialog
                showPopupDialog(message)
                
                // Track popup shown
                AI.trackAnalytics("popup_shown", mapOf(
                    "message" to message,
                    "trigger" to (trigger ?: "unknown")
                ))
            }
        }
        
        // Check for popup when navigating to new screen
        lifecycleScope.launch {
            AI.setKycStep("verification")
            AI.checkForDynamicPopup()
        }
    }

    /**
     * Example 5: Popup with user interaction tracking
     */
    fun trackPopupInteraction() {
        AI.setAgentEventListener { event ->
            if (event.type == AgentEventType.POPUP_VISIBLE) {
                val message = event.metadata.message ?: return@setAgentEventListener
                
                showPopupWithCallbacks(
                    message = message,
                    onAccept = {
                        AI.trackAnalytics("popup_accepted", mapOf(
                            "message" to message,
                            "action" to "start_assistant"
                        ))
                        AI.startAssistant()
                    },
                    onDismiss = {
                        AI.trackAnalytics("popup_dismissed", mapOf(
                            "message" to message
                        ))
                    }
                )
            }
        }
    }

    /**
     * Example 6: Periodic popup checking
     */
    fun setupPeriodicPopupCheck(lifecycleScope: kotlinx.coroutines.CoroutineScope) {
        lifecycleScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000) // Check every 60 seconds
                AI.checkForDynamicPopup()
            }
        }
    }

    /**
     * Example 7: Combined agent events and analytics
     */
    fun comprehensiveEventTracking() {
        AI.setAgentEventListener { event ->
            when (event.type) {
                AgentEventType.CONNECTED -> {
                    AI.trackAnalytics("voice_session_started", mapOf(
                        "timestamp" to event.timestamp,
                        "screen" to getCurrentScreen()
                    ))
                }
                AgentEventType.DISCONNECTED -> {
                    val duration = event.metadata.callDuration ?: 0L
                    AI.trackAnalytics("voice_session_ended", mapOf(
                        "duration_ms" to duration,
                        "duration_seconds" to (duration / 1000),
                        "screen" to getCurrentScreen()
                    ))
                }
                AgentEventType.POPUP_VISIBLE -> {
                    AI.trackAnalytics("popup_displayed", mapOf(
                        "message" to event.metadata.message,
                        "trigger" to event.metadata.trigger,
                        "screen" to getCurrentScreen()
                    ))
                }
            }
        }
    }

    // Helper functions (implement in your app)
    private fun showPopupDialog(message: String) {
        // Show Android dialog or Compose AlertDialog
    }

    private fun showPopupWithCallbacks(
        message: String,
        onAccept: () -> Unit,
        onDismiss: () -> Unit
    ) {
        // Show dialog with callbacks
    }

    private fun getCurrentScreen(): String {
        // Return current screen name
        return "unknown"
    }
}
