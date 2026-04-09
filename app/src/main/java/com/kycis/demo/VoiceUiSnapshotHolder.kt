package com.kycis.demo

import com.kycis.sdk.VoiceUiSnapshot

object VoiceUiSnapshotHolder {
    private var version: Long = 0L
    private var currentScreen: String? = null
    private val fields: MutableMap<String, String> = linkedMapOf()

    @Synchronized
    fun setCurrentScreen(screen: String?) {
        currentScreen = screen
    }

    @Synchronized
    fun upsertField(componentId: String, value: String?) {
        if (componentId.isBlank()) return
        fields[componentId] = value.orEmpty()
    }

    @Synchronized
    fun buildSnapshot(): VoiceUiSnapshot {
        version += 1L
        val components = fields.map { (componentId, value) ->
            mapOf(
                "component_id" to componentId,
                "screen" to currentScreen,
                "component_type" to "text_input",
                "is_empty" to value.isBlank(),
                "field_status" to if (value.isBlank()) "REQUIRED" else "FILLED",
                "display" to mapOf("value" to value),
                "data" to mapOf("raw" to null),
            )
        }
        return VoiceUiSnapshot(
            snapshotVersion = version,
            screen = currentScreen,
            components = components,
            idempotencyKey = "demo-$version",
        )
    }
}
