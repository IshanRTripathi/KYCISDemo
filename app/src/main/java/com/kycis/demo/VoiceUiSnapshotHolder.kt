package com.kycis.demo

import com.kycis.sdk.VoiceUiSnapshot

/**
 * In-memory UI field snapshot for voice context.
 * Fed automatically from [com.kycis.demo.kycis.KycisIntegration.reportComponentInput].
 */
object VoiceUiSnapshotHolder {
    private data class FieldMeta(
        val componentType: String,
        val semanticSlot: String,
    )

    private val fieldMetaById: Map<String, FieldMeta> = mapOf(
        "phone_field" to FieldMeta("phone_number", "phone"),
        "phone_otp_field" to FieldMeta("otp", "phone_otp"),
        "email_field" to FieldMeta("email", "email"),
        "email_otp_field" to FieldMeta("otp", "email_otp"),
        "pan_field" to FieldMeta("pan", "pan"),
        "dob_field" to FieldMeta("dob", "date_of_birth"),
        "terms_checkbox" to FieldMeta("checkbox", "terms_accepted"),
        "name_field" to FieldMeta("name", "full_name"),
        "father_name_field" to FieldMeta("name", "father_name"),
        "gender_field" to FieldMeta("dropdown", "gender"),
        "marital_status_field" to FieldMeta("dropdown", "marital_status"),
        "residency_status_field" to FieldMeta("dropdown", "residency_status"),
        "aadhaar_field" to FieldMeta("aadhaar", "aadhaar"),
        "aadhaar_front" to FieldMeta("camera", "aadhaar_front"),
        "aadhaar_back" to FieldMeta("camera", "aadhaar_back"),
        "selfie_field" to FieldMeta("camera", "selfie"),
        "signature_field" to FieldMeta("signature", "signature"),
        "verify_digilocker" to FieldMeta("button", "verify_digilocker"),
        "verify_manual" to FieldMeta("button", "verify_manual"),
    )

    private var version: Long = 0L
    private var currentScreen: String? = null
    private val fields: MutableMap<String, Pair<String, String?>> = linkedMapOf()

    @Synchronized
    fun setCurrentScreen(screen: String?) {
        if (screen != currentScreen) {
            currentScreen = screen
            fields.clear()
        } else {
            currentScreen = screen
        }
    }

    @Synchronized
    fun clear() {
        fields.clear()
    }

    @Synchronized
    fun upsertField(componentId: String, value: String?, componentType: String? = null) {
        if (componentId.isBlank()) return
        fields[componentId] = value.orEmpty() to componentType
    }

    @Synchronized
    fun buildSnapshot(): VoiceUiSnapshot {
        version += 1L
        val components = fields.map { (componentId, valueAndType) ->
            val (value, typeHint) = valueAndType
            val meta = fieldMetaById[componentId]
            mapOf(
                "component_id" to componentId,
                "screen" to currentScreen,
                "component_type" to (meta?.componentType ?: typeHint ?: "text_input"),
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
