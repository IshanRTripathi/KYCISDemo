package com.kycis.demo.harness

import android.content.Context
import com.kycis.demo.BackendUrlStore
import com.kycis.demo.BuildConfig
import com.kycis.sdk.core.ScreenSchema
import com.kycis.sdk.core.WorkflowModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Minimal, standalone HTTP client for the flow-test harness.
 *
 * Deliberately independent of the live [com.kycis.sdk.AI] singleton session: the SDK creates one
 * fixed session per app install and exposes no way to read backend state back, so driving a test
 * through it would both mutate real session data and give us nothing to assert against. Instead
 * this client opens its own disposable session (fresh user/session id, dedicated `client_id`)
 * against the same backend, mirroring the wire shapes used by
 * [com.kycis.sdk.core.HttpBackendClient] in the SDK.
 */
class HarnessBackendClient(context: Context) {

    val sessionId: String = "harness-${UUID.randomUUID()}"
    val userId: String = "harness-user-${UUID.randomUUID().toString().take(8)}"
    val clientId: String = "kycis_demo_harness"
    val mappingVersion: String = "v1"

    /** [BackendUrlStore.get] already normalizes to end with "/v1". */
    private val v1BaseUrl: String = BackendUrlStore.get(context)
    private val rootBaseUrl: String = v1BaseUrl.removeSuffix("/v1")
    private val apiKey: String = BuildConfig.KYCIS_API_KEY.trim()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    data class HarnessResponse(
        val statusCode: Int,
        val json: JSONObject?,
        val rawBody: String?,
        val error: String? = null,
    ) {
        val ok: Boolean get() = error == null && statusCode in 200..299
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    private fun execute(request: Request): HarnessResponse = try {
        http.newCall(request).execute().use { resp ->
            val raw = resp.body?.string()
            val parsed = raw?.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }
            HarnessResponse(statusCode = resp.code, json = parsed, rawBody = raw)
        }
    } catch (e: Exception) {
        HarnessResponse(statusCode = -1, json = null, rawBody = null, error = e.message ?: e.toString())
    }

    private fun post(path: String, body: JSONObject, useV1: Boolean = true): HarnessResponse {
        val url = (if (useV1) v1BaseUrl else rootBaseUrl) + path
        val builder = Request.Builder().url(url).post(body.toString().toRequestBody(jsonMediaType))
        if (apiKey.isNotBlank()) builder.addHeader("X-API-Key", apiKey)
        return execute(builder.build())
    }

    private fun get(path: String, useV1: Boolean = true): HarnessResponse {
        val url = (if (useV1) v1BaseUrl else rootBaseUrl) + path
        val builder = Request.Builder().url(url).get()
        if (apiKey.isNotBlank()) builder.addHeader("X-API-Key", apiKey)
        return execute(builder.build())
    }

    /** Reconstructs the same `properties` wire shape as [ScreenSchema] (internal to the SDK module). */
    private fun ScreenSchema.toHarnessProperties(): JSONObject {
        val obj = JSONObject().put("screen_id", screenId)
        displayName?.let { obj.put("display_name", it) }
        val comps = JSONArray()
        for (comp in components) {
            val c = JSONObject()
                .put("id", comp.id)
                .put("type", comp.type.value)
                .put("required", comp.required)
            comp.displayName?.let { c.put("display_name", it) }
            val rules = JSONArray()
            for (rule in comp.validations) {
                val r = JSONObject().put("rule_id", rule.ruleId)
                rule.intent?.let { r.put("intent", it) }
                rule.pattern?.let { r.put("pattern", it) }
                r.put("error_codes", JSONArray(rule.errorCodes))
                rule.recoveryPlaybookId?.let { r.put("recovery_playbook_id", it) }
                rule.description?.let { r.put("description", it) }
                rules.put(r)
            }
            c.put("validations", rules)
            comps.put(c)
        }
        obj.put("components", comps)
        nextScreenId?.let { obj.put("next_screen_id", it) }
        flowOrder?.let { obj.put("flow_order", it) }
        return obj
    }

    fun pushSchema(schema: ScreenSchema): HarnessResponse {
        val body = JSONObject()
            .put("user_id", userId)
            .put("session_id", sessionId)
            .put("event", "screen_schema")
            .put("screen", schema.screenId)
            .put("screen_id", schema.screenId)
            .put("client_id", clientId)
            .put("mapping_version", mappingVersion)
            .put("timestamp", nowSeconds())
            .put("properties", schema.toHarnessProperties())
            .put("invoke_source", "flow_test_harness")
        return post("/events", body)
    }

    fun pushWorkflow(model: WorkflowModel): HarnessResponse {
        val stages = JSONArray()
        model.stages.forEach { stage ->
            val o = JSONObject().put("id", stage.id)
            stage.expectedNext?.let { o.put("expected_next", it) }
            stage.displayName?.let { o.put("display_name", it) }
            stage.blockerCode?.let { o.put("blocker_code", it) }
            stages.put(o)
        }
        val properties = JSONObject()
            .put("workflow_name", model.workflowName)
            .put("stages", stages)
        val body = JSONObject()
            .put("user_id", userId)
            .put("session_id", sessionId)
            .put("event", "workflow_model")
            .put("client_id", clientId)
            .put("mapping_version", mappingVersion)
            .put("timestamp", nowSeconds())
            .put("properties", properties)
            .put("invoke_source", "flow_test_harness")
        return post("/events", body)
    }

    fun setScreenState(screenId: String): HarnessResponse {
        val body = JSONObject()
            .put("user_id", userId)
            .put("session_id", sessionId)
            .put("event", "screen_state")
            .put("screen", screenId)
            .put("screen_id", screenId)
            .put("client_id", clientId)
            .put("mapping_version", mappingVersion)
            .put("timestamp", nowSeconds())
            .put("invoke_source", "flow_test_harness")
        return post("/events", body)
    }

    fun sendComponentInput(
        componentId: String,
        hint: String,
        screenId: String,
        componentType: String,
    ): HarnessResponse {
        val body = JSONObject()
            .put("user_id", userId)
            .put("session_id", sessionId)
            .put("event", "component_input")
            .put("component_id", componentId)
            .put("hint", hint)
            .put("masked", false)
            .put("screen", screenId)
            .put("screen_id", screenId)
            .put("component_type", componentType)
            .put("client_id", clientId)
            .put("mapping_version", mappingVersion)
            .put("timestamp", nowSeconds())
            .put("properties", JSONObject())
            .put("invoke_source", "flow_test_harness")
        return post("/events", body)
    }

    fun sendValidationFailed(
        code: String,
        componentId: String,
        componentType: String,
        expectedPattern: String?,
        validationRuleId: String?,
        hint: String,
        screenId: String,
    ): HarnessResponse {
        val body = JSONObject()
            .put("user_id", userId)
            .put("session_id", sessionId)
            .put("event", "validation_failed")
            .put("screen", screenId)
            .put("screen_id", screenId)
            .put("errors", JSONArray().put(code))
            .put("failure_reason_code", code)
            .put("component_id", componentId)
            .put("component_type", componentType)
            .put("hint", hint)
            .put("masked", false)
            .put("client_id", clientId)
            .put("mapping_version", mappingVersion)
            .put("timestamp", nowSeconds())
            .put("properties", JSONObject().put("error_code", code).put("signal_strength", "strong"))
            .put("invoke_source", "flow_test_harness")
        expectedPattern?.let { body.put("expected_pattern", it) }
        validationRuleId?.let { body.put("validation_rule_id", it) }
        return post("/events", body)
    }

    fun setFlow(flowKey: String, stepSequence: List<String>, currentStep: String): HarnessResponse {
        val body = JSONObject()
            .put("user_id", userId)
            .put("session_id", sessionId)
            .put("flow_key", flowKey)
            .put("client_id", clientId)
            .put("step_sequence", JSONArray(stepSequence))
            .put("current_step", currentStep)
        return post("/flows/set", body)
    }

    fun getAssistantContext(): HarnessResponse = get("/assistant/context/$sessionId")

    fun getActivity(limit: Int = 50): HarnessResponse = get("/activity?limit=$limit", useV1 = false)
}
