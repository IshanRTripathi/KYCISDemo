package com.kycis.demo

import com.kycis.sdk.VoiceUiSnapshot

object VoiceUiSnapshotHolder {
    private data class FieldMeta(
        val componentType: String,
        val semanticSlot: String,
    )

    private val fieldMetaById: Map<String, FieldMeta> = mapOf(
        "name_field" to FieldMeta(componentType = "name", semanticSlot = "full_name"),
        "father_name_field" to FieldMeta(componentType = "name", semanticSlot = "father_name"),
        "gender_field" to FieldMeta(componentType = "dropdown", semanticSlot = "gender"),
        "marital_status_field" to FieldMeta(componentType = "dropdown", semanticSlot = "marital_status"),
        "residency_status_field" to FieldMeta(componentType = "dropdown", semanticSlot = "residency_status"),
    )

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
            val meta = fieldMetaById[componentId]
            mapOf(
                "component_id" to componentId,
                "screen" to currentScreen,
                "component_type" to (meta?.componentType ?: "text_input"),
                "semantic_slot" to meta?.semanticSlot,
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
