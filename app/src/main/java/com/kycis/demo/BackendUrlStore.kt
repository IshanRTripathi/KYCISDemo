package com.kycis.demo

import android.content.Context

object BackendUrlStore {
    private const val PREFS_NAME = "kycis_demo_prefs"
    private const val KEY_BACKEND_BASE_URL = "backend_base_url"
    private const val DEFAULT_BACKEND_BASE_URL =
        "http://prod-alb-1551985914.ap-south-1.elb.amazonaws.com"

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

