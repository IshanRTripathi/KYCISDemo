package com.kycis.sdk.core

import java.time.Instant

internal data class PassiveSnapshot(
    val currentScreen: String?,
    val timeSpentSeconds: Int?,
    val idleSeconds: Int?,
)

internal class PassiveTracker {
    private var currentScreen: String? = null
    private var screenEnteredAt: Long? = null
    private var lastInteractionAt: Long? = null

    fun onScreenChanged(screen: String) {
        val now = Instant.now().epochSecond
        currentScreen = screen
        screenEnteredAt = now
        lastInteractionAt = now
    }

    fun onInteraction() {
        lastInteractionAt = Instant.now().epochSecond
    }

    fun snapshot(): PassiveSnapshot {
        val now = Instant.now().epochSecond
        val timeSpent = screenEnteredAt?.let { (now - it).toInt().coerceAtLeast(0) }
        val idle = lastInteractionAt?.let { (now - it).toInt().coerceAtLeast(0) }
        return PassiveSnapshot(
            currentScreen = currentScreen,
            timeSpentSeconds = timeSpent,
            idleSeconds = idle,
        )
    }
}
