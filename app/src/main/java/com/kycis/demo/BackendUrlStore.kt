package com.kycis.demo

import android.content.Context

object BackendUrlStore {
    private const val PREFS_NAME = "kycis_demo_prefs"
    private const val KEY_BACKEND_BASE_URL = "backend_base_url"

    /** Emulator → host machine loopback (FastAPI default :8000). */
    const val PRESET_EMULATOR_LOCAL = "http://10.0.2.2:8000"

    /** Physical device on same LAN — edit to your host IP if needed. */
    const val PRESET_DEVICE_LOCAL = "http://192.168.1.1:8000"

    /** Legacy shared ALB (explicit prod-like preset, not the default). */
    const val PRESET_PROD_ALB = "http://prod-alb-1551985914.ap-south-1.elb.amazonaws.com"

    private const val DEFAULT_BACKEND_BASE_URL = PRESET_EMULATOR_LOCAL

    val presets: List<Pair<String, String>> = listOf(
        "Emulator → localhost:8000" to PRESET_EMULATOR_LOCAL,
        "Device LAN (edit IP)" to PRESET_DEVICE_LOCAL,
        "Prod ALB" to PRESET_PROD_ALB,
    )

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_BACKEND_BASE_URL, DEFAULT_BACKEND_BASE_URL)
        return normalizeBaseUrl(raw ?: DEFAULT_BACKEND_BASE_URL)
    }

    fun save(context: Context, url: String): String {
        val normalized = normalizeBaseUrl(url)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKEND_BASE_URL, normalized).apply()
        return normalized
    }

    fun normalizeBaseUrl(url: String): String {
        var normalized = url.trim().removeSuffix("/")
        if (normalized.isBlank()) {
            return DEFAULT_BACKEND_BASE_URL
        }
        if (!normalized.endsWith("/v1")) {
            normalized += "/v1"
        }
        return normalized
    }

    fun toHealthUrl(baseUrl: String): String {
        val normalized = normalizeBaseUrl(baseUrl)
        val root = normalized.removeSuffix("/v1")
        return "$root/health"
    }
}
