package com.kycis.sdk.core

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "KYCIS-SDK"

internal class HttpBackendClient(
    private val baseUrl: String,
) : BackendClient {
    private val executor = Executors.newSingleThreadExecutor()

    override fun setUser(id: String, phone: String?) {
        if (id.isBlank()) return
        val payload = JSONObject().put("user_id", id).put("phone", phone)
        // MVP: user enrichment sent as generic event to keep backend contract additive.
        postJson(
            path = "/events",
            body = JSONObject()
                .put("user_id", id)
                .put("session_id", "unknown")
                .put("event", "user_enrichment")
                .put("timestamp", System.currentTimeMillis() / 1000)
                .put("properties", payload),
        )
    }

    override fun trackErrorEvent(userId: String, sessionId: String, screen: String?, code: String) {
        Log.d(TAG, "trackErrorEvent: screen=$screen code=$code session_id=$sessionId")
        val properties = JSONObject().put("error_code", code)
        postJson(
            path = "/events",
            body = JSONObject()
                .put("user_id", userId)
                .put("session_id", sessionId)
                .put("event", "validation_error")
                .put("screen", screen)
                .put("errors", JSONArray().put(code))
                .put("timestamp", System.currentTimeMillis() / 1000)
                .put("properties", properties),
        )
    }

    override fun evaluateTrigger(
        userId: String,
        sessionId: String,
        signals: TriggerSignals,
        onResult: (TriggerDecision) -> Unit,
    ) {
        Log.d(TAG, "evaluateTrigger: screen=${signals.screen} errors=${signals.errors}")
        executor.execute {
            val response = postJson(
                path = "/assistant/trigger/evaluate",
                body = JSONObject()
                    .put("user_id", userId)
                    .put("session_id", sessionId)
                    .put(
                        "signals",
                        JSONObject()
                            .put("screen", signals.screen)
                            .put("time_spent", signals.timeSpent)
                            .put("errors", JSONArray(signals.errors))
                            .put("idle_seconds", signals.idleSeconds)
                    ),
            )
            val decision = if (response != null) {
                TriggerDecision(
                    trigger = response.optBoolean("trigger", false),
                    reason = response.optString("reason", "unknown"),
                    action = response.optString("action", "none"),
                )
            } else {
                TriggerDecision(trigger = false, reason = "network_error", action = "none")
            }
            onResult(decision)
        }
    }

    override fun startAssistant(
        userId: String,
        sessionId: String,
        screen: String?,
        onResult: (com.kycis.sdk.VoiceSessionResult?) -> Unit,
    ) {
        Log.d(TAG, "startAssistant: screen=$screen session_id=$sessionId")
        executor.execute {
            val response = postJson(
                path = "/assistant/session/start",
                body = JSONObject()
                    .put("user_id", userId)
                    .put("session_id", sessionId)
                    .put("screen", screen),
            )
            val result = if (response != null && response.has("token") && response.optString("token").isNotBlank()) {
                com.kycis.sdk.VoiceSessionResult(
                    token = response.optString("token", ""),
                    livekitUrl = response.optString("livekit_url", ""),
                    livekitRoom = response.optString("livekit_room", ""),
                )
            } else {
                null
            }
            onResult(result)
        }
    }

    override fun stopAssistant(userId: String, sessionId: String) {
        postJson(
            path = "/assistant/session/stop",
            body = JSONObject()
                .put("user_id", userId)
                .put("session_id", sessionId),
        )
    }

    private fun postJson(path: String, body: JSONObject): JSONObject? {
        return try {
            val url = URL("${baseUrl.trimEnd('/')}$path")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use { stream ->
                stream.write(body.toString().toByteArray())
            }
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val text = stream?.bufferedReader()?.use { it.readText() }
            if (connection.responseCode !in 200..299) {
                Log.w(TAG, "Backend $path failed: ${connection.responseCode} $text")
            }
            if (!text.isNullOrBlank()) JSONObject(text) else null
        } catch (e: Exception) {
            Log.e(TAG, "Backend $path error: ${e.message}", e)
            null
        }
    }
}
